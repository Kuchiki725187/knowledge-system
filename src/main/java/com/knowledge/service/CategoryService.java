package com.knowledge.service;

import com.knowledge.dto.CategorySaveDTO;
import com.knowledge.vo.CategoryVO;

import java.util.List;

public interface CategoryService {

    List<CategoryVO> list();

    void create(CategorySaveDTO dto);

    void update(Long id, CategorySaveDTO dto);

    void delete(Long id);
}
