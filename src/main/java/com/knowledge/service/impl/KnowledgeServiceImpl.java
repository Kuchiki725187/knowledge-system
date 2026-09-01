package com.knowledge.service.impl;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledge.common.context.BaseContext;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.common.page.PageVO;
import com.knowledge.dto.KnowledgePageDTO;
import com.knowledge.dto.KnowledgeSaveDTO;
import com.knowledge.entity.BrowseHistory;
import com.knowledge.entity.Category;
import com.knowledge.entity.Favorite;
import com.knowledge.entity.Knowledge;
import com.knowledge.entity.KnowledgeComment;
import com.knowledge.entity.KnowledgeLike;
import com.knowledge.entity.KnowledgeTag;
import com.knowledge.entity.Tag;
import com.knowledge.entity.User;
import com.knowledge.mapper.BrowseHistoryMapper;
import com.knowledge.mapper.CategoryMapper;
import com.knowledge.mapper.FavoriteMapper;
import com.knowledge.mapper.KnowledgeCommentMapper;
import com.knowledge.mapper.KnowledgeLikeMapper;
import com.knowledge.mapper.KnowledgeMapper;
import com.knowledge.mapper.KnowledgeTagMapper;
import com.knowledge.mapper.TagMapper;
import com.knowledge.mapper.UserMapper;
import com.knowledge.service.KnowledgeService;
import com.knowledge.vo.KnowledgeBaseVO;
import com.knowledge.vo.KnowledgeDetailVO;
import com.knowledge.vo.KnowledgeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {

    /**
     * 详情缓存双轨:public为发布内容(所有用户共享一份),owner为作者视角(草稿可见)。
     * 更新/删除时两把 key 都要删,否则会读到旧内容
     */
    private static final String DETAIL_CACHE_PUBLIC_PREFIX = "knowledge:detail:public:";
    private static final String DETAIL_CACHE_OWNER_PREFIX = "knowledge:detail:owner:";
    /**
     * 正常TTL加随机偏移防雪崩;空值标记用短TTL防穿透
     */
    private static final Duration DETAIL_CACHE_TTL = Duration.ofMinutes(30);
    private static final Duration DETAIL_NULL_TTL = Duration.ofMinutes(2);
    /**
     * 浏览计数 Redis key:浏览时 INCR,定时任务批量落库
     */
    private static final String VIEW_COUNT_KEY_PREFIX = "knowledge:view:";

    private final KnowledgeMapper knowledgeMapper;
    private final TagMapper tagMapper;
    private final KnowledgeTagMapper knowledgeTagMapper;
    private final CategoryMapper categoryMapper;
    private final BrowseHistoryMapper browseHistoryMapper;
    private final FavoriteMapper favoriteMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final UserMapper userMapper;
    private final KnowledgeLikeMapper likeMapper;
    private final KnowledgeCommentMapper commentMapper;

    /**
     * 创建知识
     * @param dto
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeBaseVO create(KnowledgeSaveDTO dto) {
        // 1.从 ThreadLocal 取当前登录用户,知识归属强制为本人,前端传什么都不算
        Long userId = BaseContext.getUserId();

        // 2.组装知识实体并落库(status 未传默认草稿)
        Knowledge knowledge = new Knowledge();
        knowledge.setUserId(userId);
        knowledge.setTitle(dto.getTitle());
        knowledge.setContent(dto.getContent());
        knowledge.setCategoryId(dto.getCategoryId());
        knowledge.setStatus(dto.getStatus() == null ? 0 : dto.getStatus());
        knowledgeMapper.insert(knowledge);

        // 3.处理标签:不存在的自动创建,并建立知识-标签关联
        bindTags(knowledge.getId(), userId, dto.getTags());

        // 4.返回基础信息(含回填的主键 id)
        return toBaseVO(knowledge);
    }

    /**
     * 更新知识
     * @param id
     * @param dto
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeBaseVO update(Long id, KnowledgeSaveDTO dto) {
        // 1.校验知识存在且属于当前用户(数据隔离)
        Long userId = BaseContext.getUserId();
        Knowledge knowledge = getOwnedKnowledge(id, userId);

        // 2.覆盖更新字段(status 未传则保持原值)
        knowledge.setTitle(dto.getTitle());
        knowledge.setContent(dto.getContent());
        knowledge.setCategoryId(dto.getCategoryId());
        if (dto.getStatus() != null) {
            knowledge.setStatus(dto.getStatus());
        }
        knowledgeMapper.updateById(knowledge);

        // 3.标签全量替换:先清掉旧关联,再按新标签重建
        knowledgeTagMapper.delete(
                new LambdaQueryWrapper<KnowledgeTag>().eq(KnowledgeTag::getKnowledgeId, id));
        bindTags(id, userId, dto.getTags());

        // 4.Cache Aside:写库后删缓存(公共+作者两把都要删),下次读取时按需重建
        stringRedisTemplate.delete(DETAIL_CACHE_PUBLIC_PREFIX + id);
        stringRedisTemplate.delete(DETAIL_CACHE_OWNER_PREFIX + id);

        // 5.返回基础信息
        return toBaseVO(knowledge);
    }

    /**
     * 删除知识
     * @param id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        // 1.校验知识存在且属于当前用户(数据隔离)
        Long userId = BaseContext.getUserId();
        getOwnedKnowledge(id, userId);

        // 2.逻辑删除知识本体(@TableLogic 使 deleteById 实际执行 UPDATE deleted=1)
        knowledgeMapper.deleteById(id);

        // 3.清理知识-标签关联(关联表无逻辑删除,物理删除)
        knowledgeTagMapper.delete(
                new LambdaQueryWrapper<KnowledgeTag>().eq(KnowledgeTag::getKnowledgeId, id));

        // 4.级联清理收藏记录,避免收藏列表出现已删除的知识
        favoriteMapper.delete(
                new LambdaQueryWrapper<Favorite>().eq(Favorite::getKnowledgeId, id));

        // 4.1 级联清理点赞记录
        likeMapper.delete(
                new LambdaQueryWrapper<KnowledgeLike>().eq(KnowledgeLike::getKnowledgeId, id));

        // 4.2 级联清理评论
        commentMapper.delete(
                new LambdaQueryWrapper<KnowledgeComment>().eq(KnowledgeComment::getKnowledgeId, id));

        // 5.级联清理浏览记录,同理
        browseHistoryMapper.delete(
                new LambdaQueryWrapper<BrowseHistory>().eq(BrowseHistory::getKnowledgeId, id));

        // 6.Cache Aside:写库后删缓存(公共+作者两把),下次读取时重建
        stringRedisTemplate.delete(DETAIL_CACHE_PUBLIC_PREFIX + id);
        stringRedisTemplate.delete(DETAIL_CACHE_OWNER_PREFIX + id);
    }

    /**
     * 获取知识详情(带缓存,社区可见性模型)
     * 作者本人可见草稿;其他用户仅可见已发布知识
     * @param id
     * @return
     */
    @Override
    public KnowledgeDetailVO getDetail(Long id) {
        Long userId = BaseContext.getUserId();

        // 旁路:每次浏览都计数+记历史(无论缓存命中与否);失败不影响详情返回
        //   计数只写 Redis(INCR 原子操作),由定时任务批量落库,避免每次浏览打 MySQL 行锁
        try {
            stringRedisTemplate.opsForValue().increment(VIEW_COUNT_KEY_PREFIX + id);
            browseHistoryMapper.upsert(userId, id);
        } catch (Exception e) {
            log.error("浏览计数/记录失败, knowledgeId={}", id, e);
        }

        String publicKey = DETAIL_CACHE_PUBLIC_PREFIX + id;
        String ownerKey = DETAIL_CACHE_OWNER_PREFIX + id;

        // 1.先查公共缓存:已发布内容对所有人一致,作者本人也适用
        String cached = stringRedisTemplate.opsForValue().get(publicKey);
        if (cached != null) {
            // 命中缓存后补查用户私有状态(isLiked 不入公共缓存,避免串状态)
            return applyRealtimeData(decodeDetail(publicKey, cached), userId, id);
        }

        // 2.查作者私有缓存:只有作者访问过自己的草稿才会留下这把 key
        String ownerCached = stringRedisTemplate.opsForValue().get(ownerKey);
        if (ownerCached != null) {
            return applyRealtimeData(decodeDetail(ownerKey, ownerCached), userId, id);
        }

        // 3.未命中:互斥锁防击穿,同一知识只放一个线程去查库重建
        synchronized (publicKey.intern()) {
            // 3.1 双重检查:等锁期间缓存可能已被前一个线程重建好
            cached = stringRedisTemplate.opsForValue().get(publicKey);
            if (cached != null) {
                return applyRealtimeData(decodeDetail(publicKey, cached), userId, id);
            }
            ownerCached = stringRedisTemplate.opsForValue().get(ownerKey);
            if (ownerCached != null) {
                return applyRealtimeData(decodeDetail(ownerKey, ownerCached), userId, id);
            }

            // 3.2 查库
            DetailResult result = loadDetailFromDb(id, userId);

            // 3.3 知识不存在 -> 公共空值标记防穿透(对任何人都不可见,公共标记是安全的)
            if (!result.exists()) {
                stringRedisTemplate.opsForValue().set(publicKey, "", DETAIL_NULL_TTL);
                throw new BusinessException(2001, "知识不存在");
            }

            // 3.4 草稿对非作者不可见 -> 不写缓存直接拒绝
            //     (公共空标记会误伤作者,按用户写私有空标记复杂度高,靠回源兜底即可)
            if (result.vo() == null) {
                throw new BusinessException(2001, "知识不存在");
            }

            // 3.5 写缓存:作者视角进私有 key,公开视角进公共 key;随机 TTL 防雪崩
            long ttlSeconds = DETAIL_CACHE_TTL.toSeconds() + ThreadLocalRandom.current().nextLong(300);
            String json = encodeDetail(result.vo());
            if (result.ownerView()) {
                stringRedisTemplate.opsForValue().set(ownerKey, json, Duration.ofSeconds(ttlSeconds));
            } else {
                stringRedisTemplate.opsForValue().set(publicKey, json, Duration.ofSeconds(ttlSeconds));
            }
            return applyRealtimeData(result.vo(), userId, id);
        }
    }

    /**
     * 回源结果:exists=false 表示知识不存在(可安全写公共空标记);
     * vo=null 表示存在但当前用户不可见(草稿对非作者);ownerView 表示访问者是否作者本人
     */
    private record DetailResult(KnowledgeDetailVO vo, boolean ownerView, boolean exists) {
    }

    /**
     * 从数据库查详情并装配(缓存未命中时的回源逻辑),内置社区可见性准入
     */
    private DetailResult loadDetailFromDb(Long id, Long userId) {
        // 1.查主体(逻辑删除的行查不到)
        Knowledge knowledge = knowledgeMapper.selectById(id);
        if (knowledge == null) {
            return new DetailResult(null, false, false);
        }

        // 2.可见性准入:作者本人可见(含草稿);非作者仅可见已发布,草稿对外等同不存在
        boolean ownerView = knowledge.getUserId().equals(userId);
        if (!ownerView && knowledge.getStatus() != 1) {
            return new DetailResult(null, false, true);
        }

        // 3.装配详情
        KnowledgeDetailVO vo = new KnowledgeDetailVO();
        vo.setId(knowledge.getId());
        vo.setTitle(knowledge.getTitle());
        vo.setContent(knowledge.getContent());
        vo.setCategoryId(knowledge.getCategoryId());
        vo.setStatus(knowledge.getStatus());
        vo.setViewCount(knowledge.getViewCount());
        vo.setLikeCount(knowledge.getLikeCount());
        vo.setFavoriteCount(knowledge.getFavoriteCount());
        vo.setCommentCount(knowledge.getCommentCount());
        vo.setCreateTime(knowledge.getCreateTime());
        vo.setUpdateTime(knowledge.getUpdateTime());

        // 4.作者信息(社区场景展示;authorId 供前端判断是否自己的知识)
        vo.setAuthorId(knowledge.getUserId());
        User author = userMapper.selectById(knowledge.getUserId());
        if (author != null) {
            vo.setAuthorName(author.getNickname() != null ? author.getNickname() : author.getUsername());
        }

        // 5.装配分类名称(分类被删时 category 为 null,展示为空即可)
        if (knowledge.getCategoryId() != null) {
            Category category = categoryMapper.selectById(knowledge.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getName());
            }
        }

        // 6.装配标签列表:先查关联表拿到 tagId 集合,再批量查标签表
        List<KnowledgeTag> relations = knowledgeTagMapper.selectList(
                new LambdaQueryWrapper<KnowledgeTag>().eq(KnowledgeTag::getKnowledgeId, id));
        if (!relations.isEmpty()) {
            List<Long> tagIds = relations.stream().map(KnowledgeTag::getTagId).toList();
            List<Tag> tags = tagMapper.selectByIds(tagIds);
            vo.setTags(tags.stream().map(tag -> {
                KnowledgeDetailVO.TagItem item = new KnowledgeDetailVO.TagItem();
                item.setId(tag.getId());
                item.setName(tag.getName());
                return item;
            }).toList());
        }

        return new DetailResult(vo, ownerView, true);
    }

    /**
     * 补查用户私有状态(isLiked)。
     * 这类因人而异的字段不能进公共缓存,否则 A 的点赞状态会被 B 读到
     */
    private KnowledgeDetailVO applyRealtimeData(KnowledgeDetailVO vo, Long userId, Long knowledgeId) {
        // 1.计数类:主键直查,代价极小;计数高频变化,绝不能信任缓存里的快照值
        Knowledge knowledge = knowledgeMapper.selectById(knowledgeId);
        if (knowledge != null) {
            // 浏览量 = 已落库值 + Redis 未落库增量(最多延迟一个定时周期,本用户本次浏览已计入)
            vo.setViewCount(knowledge.getViewCount() + (int) getViewDelta(knowledgeId));
            vo.setLikeCount(knowledge.getLikeCount());
            vo.setFavoriteCount(knowledge.getFavoriteCount());
            vo.setCommentCount(knowledge.getCommentCount());
        }
        // 2.用户私有状态:因人而异,同样不能进缓存
        Long liked = likeMapper.selectCount(new LambdaQueryWrapper<KnowledgeLike>()
                .eq(KnowledgeLike::getUserId, userId)
                .eq(KnowledgeLike::getKnowledgeId, knowledgeId));
        vo.setIsLiked(liked > 0);
        return vo;
    }

    /**
     * 读取 Redis 中该知识的未落库浏览增量
     */
    private long getViewDelta(Long knowledgeId) {
        String value = stringRedisTemplate.opsForValue().get(VIEW_COUNT_KEY_PREFIX + knowledgeId);
        if (value == null) {
            return 0;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 缓存JSON反序列化为 VO;坏缓存(如结构变更导致)直接删除对应 key并回源
     */
    private KnowledgeDetailVO decodeDetail(String cacheKey, String json) {
        try {
            return objectMapper.readValue(json, KnowledgeDetailVO.class);
        } catch (JsonProcessingException e) {
            log.error("详情缓存反序列化失败, 删除坏缓存, cacheKey={}", cacheKey, e);
            stringRedisTemplate.delete(cacheKey);
            throw new BusinessException(500, "缓存数据异常");
        }
    }

    /**
     * VO 序列化为缓存 JSON
     */
    private String encodeDetail(KnowledgeDetailVO vo) {
        try {
            return objectMapper.writeValueAsString(vo);
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "缓存序列化失败");
        }
    }

    /**
     * 分页查询我的知识
     * @param dto
     * @return
     */
    @Override
    public PageVO<KnowledgeVO> page(KnowledgePageDTO dto) {
        // 1.分页参数兜底:页码最小为 1,每页上限 50,防止恶意大分页拖垮数据库
        long page = dto.getPage() == null || dto.getPage() < 1 ? 1 : dto.getPage();
        long size = dto.getSize() == null || dto.getSize() < 1 ? 10 : Math.min(dto.getSize(), 50);

        // 2.构建查询条件:第一个 eq 无条件生效(user_id 强制隔离);
        //   其余条件用布尔开关重载,仅在传值时拼接进 SQL
        LambdaQueryWrapper<Knowledge> wrapper = new LambdaQueryWrapper<Knowledge>()
                .eq(Knowledge::getUserId, BaseContext.getUserId())
                .eq(dto.getCategoryId() != null, Knowledge::getCategoryId, dto.getCategoryId())
                .eq(dto.getStatus() != null, Knowledge::getStatus, dto.getStatus())
                .like(StringUtils.hasText(dto.getKeyword()), Knowledge::getTitle, dto.getKeyword());

        // 3.排序走白名单映射:orderBy 是列名无法参数化,直接拼 SQL 会有注入风险,
        //   所以只允许映射到预设的 Lambda 引用,其余一律默认按更新时间倒序
        String orderBy = dto.getOrderBy();
        if ("view_count".equals(orderBy)) {
            wrapper.orderByDesc(Knowledge::getViewCount);
        } else if ("create_time".equals(orderBy)) {
            wrapper.orderByDesc(Knowledge::getCreateTime);
        } else {
            wrapper.orderByDesc(Knowledge::getUpdateTime);
        }

        // 4.列表查询排除 content 大字段(决策 5):查询层面就不取大字段,减少传输与内存占用
        wrapper.select(Knowledge.class, field -> !"content".equals(field.getProperty()));

        // 5.分页查询:分页插件自动在 SQL 末尾拼 LIMIT,并自动执行 COUNT 统计总数
        Page<Knowledge> result = knowledgeMapper.selectPage(new Page<>(page, size), wrapper);

        // 6.批量装配分类名:先收集本页所有 categoryId(去重去 null),一次 in 查询,避免循环查库(N+1)
        List<Long> categoryIds = result.getRecords().stream()
                .map(Knowledge::getCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> categoryNameMap = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            categoryMapper.selectByIds(categoryIds)
                    .forEach(c -> categoryNameMap.put(c.getId(), c.getName()));
        }

        // 7.实体转 VO,并从 Map 里填充分类名
        List<KnowledgeVO> vos = result.getRecords().stream().map(k -> {
            KnowledgeVO vo = new KnowledgeVO();
            vo.setId(k.getId());
            vo.setTitle(k.getTitle());
            vo.setSummary(k.getSummary());
            vo.setCategoryId(k.getCategoryId());
            vo.setCategoryName(categoryNameMap.get(k.getCategoryId()));
            vo.setStatus(k.getStatus());
            vo.setViewCount(k.getViewCount());
            vo.setCreateTime(k.getCreateTime());
            vo.setUpdateTime(k.getUpdateTime());
            return vo;
        }).toList();

        // 8.包装成统一分页结构返回
        return PageVO.of(result.getTotal(), result.getCurrent(), result.getSize(), vos);
    }

    /**
     * 校验知识存在且属于当前用户"不存在"与"非本人"统一报 2001,防止探测他人知识 id
     */
    private Knowledge getOwnedKnowledge(Long id, Long userId) {
        // 按 id 查询(逻辑删除的行查不到)
        Knowledge knowledge = knowledgeMapper.selectById(id);
        // 两种失败场景返回完全相同的错误,不给攻击者探测空间
        if (knowledge == null || !knowledge.getUserId().equals(userId)) {
            throw new BusinessException(2001, "知识不存在");
        }
        return knowledge;
    }

    /**
     * 标签绑定:名字数组 -> upsert 标签 -> 建立知识-标签关联
     */
    private void bindTags(Long knowledgeId, Long userId, List<String> tagNames) {
        // 空列表直接返回,不做事
        if (tagNames == null || tagNames.isEmpty()) {
            return;
        }
        // 逐个处理标签名
        for (String name : tagNames) {
            String trimmed = name == null ? "" : name.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            // 先查当前用户是否已有同名标签(upsert:存在复用)
            Tag tag = tagMapper.selectOne(new LambdaQueryWrapper<Tag>()
                    .eq(Tag::getUserId, userId)
                    .eq(Tag::getName, trimmed));
            // 不存在则新建
            if (tag == null) {
                tag = new Tag();
                tag.setUserId(userId);
                tag.setName(trimmed);
                tagMapper.insert(tag);
            }
            // 建立知识-标签关联;同次提交传重复标签时由唯一索引兜底,冲突忽略
            try {
                KnowledgeTag kt = new KnowledgeTag();
                kt.setKnowledgeId(knowledgeId);
                kt.setTagId(tag.getId());
                knowledgeTagMapper.insert(kt);
            } catch (DuplicateKeyException e) {
                log.info("标签关联已存在,跳过, knowledgeId={}, tagId={}", knowledgeId, tag.getId());
            }
        }
    }

    private KnowledgeBaseVO toBaseVO(Knowledge knowledge) {
        KnowledgeBaseVO vo = new KnowledgeBaseVO();
        vo.setId(knowledge.getId());
        vo.setTitle(knowledge.getTitle());
        vo.setStatus(knowledge.getStatus());
        return vo;
    }
}
