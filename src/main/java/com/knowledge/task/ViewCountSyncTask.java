package com.knowledge.task;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.knowledge.entity.Knowledge;
import com.knowledge.mapper.KnowledgeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class ViewCountSyncTask {

    private static final String VIEW_COUNT_KEY_PREFIX = "knowledge:view:";

    /**
     * Lua 脚本实现"取走+删除"
     * 用 Lua 在服务器端原子执行,全版本兼容,效果与 GETDEL 一致
     */
    private static final DefaultRedisScript<Long> GET_AND_DELETE_SCRIPT = new DefaultRedisScript<>(
            "local v = redis.call('GET', KEYS[1]) "
                    + "if v then redis.call('DEL', KEYS[1]) end "
                    + "return v",
            Long.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final KnowledgeMapper knowledgeMapper;

    /**
     * 每 60 秒把 Redis 里的浏览增量批量落库
     * fixedDelay:上次执行完再间隔 60s,避免任务重叠
     */
    @Scheduled(fixedDelay = 60000)
    public void syncViewCount() {
        // 1.扫描所有浏览计数 key(用 SCAN 而非 KEYS,避免阻塞 Redis 主线程)
        Set<String> keys = new HashSet<>();
        try (Cursor<String> cursor = stringRedisTemplate.scan(
                ScanOptions.scanOptions().match(VIEW_COUNT_KEY_PREFIX + "*").count(100).build())) {
            cursor.forEachRemaining(keys::add);
        } catch (Exception e) {
            log.error("扫描浏览计数 key 失败", e);
            return;
        }
        if (keys.isEmpty()) {
            return;
        }

        // 2.逐个取走增量并写入数据库(Lua 原子取走+删除,避免取-删之间新浏览丢失)
        for (String key : keys) {
            try {
                // 调用 Lua 脚本取走增量
                Long delta = stringRedisTemplate.execute(GET_AND_DELETE_SCRIPT, List.of(key));
                if (delta == null) {
                    continue;
                }
                if (delta <= 0) {
                    continue;
                }
                // 解析知识 ID
                Long knowledgeId = Long.parseLong(key.substring(VIEW_COUNT_KEY_PREFIX.length()));
                // 增量是程序自产的数字,拼接安全;SQL 原子累加,并发落库不丢
                knowledgeMapper.update(null, new LambdaUpdateWrapper<Knowledge>()
                        .eq(Knowledge::getId, knowledgeId)
                        .setSql("view_count = view_count + " + delta));
            } catch (Exception e) {
                log.error("浏览计数落库失败, key={}", key, e);
            }
        }
    }
}
