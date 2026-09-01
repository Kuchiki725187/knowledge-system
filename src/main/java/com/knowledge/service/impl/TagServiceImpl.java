package com.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.knowledge.common.context.BaseContext;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.dto.TagSaveDTO;
import com.knowledge.entity.KnowledgeTag;
import com.knowledge.entity.Tag;
import com.knowledge.mapper.KnowledgeTagMapper;
import com.knowledge.mapper.TagMapper;
import com.knowledge.service.TagService;
import com.knowledge.vo.TagVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;
    private final KnowledgeTagMapper knowledgeTagMapper;

    /**
     * 查询我的标签列表(含使用次数,按使用次数降序)
     * @return
     */
    @Override
    public List<TagVO> list() {
        // 1.查当前用户的未删除标签
        Long userId = BaseContext.getUserId();
        List<Tag> tags = tagMapper.selectList(new LambdaQueryWrapper<Tag>()
                .eq(Tag::getUserId, userId)
                .eq(Tag::getDeleted, 0L));

        // 2.一条 group by 统计每个标签被多少知识使用;
        //   知识删除时关联表会物理删除,所以这里天然只统计"活知识"的使用量
        Map<Long, Long> usageMap = new HashMap<>();
        List<Map<String, Object>> rows = knowledgeTagMapper.selectMaps(
                new QueryWrapper<KnowledgeTag>()
                        .select("tag_id", "count(*) as cnt")
                        .groupBy("tag_id"));
        for (Map<String, Object> row : rows) {
            usageMap.put(Long.valueOf(row.get("tag_id").toString()),
                    Long.valueOf(row.get("cnt").toString()));
        }

        // 3.转 VO,按使用次数降序(API 约定)
        return tags.stream().map(t -> {
            TagVO vo = new TagVO();
            vo.setId(t.getId());
            vo.setName(t.getName());
            vo.setUsageCount(usageMap.getOrDefault(t.getId(), 0L));
            vo.setCreateTime(t.getCreateTime());
            return vo;
        }).sorted(Comparator.comparingLong(TagVO::getUsageCount).reversed()).toList();
    }

    /**
     * 创建标签(手动建标签的场景;知识编辑页是自动 upsert,走的是 bindTags)
     * @param dto
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(TagSaveDTO dto) {
        Long userId = BaseContext.getUserId();
        // 1.同用户未删除标签查重
        checkNameUnique(userId, dto.getName());
        // 2.插入
        Tag tag = new Tag();
        tag.setUserId(userId);
        tag.setName(dto.getName());
        tagMapper.insert(tag);
    }

    /**
     * 删除标签并清理所有知识关联
     * @param id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long userId = BaseContext.getUserId();
        // 1.校验标签存在且属于本人
        getOwnedTag(id, userId);
        // 2.逻辑删除:deleted 置为行 id(同分类方案,删除后可重建同名)
        tagMapper.update(null, new LambdaUpdateWrapper<Tag>()
                .eq(Tag::getId, id)
                .set(Tag::getDeleted, id));
        // 3.清理知识-标签关联(物理删除),否则残留的关联会成为脏数据
        knowledgeTagMapper.delete(new LambdaQueryWrapper<KnowledgeTag>()
                .eq(KnowledgeTag::getTagId, id));
    }

    /**
     * 校验标签存在且属于当前用户。统一报 3101,防探测
     */
    private Tag getOwnedTag(Long id, Long userId) {
        Tag tag = tagMapper.selectOne(new LambdaQueryWrapper<Tag>()
                .eq(Tag::getId, id)
                .eq(Tag::getUserId, userId)
                .eq(Tag::getDeleted, 0L));
        if (tag == null) {
            throw new BusinessException(3101, "标签不存在");
        }
        return tag;
    }

    /**
     * 同用户未删除的标签不允许重名
     */
    private void checkNameUnique(Long userId, String name) {
        if (tagMapper.selectCount(new LambdaQueryWrapper<Tag>()
                .eq(Tag::getUserId, userId)
                .eq(Tag::getName, name)
                .eq(Tag::getDeleted, 0L)) > 0) {
            throw new BusinessException(3102, "标签名称已存在");
        }
    }
}
