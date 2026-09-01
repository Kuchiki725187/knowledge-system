# 个人知识管理系统 API 设计文档

> 版本 v1.0 · 2026-08-28 · 本文档是接口契约，实际开发以最新版为准

## 一、全局约定

| 项 | 约定 |
|----|------|
| Base URL | `http://localhost:8080` |
| 认证方式 | 请求头 `Authorization: Bearer {access_token}`，除标注【无需认证】外全部需要 |
| 统一响应 | `{"code": 200, "msg": "操作成功", "data": {...}}`，HTTP 状态码恒为 200，业务结果看 `code` |
| 分页请求 | `page`（默认 1 起）、`size`（默认 10，最大 50） |
| 分页响应 | `PageVO<T>`：`{"records": [], "total": 0, "current": 1, "size": 10}` |
| 时间格式 | `yyyy-MM-dd HH:mm:ss` |
| 数据权限 | 所有"我的 xx"接口自动按登录用户过滤，无法查看/操作他人数据 |

## 二、错误码总表

| 号段 | 模块 | 已定义 |
|------|------|--------|
| 200/400/401/403/404/500 | 通用 | 对齐 HTTP 语义 |
| 1001~1999 | 用户 | 1001 用户名已存在 · 1002 用户名或密码错误 · 1003 账号已禁用 · 1004 用户不存在 |
| 2001~2999 | 知识 | 2001 知识不存在(与"无权操作"统一返回,防探测) · 2002 预留 · 2003 关联不存在或已存在 |
| 3001~3099 | 分类 | 3001 分类不存在(与无权统一,防探测) · 3002 分类下有知识，禁止删除 · 3003 分类名称已存在 |
| 3101~3199 | 标签 | 3101 标签不存在(与无权统一,防探测) · 3102 标签名称已存在 |
| 4001~4199 | 收藏/点赞 | 4001 已收藏 · 4002 已点赞 |
| 5001~5999 | 搜索 | 5001 搜索关键词为空 |
| 6001~6199 | 统计 | — |

## 三、用户模块 /user

### 3.1 注册 【无需认证】

`POST /user/register`

```json
// 请求
{"username": "tom", "password": "123456", "nickname": "汤姆"}
// username: 3~32 位字母或数字；password: 6~64 位；nickname: ≤32 位可空

// 响应 data
{"id": 1, "username": "tom", "nickname": "汤姆"}
```

### 3.2 登录 【无需认证】

`POST /user/login`

```json
// 请求
{"username": "tom", "password": "123456"}

// 响应 data
{
  "accessToken": "eyJhbGciOi...",   // 有效期 2h，每次请求携带
  "refreshToken": "eyJhbGciOi...",  // 有效期 7d，仅用于换新 token
  "user": {"id": 1, "username": "tom", "nickname": "汤姆", "avatar": null}
}
```

### 3.3 刷新 token 【无需认证，凭 refreshToken】

`POST /user/refresh`

```json
// 请求
{"refreshToken": "eyJhbGciOi..."}
// 响应 data：同登录响应（新一轮双 token，refreshToken 轮换更新）
```

### 3.4 当前用户信息

`GET /user/me` → data：同登录响应中 `user` 结构

### 3.5 修改个人信息

`PUT /user/me` 请求 `{"nickname": "新昵称", "avatar": "http://..."}` → data：更新后的 user

## 四、知识模块 /knowledge

### 4.1 创建知识

`POST /knowledge`

```json
// 请求
{
  "title": "Redis 持久化笔记",        // 必填 1~128
  "content": "# Markdown 正文...",     // 必填,最大 65535
  "categoryId": 1,                     // 可空
  "tags": ["Redis", "缓存"],           // 标签名数组,不存在自动创建
  "status": 1                          // 0-草稿 1-发布,默认 0
}
// 响应 data
{"id": 100, "title": "Redis 持久化笔记", "status": 1}
```

### 4.2 更新知识

`PUT /knowledge/{id}` 请求同创建（全量更新，tags 全量替换）

### 4.3 删除知识

`DELETE /knowledge/{id}` → data：null（逻辑删除）

### 4.4 知识详情

`GET /knowledge/{id}`

```json
// 响应 data
{
  "id": 100, "title": "...", "content": "# Markdown 原文",
  "categoryId": 1, "categoryName": "后端",
  "tags": [{"id": 3, "name": "Redis"}],
  "status": 1, "viewCount": 5, "favoriteCount": 0,
  "createTime": "2026-08-28 18:00:00", "updateTime": "2026-08-28 19:00:00"
}
// 副作用:浏览计数 +1、写入浏览记录
```

### 4.5 分页查询我的知识

`GET /knowledge/page?categoryId=&status=&keyword=&orderBy=update_time&page=1&size=10`

| 参数 | 说明 |
|------|------|
| categoryId | 可空,按分类过滤 |
| status | 可空,0/1 |
| keyword | 可空,标题模糊匹配(仅本模块内,全局搜索走 /search) |
| orderBy | `update_time`(默认)/`view_count`/`create_time` |

data：`PageVO<KnowledgeVO>`，KnowledgeVO 不含 content 正文（列表页不传大字段）

## 五、分类模块 /category

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/category/list` | 我的分类,按 sort 升序,含 `knowledgeCount` |
| POST | `/category` | 创建 `{name(1~32,同用户唯一), sort}` |
| PUT | `/category/{id}` | 改名/改排序 |
| DELETE | `/category/{id}` | 删除;分类下还有知识时报 3002 禁止删除 |

## 六、标签模块 /tag

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/tag/list` | 我的标签,含 `usageCount`,按使用次数降序 |
| POST | `/tag` | 创建 `{name(1~32,同用户唯一)}` |
| DELETE | `/tag/{id}` | 删除标签并清理知识关联 |

> 标签通常不单独创建：创建/更新知识时传 `tags` 名字数组,后端自动"存在复用、不存在新建"

## 七、点赞与评论

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/like/{knowledgeId}` | 点赞;重复报 4002(唯一索引兜底) |
| DELETE | `/like/{knowledgeId}` | 取消点赞(幂等) |
| POST | `/comment/{knowledgeId}` | 发表评论 `{content(1~1000)}`,评论数同步维护 |
| GET | `/comment/{knowledgeId}/page?page=&size=` | 评论分页,按时间正序,含评论者昵称 |
| DELETE | `/comment/{commentId}` | 删除自己的评论 |

> 点赞状态通过详情的 `isLiked` 返回(用户私有状态不入公共缓存,实时补查)

## 八、收藏模块 /favorite

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/favorite/{knowledgeId}` | 收藏;重复收藏报 4001(数据库唯一索引兜底) |
| DELETE | `/favorite/{knowledgeId}` | 取消收藏 |
| GET | `/favorite/page?page=&size=` | 我的收藏,按收藏时间倒序,含知识摘要信息 |

## 九、浏览记录 /browse-history

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/browse-history/recent?limit=20` | 最近浏览(同一知识只留最新一条,limit 最大 50) |

> 浏览记录不提供主动写入接口 —— 访问知识详情时自动记录

## 十、搜索模块 /search 【第八阶段实现,MySQL 全文索引起步】

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/search?keyword=&page=&size=` | 全局搜索(标题+正文),按相关度/时间排序,自动记录搜索日志,关键词必填(5001) |

## 十一、知识关联 /knowledge

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/knowledge/{id}/relations` | `{targetId, relationType: 1-相关}` 双向写入 |
| GET | `/knowledge/{id}/relations` | 该知识的全部关联(含对方标题摘要) |
| DELETE | `/knowledge/relations/{relationId}` | 删除一条关联 |

## 十二、统计模块 /stats 【第九阶段随 Redis 缓存实现】

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/stats/overview` | data: `{knowledgeCount, categoryCount, tagCount, favoriteCount}` |
| GET | `/stats/trend?days=7` | data: `[{date:"2026-08-28", count:3}]`,近 N 天(≤30)每日新增 |
| GET | `/stats/hot?limit=10` | data: `[{id, title, viewCount}]` 浏览量 Top N |

## 十三、关键设计决策记录

| # | 决策 | 理由 |
|---|------|------|
| 1 | 标签随知识传**名字数组**自动 upsert | 前端不必先建标签,体验好;幂等 |
| 2 | 草稿/发布用 `status` 字段,不设独立发布接口 | 一个字段能表达的状态不引入新接口 |
| 3 | 分类删除:有关联知识时**禁止**而非级联 | 数据安全优先,提示用户先移走知识 |
| 4 | 双 token:access 2h + refresh 7d,refresh 时**轮换**新 refreshToken | access 泄露损失上限 2h;轮换降低 refreshToken 被重放风险 |
| 5 | 列表接口不返回 `content` 正文 | 避免大字段拖慢列表查询与传输 |
| 6 | 浏览计数/记录在详情接口内**旁路**完成 | 前端零成本;实现上用异步不阻塞主查询(第九阶段) |
| 7 | 更新/删除时"不存在"与"非本人"统一返回 2001 | 避免攻击者用响应差异探测他人知识 id 是否存在(防 IDOR 探测) |
