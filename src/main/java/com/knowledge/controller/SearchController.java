package com.knowledge.controller;

import com.knowledge.common.page.PageVO;
import com.knowledge.common.result.Result;
import com.knowledge.service.SearchService;
import com.knowledge.vo.KnowledgeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public Result<PageVO<KnowledgeVO>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(searchService.search(keyword, page, size));
    }
}
