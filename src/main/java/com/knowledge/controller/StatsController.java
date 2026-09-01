package com.knowledge.controller;

import com.knowledge.common.result.Result;
import com.knowledge.service.StatsService;
import com.knowledge.vo.HotItemVO;
import com.knowledge.vo.StatsOverviewVO;
import com.knowledge.vo.TrendItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/overview")
    public Result<StatsOverviewVO> overview() {
        return Result.ok(statsService.overview());
    }

    @GetMapping("/trend")
    public Result<List<TrendItemVO>> trend(@RequestParam(defaultValue = "7") int days) {
        return Result.ok(statsService.trend(days));
    }

    @GetMapping("/hot")
    public Result<List<HotItemVO>> hot(@RequestParam(defaultValue = "10") int limit) {
        return Result.ok(statsService.hot(limit));
    }
}
