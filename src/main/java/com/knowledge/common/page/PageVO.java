package com.knowledge.common.page;

import lombok.Data;

import java.util.List;

/**
 * 全项目统一的分页响应结构
 */
@Data
public class PageVO<T> {

    private List<T> records;

    private Long total;

    private Long current;

    private Long size;

    public static <T> PageVO<T> of(Long total, Long current, Long size, List<T> records) {
        PageVO<T> vo = new PageVO<>();
        vo.setRecords(records);
        vo.setTotal(total);
        vo.setCurrent(current);
        vo.setSize(size);
        return vo;
    }
}
