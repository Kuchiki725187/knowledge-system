package com.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.knowledge.entity.Knowledge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface KnowledgeMapper extends BaseMapper<Knowledge> {

    /**
     * ngram 全文检索:标题+正文匹配,按相关度降序、时间降序排列。
     * 注意:自定义 SQL 不会自动拼 @TableLogic 的 deleted=0,必须手写;
     * 明确列出字段而不是 SELECT *,是为了排除 content 大字段。
     * 第一个参数是 IPage,分页插件会自动拼 LIMIT 并生成 COUNT 语句。
     */
    @Select("SELECT id, user_id, title, summary, category_id, status, view_count, create_time, update_time "
            + "FROM t_knowledge "
            + "WHERE deleted = 0 AND user_id = #{userId} "
            + "AND MATCH(title, content) AGAINST(#{keyword} IN NATURAL LANGUAGE MODE) "
            + "ORDER BY MATCH(title, content) AGAINST(#{keyword} IN NATURAL LANGUAGE MODE) DESC, update_time DESC")
    IPage<Knowledge> searchByFullText(IPage<Knowledge> page,
                                      @Param("userId") Long userId,
                                      @Param("keyword") String keyword);
}
