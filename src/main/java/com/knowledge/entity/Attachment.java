package com.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_attachment")
public class Attachment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long knowledgeId;

    private String fileName;

    private String filePath;

    private Long fileSize;

    private String fileType;

    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
