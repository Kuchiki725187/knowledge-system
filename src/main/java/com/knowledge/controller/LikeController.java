package com.knowledge.controller;

import com.knowledge.common.result.Result;
import com.knowledge.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/like")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    /**
     * 点赞
     */
    @PostMapping("/{knowledgeId}")
    public Result<Void> like(@PathVariable Long knowledgeId) {
        likeService.like(knowledgeId);
        return Result.ok();
    }

    /**
     * 取消点赞
     */
    @DeleteMapping("/{knowledgeId}")
    public Result<Void> unlike(@PathVariable Long knowledgeId) {
        likeService.unlike(knowledgeId);
        return Result.ok();
    }
}
