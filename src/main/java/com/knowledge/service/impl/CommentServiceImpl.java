package com.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.common.context.BaseContext;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.common.page.PageVO;
import com.knowledge.dto.CommentSaveDTO;
import com.knowledge.entity.Knowledge;
import com.knowledge.entity.KnowledgeComment;
import com.knowledge.entity.User;
import com.knowledge.mapper.KnowledgeCommentMapper;
import com.knowledge.mapper.KnowledgeMapper;
import com.knowledge.mapper.UserMapper;
import com.knowledge.service.CommentService;
import com.knowledge.vo.CommentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final KnowledgeMapper knowledgeMapper;
    private final KnowledgeCommentMapper commentMapper;
    private final UserMapper userMapper;

    /**
     * 发表评论(评论数冗余列同步自增)
     * @param knowledgeId
     * @param dto
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Long knowledgeId, CommentSaveDTO dto) {
        Long userId = BaseContext.getUserId();

        // 1.校验知识可见(已发布,或本人草稿)
        checkVisible(knowledgeId, userId);

        // 2.插入评论
        KnowledgeComment comment = new KnowledgeComment();
        comment.setUserId(userId);
        comment.setKnowledgeId(knowledgeId);
        comment.setContent(dto.getContent());
        commentMapper.insert(comment);

        // 3.评论计数自增,与插入同事务保证一致
        knowledgeMapper.update(null, new LambdaUpdateWrapper<Knowledge>()
                .eq(Knowledge::getId, knowledgeId)
                .setSql("comment_count = comment_count + 1"));
    }

    /**
     * 评论分页(按时间正序,先来后到)
     * @param knowledgeId
     * @param page
     * @param size
     * @return
     */
    @Override
    public PageVO<CommentVO> page(Long knowledgeId, long page, long size) {
        Long userId = BaseContext.getUserId();

        // 1.校验知识可见
        checkVisible(knowledgeId, userId);

        // 2.分页参数兜底
        long p = page < 1 ? 1 : page;
        long s = size < 1 ? 10 : Math.min(size, 50);

        // 3.分页查评论(正序:评论区习惯先来后到)
        Page<KnowledgeComment> result = commentMapper.selectPage(new Page<>(p, s),
                new LambdaQueryWrapper<KnowledgeComment>()
                        .eq(KnowledgeComment::getKnowledgeId, knowledgeId)
                        .orderByAsc(KnowledgeComment::getCreateTime));

        // 4.批量装配评论者昵称(评论人可能是任何用户,跨到 user 表)
        List<Long> userIds = result.getRecords().stream()
                .map(KnowledgeComment::getUserId)
                .distinct()
                .toList();
        Map<Long, String> nameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            userMapper.selectByIds(userIds).forEach(u ->
                    nameMap.put(u.getId(), u.getNickname() != null ? u.getNickname() : u.getUsername()));
        }

        // 5.转 VO
        List<CommentVO> vos = result.getRecords().stream().map(c -> {
            CommentVO vo = new CommentVO();
            vo.setId(c.getId());
            vo.setContent(c.getContent());
            vo.setUserId(c.getUserId());
            vo.setAuthorName(nameMap.get(c.getUserId()));
            vo.setCreateTime(c.getCreateTime());
            return vo;
        }).toList();

        return PageVO.of(result.getTotal(), result.getCurrent(), result.getSize(), vos);
    }

    /**
     * 删除自己的评论(评论数同步递减)
     * @param commentId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long commentId) {
        Long userId = BaseContext.getUserId();

        // 1.按 id + 用户查评论:非本人的评论等同不存在(统一报错,防探测)
        KnowledgeComment comment = commentMapper.selectOne(
                new LambdaQueryWrapper<KnowledgeComment>()
                        .eq(KnowledgeComment::getId, commentId)
                        .eq(KnowledgeComment::getUserId, userId));
        if (comment == null) {
            throw new BusinessException(404, "评论不存在");
        }

        // 2.物理删除
        commentMapper.deleteById(commentId);

        // 3.评论计数递减,GREATEST 兜底防负数
        knowledgeMapper.update(null, new LambdaUpdateWrapper<Knowledge>()
                .eq(Knowledge::getId, comment.getKnowledgeId())
                .setSql("comment_count = GREATEST(comment_count - 1, 0)"));
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
