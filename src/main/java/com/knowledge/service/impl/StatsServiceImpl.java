package com.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.common.context.BaseContext;
import com.knowledge.entity.Category;
import com.knowledge.entity.Favorite;
import com.knowledge.entity.Knowledge;
import com.knowledge.entity.Tag;
import com.knowledge.mapper.CategoryMapper;
import com.knowledge.mapper.FavoriteMapper;
import com.knowledge.mapper.KnowledgeMapper;
import com.knowledge.mapper.TagMapper;
import com.knowledge.service.StatsService;
import com.knowledge.vo.HotItemVO;
import com.knowledge.vo.StatsOverviewVO;
import com.knowledge.vo.TrendItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private static final String STATS_CACHE_PREFIX = "stats:";
    /**
     * 统计允许延迟5分钟,所以只缓存、不做精确失效(写操作无需删缓存)
     * TTL 加随机偏移防雪崩
     */
    private static final Duration STATS_CACHE_TTL = Duration.ofMinutes(5);

    private final KnowledgeMapper knowledgeMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final FavoriteMapper favoriteMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 总览:知识/分类/标签/收藏 数量
     */
    @Override
    public StatsOverviewVO overview() {
        Long userId = BaseContext.getUserId();
        String cacheKey = STATS_CACHE_PREFIX + "overview:" + userId;

        // 1.先查缓存
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            StatsOverviewVO vo = decode(cacheKey, cached, StatsOverviewVO.class);
            if (vo != null) {
                return vo;
            }
        }

        // 2.缓存未命中则查库统计(分类/标签 deleted=0 过滤逻辑删除)
        StatsOverviewVO vo = new StatsOverviewVO();
        vo.setKnowledgeCount(knowledgeMapper.selectCount(
                new LambdaQueryWrapper<Knowledge>().eq(Knowledge::getUserId, userId)));
        vo.setCategoryCount(categoryMapper.selectCount(
                new LambdaQueryWrapper<Category>().eq(Category::getUserId, userId).eq(Category::getDeleted, 0)));
        vo.setTagCount(tagMapper.selectCount(
                new LambdaQueryWrapper<Tag>().eq(Tag::getUserId, userId).eq(Tag::getDeleted, 0)));
        vo.setFavoriteCount(favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>().eq(Favorite::getUserId, userId)));

        // 3.写缓存(短 TTL + 随机偏移)
        setCache(cacheKey, vo);
        return vo;
    }

    /**
     * 近 N 天每日新增知识数(数据库没有记录的天补 0)
     */
    @Override
    public List<TrendItemVO> trend(int days) {
        Long userId = BaseContext.getUserId();
        days = Math.min(Math.max(days, 1), 30);
        String cacheKey = STATS_CACHE_PREFIX + "trend:" + userId + ":" + days;
       //先查缓存
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            List<TrendItemVO> vo = decodeList(cacheKey, cached, new TypeReference<List<TrendItemVO>>() {
            });
            if (vo != null) {
                return vo;
            }
        }

        // 1.按天分组统计新增知识数(聚合 SQL 列名是字符串,用 QueryWrapper)
        LocalDate start = LocalDate.now().minusDays(days - 1);
        List<Map<String, Object>> rows = knowledgeMapper.selectMaps(new QueryWrapper<Knowledge>()
                .select("DATE(create_time) AS day", "COUNT(*) AS cnt")
                .eq("user_id", userId)
                .ge("create_time", start.atStartOfDay())
                .groupBy("DATE(create_time)"));

        // 2.组装 Map 方便补零
        Map<String, Long> dayCountMap = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object day = row.get("day");
            Object cnt = row.get("cnt");
            if (day != null && cnt != null) {
                dayCountMap.put(day.toString(), Long.valueOf(cnt.toString()));
            }
        }

        // 3.从起始日到今天逐天补零,保证返回连续日期
        List<TrendItemVO> result = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            String date = start.plusDays(i).toString();
            TrendItemVO item = new TrendItemVO();
            item.setDate(date);
            item.setCount(dayCountMap.getOrDefault(date, 0L));
            result.add(item);
        }

        setCache(cacheKey, result);
        return result;
    }

    /**
     * 浏览量 Top N(展示值叠加 Redis 未落库增量,排序用已落库值)
     */
    @Override
    public List<HotItemVO> hot(int limit) {
        Long userId = BaseContext.getUserId();
        limit = Math.min(Math.max(limit, 1), 50);
        String cacheKey = STATS_CACHE_PREFIX + "hot:" + userId + ":" + limit;

        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            List<HotItemVO> vo = decodeList(cacheKey, cached, new TypeReference<List<HotItemVO>>() {
            });
            if (vo != null) {
                return vo;
            }
        }

        // 查询排除 content 大字段,按浏览量降序取前 N
        List<Knowledge> list = knowledgeMapper.selectList(new LambdaQueryWrapper<Knowledge>()
                .eq(Knowledge::getUserId, userId)
                .select(Knowledge.class, field -> !"content".equals(field.getProperty()))
                .orderByDesc(Knowledge::getViewCount)
                .last("LIMIT " + limit));

        List<HotItemVO> result = list.stream().map(k -> {
            HotItemVO vo = new HotItemVO();
            vo.setId(k.getId());
            vo.setTitle(k.getTitle());
            vo.setViewCount(k.getViewCount() + (int) getViewDelta(k.getId()));
            return vo;
        }).toList();

        setCache(cacheKey, result);
        return result;
    }

    /**
     * 读取 Redis 中未落库的浏览增量
     */
    private long getViewDelta(Long knowledgeId) {
        String value = stringRedisTemplate.opsForValue().get("knowledge:view:" + knowledgeId);
        if (value == null) {
            return 0;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void setCache(String cacheKey, Object obj) {
        try {
            long ttlSeconds = STATS_CACHE_TTL.toSeconds() + ThreadLocalRandom.current().nextLong(120);
            stringRedisTemplate.opsForValue()
                    .set(cacheKey, objectMapper.writeValueAsString(obj), Duration.ofSeconds(ttlSeconds));
        } catch (JsonProcessingException e) {
            log.error("统计缓存序列化失败, cacheKey={}", cacheKey, e);
        }
    }

    /**
     * 反序列化单个对象;坏缓存直接删除并返回 null(调用方回源查库)
     */
    private <T> T decode(String cacheKey, String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("统计缓存反序列化失败, 删除坏缓存, cacheKey={}", cacheKey, e);
            stringRedisTemplate.delete(cacheKey);
            return null;
        }
    }

    /**
     * 反序列化列表;坏缓存直接删除并返回 null(调用方回源查库)
     */
    private <T> List<T> decodeList(String cacheKey, String json, TypeReference<List<T>> typeRef) {
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            log.error("统计缓存反序列化失败, 删除坏缓存, cacheKey={}", cacheKey, e);
            stringRedisTemplate.delete(cacheKey);
            return null;
        }
    }
}
