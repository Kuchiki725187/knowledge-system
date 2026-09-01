package com.knowledge.service;

import com.knowledge.common.page.PageVO;
import com.knowledge.vo.FavoriteVO;

public interface FavoriteService {

    void add(Long knowledgeId);

    void remove(Long knowledgeId);

    PageVO<FavoriteVO> page(long page, long size);
}
