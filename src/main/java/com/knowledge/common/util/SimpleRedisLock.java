package com.knowledge.common.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * 基于 Redis SET NX EX 的简易分布式锁
 * 适用:多实例部署时互斥执行定时任务等跨进程临界区
 * 单实例场景用 synchronized 即可,不需要本锁(锁的目的域不同)
 */
@Component
@RequiredArgsConstructor
public class SimpleRedisLock {

    /**
     * 释放锁的 Lua 脚本:只有 value 匹配(锁是自己持有的)才删除。
     * 防止持有超时后锁已过期被他人抢走,原持有者却把别人的锁删了
     */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('DEL', KEYS[1]) "
                    + "else return 0 end",
            Long.class);

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 尝试加锁,立即返回(不阻塞等待)
     * @param key           锁标识,lock:view-count-sync
     * @param expireSeconds 锁自动过期时间,兜底防止持锁实例崩溃导致死锁
     * @return 锁持有者标识;拿到锁返回非空,没拿到返回 null
     */
    public String tryLock(String key, long expireSeconds) {
        // value 用 UUID:释放时必须校验是自己的锁
        String value = UUID.randomUUID().toString().replace("-", "");
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, value, Duration.ofSeconds(expireSeconds));
        return Boolean.TRUE.equals(success) ? value : null;
    }

    /**
     * 释放锁(通过 Lua 脚本原子校验持有者)
     * @param key   锁标识
     * @param value 加锁时返回的持有者标识
     */
    public void unlock(String key, String value) {
        if (value == null) {
            return;
        }
        stringRedisTemplate.execute(UNLOCK_SCRIPT, List.of(key), value);
    }
}
