package com.knowledge.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class KnowledgeDetailVO {

    private Long id;

    private String title;

    private String content;

    private Long categoryId;

    private String categoryName;

    private List<TagItem> tags;

    /**
     * 作者昵称(社区场景展示)
     */
    private String authorName;

    /**
     * 作者用户 id(前端用它判断"是否自己的知识",控制编辑/删除按钮显隐)
     */
    private Long authorId;

    private Integer status;

    private Integer viewCount;

    private Integer likeCount;

    private Integer favoriteCount;

    private Integer commentCount;

    /**
     * 当前登录用户是否已点赞(用户私有状态,不入公共缓存,每次实时补查)
     */
    private Boolean isLiked;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @Data
    public static class TagItem {

        private Long id;

        private String name;
    }
}
