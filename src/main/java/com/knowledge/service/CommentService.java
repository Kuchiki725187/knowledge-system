package com.knowledge.service;

import com.knowledge.common.page.PageVO;
import com.knowledge.dto.CommentSaveDTO;
import com.knowledge.vo.CommentVO;

public interface CommentService {

    void add(Long knowledgeId, CommentSaveDTO dto);

    PageVO<CommentVO> page(Long knowledgeId, long page, long size);

    void delete(Long commentId);
}
