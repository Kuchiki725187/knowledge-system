package com.knowledge.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CategoryVO {

    private Long id;

    private String name;

    private Integer sort;

    private Long knowledgeCount;

    private LocalDateTime createTime;
}
