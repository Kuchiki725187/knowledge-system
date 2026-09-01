-- ============================================================
-- 个人知识管理系统 · 数据库初始化脚本
-- 环境: MySQL 8.x · 字符集: utf8mb4
-- ============================================================

CREATE DATABASE IF NOT EXISTS knowledge_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE knowledge_db;

-- ------------------------------------------------------------
-- 用户表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_user (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    username    VARCHAR(32)  NOT NULL COMMENT '登录名',
    password    VARCHAR(100) NOT NULL COMMENT '密码(BCrypt密文)',
    nickname    VARCHAR(32)  DEFAULT NULL COMMENT '昵称',
    avatar      VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '0-禁用 1-正常',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除:0-未删除 1-已删除',
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ------------------------------------------------------------
-- 知识表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_knowledge (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id     BIGINT       NOT NULL COMMENT '所属用户',
    title       VARCHAR(128) NOT NULL COMMENT '标题',
    summary     VARCHAR(512) DEFAULT NULL COMMENT '摘要(后期AI自动生成)',
    content     MEDIUMTEXT   COMMENT 'Markdown正文',
    category_id BIGINT       DEFAULT NULL COMMENT '所属分类',
    status      TINYINT      NOT NULL DEFAULT 0 COMMENT '0-草稿 1-发布(发布即进入知识大厅)',
    view_count  INT          NOT NULL DEFAULT 0 COMMENT '浏览量',
    like_count     INT       NOT NULL DEFAULT 0 COMMENT '点赞数(冗余计数,点赞/取消时维护)',
    favorite_count INT       NOT NULL DEFAULT 0 COMMENT '收藏数(冗余计数)',
    comment_count  INT       NOT NULL DEFAULT 0 COMMENT '评论数(冗余计数)',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除:0-未删除 1-已删除',
    KEY idx_user_updated (user_id, update_time),
    KEY idx_category (category_id),
    FULLTEXT KEY ft_title_content (title, content) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识表';

-- ------------------------------------------------------------
-- 分类表
-- deleted 用 BIGINT:0-未删除,删除时置为行 id
-- 配合 (user_id, name, deleted) 联合唯一,解决"删除后无法重建同名"问题
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_category (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id     BIGINT      NOT NULL COMMENT '所属用户',
    name        VARCHAR(32) NOT NULL COMMENT '分类名称',
    sort        INT         NOT NULL DEFAULT 0 COMMENT '排序(越小越靠前)',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     BIGINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除:0-未删除,删除时置为id',
    UNIQUE KEY uk_user_name (user_id, name, deleted),
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类表';

-- ------------------------------------------------------------
-- 标签表(结构与分类表一致)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_tag (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id     BIGINT      NOT NULL COMMENT '所属用户',
    name        VARCHAR(32) NOT NULL COMMENT '标签名称',
    sort        INT         NOT NULL DEFAULT 0 COMMENT '排序(预留)',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     BIGINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除:0-未删除,删除时置为id',
    UNIQUE KEY uk_user_name (user_id, name, deleted),
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签表';

-- ------------------------------------------------------------
-- 知识-标签关联表(多对多,物理删除)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_knowledge_tag (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    knowledge_id BIGINT   NOT NULL COMMENT '知识id',
    tag_id       BIGINT   NOT NULL COMMENT '标签id',
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_knowledge_tag (knowledge_id, tag_id),
    KEY idx_tag (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识标签关联表';

-- ------------------------------------------------------------
-- 浏览记录表(同一用户同一知识只保留最新一条,靠唯一索引 upsert)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_browse_history (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id      BIGINT   NOT NULL COMMENT '用户id',
    knowledge_id BIGINT   NOT NULL COMMENT '知识id',
    browse_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
    UNIQUE KEY uk_user_knowledge (user_id, knowledge_id),
    KEY idx_user_time (user_id, browse_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='浏览记录表';

-- ------------------------------------------------------------
-- 搜索日志表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_search_log (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id      BIGINT       NOT NULL COMMENT '用户id',
    keyword      VARCHAR(128) NOT NULL COMMENT '搜索词',
    result_count INT          NOT NULL DEFAULT 0 COMMENT '本次搜索结果数量',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '搜索时间',
    KEY idx_user_time (user_id, create_time),
    KEY idx_keyword (keyword)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搜索日志表';

-- ------------------------------------------------------------
-- 收藏表(唯一索引防重复收藏)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_favorite (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id      BIGINT   NOT NULL COMMENT '用户id',
    knowledge_id BIGINT   NOT NULL COMMENT '知识id',
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    UNIQUE KEY uk_user_knowledge (user_id, knowledge_id),
    KEY idx_user_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';

-- ------------------------------------------------------------
-- 附件表(文件存本地磁盘,表里只存元数据与相对路径)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_attachment (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id      BIGINT        NOT NULL COMMENT '所属用户',
    knowledge_id BIGINT        NOT NULL COMMENT '所属知识',
    file_name    VARCHAR(255)  NOT NULL COMMENT '原始文件名',
    file_path    VARCHAR(512)  NOT NULL COMMENT '存储相对路径(基于上传根目录)',
    file_size    BIGINT        NOT NULL DEFAULT 0 COMMENT '文件大小(字节)',
    file_type    VARCHAR(50)   DEFAULT NULL COMMENT 'MIME类型',
    create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    deleted      TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    KEY idx_knowledge (knowledge_id),
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='附件表';

-- ------------------------------------------------------------
-- 知识关联表(单向存储,双向查询;relation_type=1 手动相关)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_knowledge_relation (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id      BIGINT   NOT NULL COMMENT '所属用户',
    source_id    BIGINT   NOT NULL COMMENT '知识id(添加方)',
    target_id    BIGINT   NOT NULL COMMENT '对方知识id',
    relation_type TINYINT NOT NULL DEFAULT 1 COMMENT '1-相关(预留 AI 推荐)',
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_pair (source_id, target_id),
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识关联表';

-- ------------------------------------------------------------
-- 点赞表(唯一索引防重复点赞)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_like (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id      BIGINT   NOT NULL COMMENT '点赞用户',
    knowledge_id BIGINT   NOT NULL COMMENT '知识id',
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    UNIQUE KEY uk_user_knowledge (user_id, knowledge_id),
    KEY idx_knowledge (knowledge_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞表';

-- ------------------------------------------------------------
-- 评论表(一级平铺,不做嵌套回复;物理删除)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_comment (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id      BIGINT        NOT NULL COMMENT '评论人',
    knowledge_id BIGINT        NOT NULL COMMENT '知识id',
    content      VARCHAR(1000) NOT NULL COMMENT '评论内容',
    create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
    KEY idx_knowledge_time (knowledge_id, create_time),
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';
