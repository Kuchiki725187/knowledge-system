package com.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.knowledge.common.context.BaseContext;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.dto.CategorySaveDTO;
import com.knowledge.entity.Category;
import com.knowledge.entity.Knowledge;
import com.knowledge.mapper.CategoryMapper;
import com.knowledge.mapper.KnowledgeMapper;
import com.knowledge.service.CategoryService;
import com.knowledge.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final KnowledgeMapper knowledgeMapper;

    /**
     * 查询我的分类列表(含每个分类的知识数)
     * @return
     */
    @Override
    public List<CategoryVO> list() {
        // 1.查当前用户的未删除分类,按 sort 升序(手动加 deleted=0,因为 Category 没配 @TableLogic)
        Long userId = BaseContext.getUserId();
        List<Category> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getUserId, userId)
                        .eq(Category::getDeleted, 0L)
                        .orderByAsc(Category::getSort)
                        .orderByAsc(Category::getCreateTime));

        // 2.一条 group by 语句统计每个分类下的知识数,避免循环查库(N+1)
        Map<Long, Long> countMap = new HashMap<>();
        List<Map<String, Object>> countRows = knowledgeMapper.selectMaps(
                new QueryWrapper<Knowledge>()
                        .select("category_id", "count(*) as cnt")
                        .eq("user_id", userId)
                        .isNotNull("category_id")
                        .groupBy("category_id"));
        for (Map<String, Object> row : countRows) {
            countMap.put(Long.valueOf(row.get("category_id").toString()),
                    Long.valueOf(row.get("cnt").toString()));
        }

        // 3.转 VO 并填入知识数(没有知识的分类默认 0)
        return categories.stream().map(c -> {
            CategoryVO vo = new CategoryVO();
            vo.setId(c.getId());
            vo.setName(c.getName());
            vo.setSort(c.getSort());
            vo.setKnowledgeCount(countMap.getOrDefault(c.getId(), 0L));
            vo.setCreateTime(c.getCreateTime());
            return vo;
        }).toList();
    }

    /**
     * 创建分类
     * @param dto
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(CategorySaveDTO dto) {
        Long userId = BaseContext.getUserId();
        // 1.同一用户下查重(未删除的行)
        checkNameUnique(userId, dto.getName(), null);
        // 2.插入,deleted 默认 0
        Category category = new Category();
        category.setUserId(userId);
        category.setName(dto.getName());
        category.setSort(dto.getSort() == null ? 0 : dto.getSort());
        categoryMapper.insert(category);
    }

    /**
     * 更新分类(改名/改排序)
     * @param id
     * @param dto
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CategorySaveDTO dto) {
        Long userId = BaseContext.getUserId();
        // 1.校验分类存在且属于本人
        Category category = getOwnedCategory(id, userId);
        // 2.改名时查重(排除自己,否则改回自己的名字会误报重名)
        checkNameUnique(userId, dto.getName(), id);
        // 3.更新字段(sort 未传保持原值)
        category.setName(dto.getName());
        category.setSort(dto.getSort() == null ? category.getSort() : dto.getSort());
        categoryMapper.updateById(category);
    }

    /**
     * 删除分类(分类下还有知识时禁止)
     * @param id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long userId = BaseContext.getUserId();
        // 1.校验归属
        getOwnedCategory(id, userId);
        // 2.分类下还有知识时报 3002,提示先移走知识(决策 3:不级联删除)
        Long count = knowledgeMapper.selectCount(new LambdaQueryWrapper<Knowledge>()
                .eq(Knowledge::getUserId, userId)
                .eq(Knowledge::getCategoryId, id));
        if (count > 0) {
            throw new BusinessException(3002, "分类下还有知识，请先移动或删除");
        }
        // 3.逻辑删除:deleted 置为行 id 而不是 1 —— 已删除行的第三列各不相同,
        //   永远不占 (user_id, name, deleted) 联合唯一索引的坑,删除后可重建同名
        categoryMapper.update(null, new LambdaUpdateWrapper<Category>()
                .eq(Category::getId, id)
                .set(Category::getDeleted, id));
    }

    /**
     * 校验分类存在且属于当前用户。"不存在"与"非本人"统一报 3001,防探测
     */
    private Category getOwnedCategory(Long id, Long userId) {
        // userId 直接放进查询条件,查不到就统一报 3001
        Category category = categoryMapper.selectOne(new LambdaQueryWrapper<Category>()
                .eq(Category::getId, id)
                .eq(Category::getUserId, userId)
                .eq(Category::getDeleted, 0L));
        if (category == null) {
            throw new BusinessException(3001, "分类不存在");
        }
        return category;
    }

    /**
     * 同用户未删除的分类不允许重名;excludeId 用于改名时排除自己
     */
    private void checkNameUnique(Long userId, String name, Long excludeId) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<Category>()
                .eq(Category::getUserId, userId)
                .eq(Category::getName, name)
                .eq(Category::getDeleted, 0L)
                .ne(excludeId != null, Category::getId, excludeId);
        if (categoryMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(3003, "分类名称已存在");
        }
    }
}
