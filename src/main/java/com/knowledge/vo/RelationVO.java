package com.knowledge.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RelationVO {

    /**
     * 关联记录 id(删除时用)
     */
    private Long id;

    /**
     * 对方知识 id
     */
    private Long knowledgeId;

    private String title;

    private String summary;

    private Integer relationType;

    private LocalDateTime createTime;
}
