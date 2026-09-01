package com.knowledge.dto;

import lombok.Data;

/**
 * 知识分页查询参数(GET query 参数自动绑定到字段)
 */
@Data
public class KnowledgePageDTO {

    private Long page;

    private Long size;

    private Long categoryId;

    private Integer status;

    private String keyword;

    private String orderBy;
}
