package com.knowledge.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AttachmentVO {

    private Long id;

    private String fileName;

    private Long fileSize;

    private String fileType;

    private LocalDateTime createTime;
}
