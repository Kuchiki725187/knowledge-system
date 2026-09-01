package com.knowledge.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识大厅列表 VO:不含 content 大字段,含互动计数与作者
 */
@Data
public class PlazaVO {

    private Long id;

    private String title;

    private String summary;

    private String authorName;

    private Integer viewCount;

    private Integer likeCount;

    private Integer favoriteCount;

    private LocalDateTime updateTime;
}
