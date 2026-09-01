package com.knowledge.controller;

import com.knowledge.common.page.PageVO;
import com.knowledge.common.result.Result;
import com.knowledge.dto.CommentSaveDTO;
import com.knowledge.service.CommentService;
import com.knowledge.vo.CommentVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/comment")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 发表评论
     */
    @PostMapping("/{knowledgeId}")
    public Result<Void> add(@PathVariable Long knowledgeId, @RequestBody @Valid CommentSaveDTO dto) {
        commentService.add(knowledgeId, dto);
        return Result.ok();
    }

    /**
     * 评论分页
     */
    @GetMapping("/{knowledgeId}/page")
    public Result<PageVO<CommentVO>> page(
            @PathVariable Long knowledgeId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(commentService.page(knowledgeId, page, size));
    }

    /**
     * 删除自己的评论
     */
    @DeleteMapping("/{commentId}")
    public Result<Void> delete(@PathVariable Long commentId) {
        commentService.delete(commentId);
        return Result.ok();
    }
}
