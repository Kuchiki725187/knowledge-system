package com.knowledge.service;

import com.knowledge.vo.AttachmentVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AttachmentService {

    AttachmentVO upload(Long knowledgeId, MultipartFile file);

    List<AttachmentVO> list(Long knowledgeId);

    void download(Long attachmentId, HttpServletResponse response);

    void delete(Long attachmentId);
}
