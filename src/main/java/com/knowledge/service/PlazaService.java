package com.knowledge.service;

import com.knowledge.common.page.PageVO;
import com.knowledge.vo.PlazaVO;

public interface PlazaService {

    PageVO<PlazaVO> page(long page, long size);
}
