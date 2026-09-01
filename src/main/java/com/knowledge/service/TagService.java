package com.knowledge.service;

import com.knowledge.dto.TagSaveDTO;
import com.knowledge.vo.TagVO;

import java.util.List;

public interface TagService {

    List<TagVO> list();

    void create(TagSaveDTO dto);

    void delete(Long id);
}
