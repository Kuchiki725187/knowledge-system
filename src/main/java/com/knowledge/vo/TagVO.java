package com.knowledge.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TagVO {

    private Long id;

    private String name;

    private Long usageCount;

    private LocalDateTime createTime;
}
