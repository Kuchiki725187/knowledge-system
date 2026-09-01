package com.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.common.context.BaseContext;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.common.page.PageVO;
import com.knowledge.entity.Category;
import com.knowledge.entity.Favorite;
import com.knowledge.entity.Knowledge;
import com.knowledge.mapper.CategoryMapper;
import com.knowledge.mapper.FavoriteMapper;
import com.knowledge.mapper.KnowledgeMapper;
import com.knowledge.service.FavoriteService;
import com.knowledge.vo.FavoriteVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final KnowledgeMapper knowledgeMapper;
    private final CategoryMapper categoryMapper;

    /**
     * 收藏知识(社区模型:已发布知识人人可收藏;收藏数冗余列同步自增)
     * @param knowledgeId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Long knowledgeId) {
        Long userId = BaseContext.getUserId();

        // 1.校验知识可见(已发布,或本人草稿) —— 与详情/点赞同一个准入标准
        Knowledge knowledge = knowledgeMapper.selectById(knowledgeId);
        if (knowledge == null
                || (knowledge.getStatus() != 1 && !knowledge.getUserId().equals(userId))) {
            throw new BusinessException(2001, "知识不存在");
        }
        // 2.直接插入,重复收藏由唯一索引兜底(数据库约束优于"先查后插",并发下不会漏)
        try {
            Favorite favorite = new Favorite();
            favorite.setUserId(userId);
            favorite.setKnowledgeId(knowledgeId);
            favoriteMapper.insert(favorite);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(4001, "已收藏，请勿重复操作");
        }
        // 3.收藏计数列 SQL 原子自增,与插入同事务
        knowledgeMapper.update(null, new LambdaUpdateWrapper<Knowledge>()
                .eq(Knowledge::getId, knowledgeId)
                .setSql("favorite_count = favorite_count + 1"));
    }

    /**
     * 取消收藏(幂等;确实删到记录才递减计数,GREATEST 防负数)
     * @param knowledgeId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long knowledgeId) {
        Long userId = BaseContext.getUserId();

        // 1.按 用户+知识 删除;删到几行决定是否减计数,保证记录与计数严格同步
        int deleted = favoriteMapper.delete(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getKnowledgeId, knowledgeId));

        // 2.确实删掉了才递减
        if (deleted > 0) {
            knowledgeMapper.update(null, new LambdaUpdateWrapper<Knowledge>()
                    .eq(Knowledge::getId, knowledgeId)
                    .setSql("favorite_count = GREATEST(favorite_count - 1, 0)"));
        }
    }

    /**
     * 我的收藏分页(按收藏时间倒序)
     * @param page
     * @param size
     * @return
     */
    @Override
    public PageVO<FavoriteVO> page(long page, long size) {
        // 1.分页参数兜底
        long p = page < 1 ? 1 : page;
        long s = size < 1 ? 10 : Math.min(size, 50);
        Long userId = BaseContext.getUserId();

        // 2.分页查收藏记录,按收藏时间倒序
        Page<Favorite> result = favoriteMapper.selectPage(new Page<>(p, s),
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .orderByDesc(Favorite::getCreateTime));

        // 3.批量查知识装配标题(知识删除时会级联清理收藏,这里不会查到已删知识)
        List<Long> knowledgeIds = result.getRecords().stream()
                .map(Favorite::getKnowledgeId).toList();
        Map<Long, Knowledge> knowledgeMap = new HashMap<>();
        Map<Long, String> categoryNameMap = new HashMap<>();
        if (!knowledgeIds.isEmpty()) {
            List<Knowledge> knows = knowledgeMapper.selectByIds(knowledgeIds);
            knows.forEach(k -> knowledgeMap.put(k.getId(), k));
            // 4.分类名同样批量装配(规避 N+1)
            List<Long> categoryIds = knows.stream()
                    .map(Knowledge::getCategoryId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (!categoryIds.isEmpty()) {
                categoryMapper.selectByIds(categoryIds)
                        .forEach(c -> categoryNameMap.put(c.getId(), c.getName()));
            }
        }

        // 5.转 VO;知识已被删除的收藏保留但打 deleted 标记,前端显示"已删除"
        List<FavoriteVO> vos = result.getRecords().stream().map(f -> {
            FavoriteVO vo = new FavoriteVO();
            vo.setId(f.getKnowledgeId());
            vo.setFavoriteTime(f.getCreateTime());
            Knowledge k = knowledgeMap.get(f.getKnowledgeId());
            if (k == null) {
                vo.setDeleted(true);
            } else {
                vo.setDeleted(false);
                vo.setTitle(k.getTitle());
                vo.setCategoryName(categoryNameMap.get(k.getCategoryId()));
            }
            return vo;
        }).toList();

        return PageVO.of(result.getTotal(), result.getCurrent(), result.getSize(), vos);
    }
}
