package com.knowledge.service;

import com.knowledge.vo.BrowseHistoryVO;

import java.util.List;

public interface BrowseHistoryService {

    List<BrowseHistoryVO> recent(int limit);
}
