package com.knowledge.service;

import com.knowledge.common.page.PageVO;
import com.knowledge.dto.KnowledgePageDTO;
import com.knowledge.dto.KnowledgeSaveDTO;
import com.knowledge.vo.KnowledgeBaseVO;
import com.knowledge.vo.KnowledgeDetailVO;
import com.knowledge.vo.KnowledgeVO;

public interface KnowledgeService {

    KnowledgeBaseVO create(KnowledgeSaveDTO dto);

    KnowledgeBaseVO update(Long id, KnowledgeSaveDTO dto);

    void delete(Long id);

    KnowledgeDetailVO getDetail(Long id);

    PageVO<KnowledgeVO> page(KnowledgePageDTO dto);
}
