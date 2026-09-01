package com.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.common.context.BaseContext;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.dto.RelationSaveDTO;
import com.knowledge.entity.Knowledge;
import com.knowledge.entity.KnowledgeRelation;
import com.knowledge.mapper.KnowledgeMapper;
import com.knowledge.mapper.KnowledgeRelationMapper;
import com.knowledge.service.RelationService;
import com.knowledge.vo.RelationVO;
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
public class RelationServiceImpl implements RelationService {

    private final KnowledgeMapper knowledgeMapper;
    private final KnowledgeRelationMapper relationMapper;

    /**
     * 建立知识关联(A-相关-B,双向语义,单向存储)
     * @param knowledgeId 当前知识 id
     * @param dto         对方知识 id + 关联类型
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addRelation(Long knowledgeId, RelationSaveDTO dto) {
        Long userId = BaseContext.getUserId();

        // 1.校验自己不能关联自己
        if (dto.getTargetId().equals(knowledgeId)) {
            throw new BusinessException(400, "不能关联自己");
        }

        // 2.校验两边知识都存在且属于本人(数据隔离)
        getOwnedKnowledge(knowledgeId, userId);
        getOwnedKnowledge(dto.getTargetId(), userId);

        // 3.插入关联记录;A→B 或 B→A 已存在时由唯一索引兜底报 2003
        try {
            KnowledgeRelation relation = new KnowledgeRelation();
            relation.setUserId(userId);
            relation.setSourceId(knowledgeId);
            relation.setTargetId(dto.getTargetId());
            relation.setRelationType(dto.getRelationType() == null ? 1 : dto.getRelationType());
            relationMapper.insert(relation);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(2003, "关联已存在");
        }
    }

    /**
     * 查询某知识的全部关联(含对方知识标题与摘要)
     * @param knowledgeId
     * @return
     */
    @Override
    public List<RelationVO> listRelations(Long knowledgeId) {
        Long userId = BaseContext.getUserId();
        // 1.校验知识归属
        getOwnedKnowledge(knowledgeId, userId);

        // 2.双向查询:当前知识可能在 source 也可能在 target
        List<KnowledgeRelation> relations = relationMapper.selectList(
                new LambdaQueryWrapper<KnowledgeRelation>()
                        .eq(KnowledgeRelation::getUserId, userId)
                        .and(w -> w.eq(KnowledgeRelation::getSourceId, knowledgeId)
                                .or()
                                .eq(KnowledgeRelation::getTargetId, knowledgeId)));

        // 3.收集"对方知识 id"(自己在 source 则对方是 target,反之亦然),批量查标题摘要
        List<Long> otherIds = relations.stream()
                .map(r -> r.getSourceId().equals(knowledgeId) ? r.getTargetId() : r.getSourceId())
                .toList();
        Map<Long, Knowledge> knowledgeMap = new HashMap<>();
        if (!otherIds.isEmpty()) {
            knowledgeMapper.selectByIds(otherIds).forEach(k -> knowledgeMap.put(k.getId(), k));
        }

        // 4.转 VO
        return relations.stream().map(r -> {
            Long otherId = r.getSourceId().equals(knowledgeId) ? r.getTargetId() : r.getSourceId();
            RelationVO vo = new RelationVO();
            vo.setId(r.getId());
            vo.setKnowledgeId(otherId);
            vo.setRelationType(r.getRelationType());
            vo.setCreateTime(r.getCreateTime());
            Knowledge other = knowledgeMap.get(otherId);
            if (other != null) {
                vo.setTitle(other.getTitle());
                vo.setSummary(other.getSummary());
            }
            return vo;
        }).toList();
    }

    /**
     * 删除一条关联
     * @param relationId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeRelation(Long relationId) {
        // 物理删除,userId 放进条件保证只能删自己的关联(查不到就统一报错,防探测)
        Long userId = BaseContext.getUserId();
        int deleted = relationMapper.delete(new LambdaQueryWrapper<KnowledgeRelation>()
                .eq(KnowledgeRelation::getId, relationId)
                .eq(KnowledgeRelation::getUserId, userId));
        if (deleted == 0) {
            throw new BusinessException(2003, "关联不存在");
        }
    }

    /**
     * 校验知识存在且属于当前用户
     */
    private void getOwnedKnowledge(Long id, Long userId) {
        Knowledge knowledge = knowledgeMapper.selectById(id);
        if (knowledge == null || !knowledge.getUserId().equals(userId)) {
            throw new BusinessException(2001, "知识不存在");
        }
    }
}
