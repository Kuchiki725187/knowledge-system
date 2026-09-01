package com.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.common.page.PageVO;
import com.knowledge.entity.Knowledge;
import com.knowledge.entity.User;
import com.knowledge.mapper.KnowledgeMapper;
import com.knowledge.mapper.UserMapper;
import com.knowledge.service.PlazaService;
import com.knowledge.vo.PlazaVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlazaServiceImpl implements PlazaService {

    private final KnowledgeMapper knowledgeMapper;
    private final UserMapper userMapper;

    /**
     * 知识大厅分页:所有用户共享,只展示已发布(status=1)的知识
     * @param page
     * @param size
     * @return
     */
    @Override
    public PageVO<PlazaVO> page(long page, long size) {
        // 1.分页参数兜底
        long p = page < 1 ? 1 : page;
        long s = size < 1 ? 10 : Math.min(size, 50);

        // 2.分页查已发布知识:注意这里没有 user_id 过滤 —— 大厅对全体用户开放;
        //   查询层面排除 content 大字段
        Page<Knowledge> result = knowledgeMapper.selectPage(new Page<>(p, s),
                new LambdaQueryWrapper<Knowledge>()
                        .select(Knowledge.class, field -> !"content".equals(field.getProperty()))
                        .eq(Knowledge::getStatus, 1)
                        .orderByDesc(Knowledge::getUpdateTime));

        // 3.批量装配作者昵称(装配维度跨到 user 表,模式与分类名装配一致:收集去重 + 一次 in 查询)
        List<Long> userIds = result.getRecords().stream()
                .map(Knowledge::getUserId)
                .distinct()
                .toList();
        Map<Long, String> authorNameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            userMapper.selectByIds(userIds).forEach(u ->
                    authorNameMap.put(u.getId(), u.getNickname() != null ? u.getNickname() : u.getUsername()));
        }

        // 4.转 VO
        List<PlazaVO> vos = result.getRecords().stream().map(k -> {
            PlazaVO vo = new PlazaVO();
            vo.setId(k.getId());
            vo.setTitle(k.getTitle());
            vo.setSummary(k.getSummary());
            vo.setAuthorName(authorNameMap.get(k.getUserId()));
            vo.setViewCount(k.getViewCount());
            vo.setLikeCount(k.getLikeCount());
            vo.setFavoriteCount(k.getFavoriteCount());
            vo.setUpdateTime(k.getUpdateTime());
            return vo;
        }).toList();

        return PageVO.of(result.getTotal(), result.getCurrent(), result.getSize(), vos);
    }
}
