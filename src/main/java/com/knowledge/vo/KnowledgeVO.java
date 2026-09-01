package com.knowledge.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识列表 VO:刻意不含 content 大字段(决策 5)
 */
@Data
public class KnowledgeVO {

    private Long id;

    private String title;

    private String summary;

    private Long categoryId;

    private String categoryName;

    private Integer status;

    private Integer viewCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
