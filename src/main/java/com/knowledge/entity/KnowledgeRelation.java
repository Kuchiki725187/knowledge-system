package com.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_knowledge_relation")
public class KnowledgeRelation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long sourceId;

    private Long targetId;

    private Integer relationType;

    private LocalDateTime createTime;
}
