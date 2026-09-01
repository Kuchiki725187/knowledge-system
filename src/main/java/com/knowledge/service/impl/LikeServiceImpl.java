package com.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.knowledge.common.context.BaseContext;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.entity.Knowledge;
import com.knowledge.entity.KnowledgeLike;
import com.knowledge.mapper.KnowledgeLikeMapper;
import com.knowledge.mapper.KnowledgeMapper;
import com.knowledge.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

    private final KnowledgeMapper knowledgeMapper;
    private final KnowledgeLikeMapper likeMapper;

    /**
     * 点赞(唯一索引防重 + 冗余计数列自增)
     * @param knowledgeId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void like(Long knowledgeId) {
        Long userId = BaseContext.getUserId();

        // 1.校验知识可见(已发布,或本人草稿) —— 和详情同一个准入标准
        checkVisible(knowledgeId, userId);

        // 2.插入点赞记录;重复点赞由唯一索引兜底报 4002(数据库约束优于先查后插)
        try {
            KnowledgeLike like = new KnowledgeLike();
            like.setUserId(userId);
            like.setKnowledgeId(knowledgeId);
            likeMapper.insert(like);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(4002, "已点赞，请勿重复操作");
        }

        // 3.冗余计数列SQL原子自增,和记录插入同事务,要么都成要么都回滚
        knowledgeMapper.update(null, new LambdaUpdateWrapper<Knowledge>()
                .eq(Knowledge::getId, knowledgeId)
                .setSql("like_count = like_count + 1"));
    }

    /**
     * 取消点赞
     * @param knowledgeId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlike(Long knowledgeId) {
        Long userId = BaseContext.getUserId();

        // 1.删点赞记录;删到几行决定是否减计数,保证"记录行数"与"计数"严格同步
        int deleted = likeMapper.delete(new LambdaQueryWrapper<KnowledgeLike>()
                .eq(KnowledgeLike::getUserId, userId)
                .eq(KnowledgeLike::getKnowledgeId, knowledgeId));

        // 2.确实删掉了才递减;GREATEST 兜底,防止脏数据导致计数变负数
        if (deleted > 0) {
            knowledgeMapper.update(null, new LambdaUpdateWrapper<Knowledge>()
                    .eq(Knowledge::getId, knowledgeId)
                    .setSql("like_count = GREATEST(like_count - 1, 0)"));
        }
    }

    /**
     * 校验知识对当前用户可见(存在,且已发布或本人所有)
     */
    private void checkVisible(Long knowledgeId, Long userId) {
        Knowledge knowledge = knowledgeMapper.selectById(knowledgeId);
        if (knowledge == null
                || (knowledge.getStatus() != 1 && !knowledge.getUserId().equals(userId))) {
            throw new BusinessException(2001, "知识不存在");
        }
    }
}
