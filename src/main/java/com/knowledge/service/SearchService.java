package com.knowledge.service;

import com.knowledge.common.page.PageVO;
import com.knowledge.vo.KnowledgeVO;

public interface SearchService {

    PageVO<KnowledgeVO> search(String keyword, long page, long size);
}
