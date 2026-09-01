package com.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.common.context.BaseContext;
import com.knowledge.entity.BrowseHistory;
import com.knowledge.entity.Category;
import com.knowledge.entity.Knowledge;
import com.knowledge.mapper.BrowseHistoryMapper;
import com.knowledge.mapper.CategoryMapper;
import com.knowledge.mapper.KnowledgeMapper;
import com.knowledge.service.BrowseHistoryService;
import com.knowledge.vo.BrowseHistoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BrowseHistoryServiceImpl implements BrowseHistoryService {

    private final BrowseHistoryMapper browseHistoryMapper;
    private final KnowledgeMapper knowledgeMapper;
    private final CategoryMapper categoryMapper;

    /**
     * 最近浏览列表
     * @param limit 条数上限
     * @return
     */
    @Override
    public List<BrowseHistoryVO> recent(int limit) {
        // 1.参数兜底:默认 20,上限 50
        int n = limit < 1 ? 20 : Math.min(limit, 50);
        Long userId = BaseContext.getUserId();

        // 2.查最近 N 条浏览记录(同一知识只留一行,表结构已保证去重)
        Page<BrowseHistory> result = browseHistoryMapper.selectPage(new Page<>(1, n),
                new LambdaQueryWrapper<BrowseHistory>()
                        .eq(BrowseHistory::getUserId, userId)
                        .orderByDesc(BrowseHistory::getBrowseTime));

        // 3.批量装配知识标题与分类名(知识删除时浏览记录会被级联清理)
        List<Long> knowledgeIds = result.getRecords().stream()
                .map(BrowseHistory::getKnowledgeId).toList();
        Map<Long, Knowledge> knowledgeMap = new HashMap<>();
        Map<Long, String> categoryNameMap = new HashMap<>();
        if (!knowledgeIds.isEmpty()) {
            List<Knowledge> knows = knowledgeMapper.selectByIds(knowledgeIds);
            knows.forEach(k -> knowledgeMap.put(k.getId(), k));
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

        // 4.转 VO;知识已被删除的记录保留但不给标题,标记 deleted 让前端显示"已删除"
        return result.getRecords().stream().map(h -> {
            BrowseHistoryVO vo = new BrowseHistoryVO();
            vo.setId(h.getKnowledgeId());
            vo.setBrowseTime(h.getBrowseTime());
            Knowledge k = knowledgeMap.get(h.getKnowledgeId());
            if (k == null) {
                // 知识已删除:历史记录保留,打上 deleted 标记
                vo.setDeleted(true);
            } else {
                vo.setDeleted(false);
                vo.setTitle(k.getTitle());
                vo.setCategoryName(categoryNameMap.get(k.getCategoryId()));
            }
            return vo;
        }).toList();
    }
}
