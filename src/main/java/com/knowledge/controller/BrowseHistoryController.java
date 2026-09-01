package com.knowledge.controller;

import com.knowledge.common.result.Result;
import com.knowledge.service.BrowseHistoryService;
import com.knowledge.vo.BrowseHistoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/browse-history")
@RequiredArgsConstructor
public class BrowseHistoryController {

    private final BrowseHistoryService browseHistoryService;

    @GetMapping("/recent")
    public Result<List<BrowseHistoryVO>> recent(
            @RequestParam(defaultValue = "20") int limit) {
        return Result.ok(browseHistoryService.recent(limit));
    }
}
