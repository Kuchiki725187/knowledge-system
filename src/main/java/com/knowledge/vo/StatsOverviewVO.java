package com.knowledge.vo;

import lombok.Data;

@Data
public class StatsOverviewVO {

    private Long knowledgeCount;

    private Long categoryCount;

    private Long tagCount;

    private Long favoriteCount;
}
