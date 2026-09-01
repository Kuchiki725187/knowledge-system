package com.knowledge.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BrowseHistoryVO {

    /**
     * 知识 id(前端跳转详情用)
     */
    private Long id;

    private String title;

    private String categoryName;

    /**
     * 知识是否已被删除(为 true 时前端显示"已删除"标记,不可点击)
     */
    private Boolean deleted;

    private LocalDateTime browseTime;
}
