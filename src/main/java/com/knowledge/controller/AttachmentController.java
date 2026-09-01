package com.knowledge.controller;

import com.knowledge.common.result.Result;
import com.knowledge.service.AttachmentService;
import com.knowledge.vo.AttachmentVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/attachment")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    /**
     * 上传附件(表单 file 字段)
     */
    @PostMapping("/{knowledgeId}")
    public Result<AttachmentVO> upload(@PathVariable Long knowledgeId,
                                       @RequestParam("file") MultipartFile file) {
        return Result.ok(attachmentService.upload(knowledgeId, file));
    }

    /**
     * 知识的附件列表
     */
    @GetMapping("/list/{knowledgeId}")
    public Result<List<AttachmentVO>> list(@PathVariable Long knowledgeId) {
        return Result.ok(attachmentService.list(knowledgeId));
    }

    /**
     * 下载附件(不走统一 Result,直接写文件流)
     */
    @GetMapping("/download/{attachmentId}")
    public void download(@PathVariable Long attachmentId, HttpServletResponse response) {
        attachmentService.download(attachmentId, response);
    }

    /**
     * 删除附件
     */
    @DeleteMapping("/{attachmentId}")
    public Result<Void> delete(@PathVariable Long attachmentId) {
        attachmentService.delete(attachmentId);
        return Result.ok();
    }
}
