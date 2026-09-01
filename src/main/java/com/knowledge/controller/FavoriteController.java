package com.knowledge.controller;

import com.knowledge.common.page.PageVO;
import com.knowledge.common.result.Result;
import com.knowledge.service.FavoriteService;
import com.knowledge.vo.FavoriteVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/favorite")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/{knowledgeId}")
    public Result<Void> add(@PathVariable Long knowledgeId) {
        favoriteService.add(knowledgeId);
        return Result.ok();
    }

    @DeleteMapping("/{knowledgeId}")
    public Result<Void> remove(@PathVariable Long knowledgeId) {
        favoriteService.remove(knowledgeId);
        return Result.ok();
    }

    @GetMapping("/page")
    public Result<PageVO<FavoriteVO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(favoriteService.page(page, size));
    }
}
