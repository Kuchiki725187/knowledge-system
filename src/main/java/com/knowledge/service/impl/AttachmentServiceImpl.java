package com.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.common.context.BaseContext;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.entity.Attachment;
import com.knowledge.entity.Knowledge;
import com.knowledge.mapper.AttachmentMapper;
import com.knowledge.mapper.KnowledgeMapper;
import com.knowledge.service.AttachmentService;
import com.knowledge.vo.AttachmentVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    /**
     * 允许上传的扩展名白名单(防上传可执行文件等危险类型)
     */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "md", "txt", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "png", "jpg", "jpeg", "gif", "webp", "zip", "rar", "7z", "csv", "json", "sql");

    private final AttachmentMapper attachmentMapper;
    private final KnowledgeMapper knowledgeMapper;

    /**
     * 上传根目录
     */
    @Value("${app.upload.dir}")
    private String uploadDir;

    /**
     * 上传附件
     * @param knowledgeId 所属知识
     * @param file        文件
     * @return 附件信息
     */
    @Override
    public AttachmentVO upload(Long knowledgeId, MultipartFile file) {
        Long userId = BaseContext.getUserId();

        // 1.校验知识存在且属于当前用户(只有作者能上传附件)
        Knowledge knowledge = knowledgeMapper.selectById(knowledgeId);
        if (knowledge == null || !knowledge.getUserId().equals(userId)) {
            throw new BusinessException(2001, "知识不存在");
        }

        // 2.基本校验:文件非空、大小非 0
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择要上传的文件");
        }

        // 3.扩展名白名单校验(小写匹配,防止 .EXE 绕过)
        String originalName = StringUtils.cleanPath(file.getOriginalFilename());
        String ext = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0) {
            ext = originalName.substring(dotIndex + 1).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException(400, "不支持的文件类型: ." + ext);
        }

        // 4.存储路径拼接:uploads/{userId}/{日期}/{uuid}.{ext}
        //   UUID 重命名:文件名不可控,直接用随机名,规避路径穿越
        Path relativePath = Paths.get(userId.toString(), LocalDate.now().toString(),
                UUID.randomUUID().toString().replace("-", "") + "." + ext);
        Path absolutePath = Paths.get(uploadDir).resolve(relativePath).normalize();
        try {
            Files.createDirectories(absolutePath.getParent());
            file.transferTo(absolutePath);
        } catch (IOException e) {
            log.error("附件存储失败, knowledgeId={}", knowledgeId, e);
            throw new BusinessException(500, "文件保存失败");
        }

        // 5.落库元数据
        Attachment attachment = new Attachment();
        attachment.setUserId(userId);
        attachment.setKnowledgeId(knowledgeId);
        attachment.setFileName(originalName);
        attachment.setFilePath(relativePath.toString().replace('\\', '/'));
        attachment.setFileSize(file.getSize());
        attachment.setFileType(file.getContentType());
        attachmentMapper.insert(attachment);

        return toVO(attachment);
    }

    /**
     * 知识的附件列表(知识可见才可查看)
     * @param knowledgeId
     * @return
     */
    @Override
    public List<AttachmentVO> list(Long knowledgeId) {
        Long userId = BaseContext.getUserId();
        // 校验知识可见(作者或已发布),与详情同准入
        Knowledge knowledge = knowledgeMapper.selectById(knowledgeId);
        if (knowledge == null
                || (knowledge.getStatus() != 1 && !knowledge.getUserId().equals(userId))) {
            throw new BusinessException(2001, "知识不存在");
        }
        return attachmentMapper.selectList(new LambdaQueryWrapper<Attachment>()
                        .eq(Attachment::getKnowledgeId, knowledgeId)
                        .orderByDesc(Attachment::getCreateTime))
                .stream().map(this::toVO).toList();
    }

    /**
     * 下载附件(知识可见才可下载)
     * @param attachmentId
     * @param response
     */
    @Override
    public void download(Long attachmentId, HttpServletResponse response) {
        Long userId = BaseContext.getUserId();

        // 1.查附件记录(逻辑删除的查不到)
        Attachment attachment = attachmentMapper.selectById(attachmentId);
        if (attachment == null) {
            throw new BusinessException(400, "附件不存在");
        }

        // 2.校验附件所属知识可见
        Knowledge knowledge = knowledgeMapper.selectById(attachment.getKnowledgeId());
        if (knowledge == null
                || (knowledge.getStatus() != 1 && !knowledge.getUserId().equals(userId))) {
            throw new BusinessException(2001, "知识不存在");
        }

        // 3.读文件并流式写出
        Path absolutePath = Paths.get(uploadDir).resolve(attachment.getFilePath()).normalize();
        try {
            if (!Files.exists(absolutePath)) {
                log.error("附件文件丢失, attachmentId={}, path={}", attachmentId, attachment.getFilePath());
                throw new BusinessException(500, "文件已丢失");
            }
            // 文件名用 RFC 5987 编码,中文文件名在响应头里不乱码
            String encodedName = URLEncoder.encode(attachment.getFileName(), StandardCharsets.UTF_8)
                    .replace("+", "%20");
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition",
                    "attachment; filename*=UTF-8''" + encodedName);
            response.setContentLengthLong(attachment.getFileSize());
            try (InputStream in = Files.newInputStream(absolutePath);
                 OutputStream out = response.getOutputStream()) {
                in.transferTo(out);
            }
        } catch (IOException e) {
            log.error("附件下载失败, attachmentId={}", attachmentId, e);
            throw new BusinessException(500, "下载失败");
        }
    }

    /**
     * 删除附件(仅作者;删记录+删物理文件)
     * @param attachmentId
     */
    @Override
    public void delete(Long attachmentId) {
        Long userId = BaseContext.getUserId();

        // 1.校验附件存在且属于当前用户
        Attachment attachment = attachmentMapper.selectById(attachmentId);
        if (attachment == null || !attachment.getUserId().equals(userId)) {
            throw new BusinessException(400, "附件不存在");
        }

        // 2.逻辑删除记录
        attachmentMapper.deleteById(attachmentId);

        // 3.物理删除文件(失败只记日志,不阻断:文件残留可人工清理,记录必须删干净)
        try {
            Files.deleteIfExists(Paths.get(uploadDir).resolve(attachment.getFilePath()).normalize());
        } catch (IOException e) {
            log.error("附件文件删除失败, attachmentId={}, path={}", attachmentId, attachment.getFilePath(), e);
        }
    }

    private AttachmentVO toVO(Attachment attachment) {
        AttachmentVO vo = new AttachmentVO();
        vo.setId(attachment.getId());
        vo.setFileName(attachment.getFileName());
        vo.setFileSize(attachment.getFileSize());
        vo.setFileType(attachment.getFileType());
        vo.setCreateTime(attachment.getCreateTime());
        return vo;
    }
}
