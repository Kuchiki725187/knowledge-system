package com.knowledge.service;

import com.knowledge.vo.HotItemVO;
import com.knowledge.vo.StatsOverviewVO;
import com.knowledge.vo.TrendItemVO;

import java.util.List;

public interface StatsService {

    StatsOverviewVO overview();

    List<TrendItemVO> trend(int days);

    List<HotItemVO> hot(int limit);
}
