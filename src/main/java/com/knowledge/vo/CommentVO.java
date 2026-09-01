package com.knowledge.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentVO {

    private Long id;

    private String content;

    /**
     * 评论人 id(前端用它判断是否自己的评论,控制删除按钮显隐)
     */
    private Long userId;

    private String authorName;

    private LocalDateTime createTime;
}
