package com.knowledge.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.common.context.BaseContext;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.common.page.PageVO;
import com.knowledge.entity.Category;
import com.knowledge.entity.Knowledge;
import com.knowledge.entity.SearchLog;
import com.knowledge.mapper.CategoryMapper;
import com.knowledge.mapper.KnowledgeMapper;
import com.knowledge.mapper.SearchLogMapper;
import com.knowledge.service.SearchService;
import com.knowledge.vo.KnowledgeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final KnowledgeMapper knowledgeMapper;
    private final CategoryMapper categoryMapper;
    private final SearchLogMapper searchLogMapper;

    /**
     * 全局搜索我的知识(标题+正文全文匹配)
     * @param keyword 搜索关键词
     * @param page    页码
     * @param size    每页数量
     * @return
     */
    @Override
    public PageVO<KnowledgeVO> search(String keyword, long page, long size) {
        // 1.关键词必填(5001),空搜索没有意义还浪费全文索引
        if (!StringUtils.hasText(keyword)) {
            throw new BusinessException(5001, "搜索关键词为空");
        }

        // 2.分页参数兜底:页码最小 1,每页上限 50
        long p = page < 1 ? 1 : page;
        long s = size < 1 ? 10 : Math.min(size, 50);

        // 3.只搜自己的知识(数据隔离),执行 ngram 全文检索
        Long userId = BaseContext.getUserId();
        IPage<Knowledge> result = knowledgeMapper.searchByFullText(new Page<>(p, s), userId, keyword);

        // 4.批量装配分类名(先收集去重,一次 in 查询,避免 N+1)
        List<Long> categoryIds = result.getRecords().stream()
                .map(Knowledge::getCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> categoryNameMap = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            categoryMapper.selectByIds(categoryIds)
                    .forEach(c -> categoryNameMap.put(c.getId(), c.getName()));
        }

        // 5.实体转 VO 并填充分类名
        List<KnowledgeVO> vos = result.getRecords().stream().map(k -> {
            KnowledgeVO vo = new KnowledgeVO();
            vo.setId(k.getId());
            vo.setTitle(k.getTitle());
            vo.setSummary(k.getSummary());
            vo.setCategoryId(k.getCategoryId());
            vo.setCategoryName(categoryNameMap.get(k.getCategoryId()));
            vo.setStatus(k.getStatus());
            vo.setViewCount(k.getViewCount());
            vo.setCreateTime(k.getCreateTime());
            vo.setUpdateTime(k.getUpdateTime());
            return vo;
        }).toList();

        // 6.旁路操作:记录搜索日志(为后期检索质量分析和 AI 阶段攒数据),失败不影响搜索
        try {
            SearchLog searchLog = new SearchLog();
            searchLog.setUserId(userId);
            searchLog.setKeyword(keyword);
            searchLog.setResultCount((int) result.getTotal());
            searchLogMapper.insert(searchLog);
        } catch (Exception e) {
            log.error("搜索日志记录失败, keyword={}", keyword, e);
        }

        return PageVO.of(result.getTotal(), result.getCurrent(), result.getSize(), vos);
    }
}
