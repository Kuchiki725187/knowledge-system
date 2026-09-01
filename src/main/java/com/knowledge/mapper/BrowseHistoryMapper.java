package com.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.entity.BrowseHistory;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BrowseHistoryMapper extends BaseMapper<BrowseHistory> {

    /**
     * 原子 upsert:同一用户同一知识只保留一行,重复浏览只刷新时间
     * (唯一索引 uk_user_knowledge 冲突时走 UPDATE 分支)
     */
    @Insert("INSERT INTO t_browse_history (user_id, knowledge_id, browse_time) "
            + "VALUES (#{userId}, #{knowledgeId}, NOW()) "
            + "ON DUPLICATE KEY UPDATE browse_time = NOW()")
    void upsert(@Param("userId") Long userId, @Param("knowledgeId") Long knowledgeId);
}
