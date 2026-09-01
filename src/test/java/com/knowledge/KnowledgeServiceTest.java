package com.knowledge;

import com.knowledge.common.context.BaseContext;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.dto.KnowledgeSaveDTO;
import com.knowledge.entity.Knowledge;
import com.knowledge.entity.User;
import com.knowledge.mapper.KnowledgeMapper;
import com.knowledge.mapper.UserMapper;
import com.knowledge.service.KnowledgeService;
import com.knowledge.vo.KnowledgeBaseVO;
import com.knowledge.vo.KnowledgeDetailVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 知识模块核心 Service 测试:数据隔离与可见性是本模块的命根子,逐条验证
 */
@SpringBootTest
@Transactional
class KnowledgeServiceTest {

    @Autowired
    private KnowledgeService knowledgeService;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private KnowledgeMapper knowledgeMapper;

    /**
     * 造一个测试用户,返回其 id
     */
    private Long createUser(String prefix) {
        User user = new User();
        user.setUsername(prefix + System.currentTimeMillis());
        user.setPassword("test-password");
        userMapper.insert(user);
        return user.getId();
    }

    private KnowledgeSaveDTO buildDTO(String title, int status) {
        KnowledgeSaveDTO dto = new KnowledgeSaveDTO();
        dto.setTitle(title);
        dto.setContent("# 测试正文");
        dto.setStatus(status);
        return dto;
    }

    @Test
    void create_shouldInjectCurrentUserAsOwner() {
        // 创建时归属强制取 ThreadLocal 里的登录用户
        Long userId = createUser("owner_");
        BaseContext.setUserId(userId);
        try {
            KnowledgeBaseVO vo = knowledgeService.create(buildDTO("归属测试", 1));
            assertNotNull(vo.getId());
            // 查库确认 user_id 被强制设置为当前用户
            Knowledge saved = knowledgeMapper.selectById(vo.getId());
            assertEquals(userId, saved.getUserId());
        } finally {
            BaseContext.remove();
        }
    }

    @Test
    void detail_otherUserCannotSeeDraft() {
        // 他人访问草稿:与"知识不存在"同响应(防探测)
        Long owner = createUser("draft_owner_");
        Long other = createUser("draft_other_");
        BaseContext.setUserId(owner);
        Long knowledgeId;
        try {
            knowledgeId = knowledgeService.create(buildDTO("草稿", 0)).getId();
        } finally {
            BaseContext.remove();
        }

        BaseContext.setUserId(other);
        try {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> knowledgeService.getDetail(knowledgeId));
            assertEquals(2001, ex.getCode());
        } finally {
            BaseContext.remove();
        }
    }

    @Test
    void detail_otherUserCanSeePublished() {
        // 社区模型:已发布知识对所有人可见
        Long owner = createUser("pub_owner_");
        Long other = createUser("pub_other_");
        BaseContext.setUserId(owner);
        Long knowledgeId;
        try {
            knowledgeId = knowledgeService.create(buildDTO("已发布", 1)).getId();
        } finally {
            BaseContext.remove();
        }

        BaseContext.setUserId(other);
        try {
            KnowledgeDetailVO vo = knowledgeService.getDetail(knowledgeId);
            assertNotNull(vo);
            assertEquals("已发布", vo.getTitle());
        } finally {
            BaseContext.remove();
        }
    }

    @Test
    void delete_otherUserKnowledge_shouldFail() {
        // 越权删除:他人知识统一报 2001
        Long owner = createUser("del_owner_");
        Long other = createUser("del_other_");
        BaseContext.setUserId(owner);
        Long knowledgeId;
        try {
            knowledgeId = knowledgeService.create(buildDTO("要删的知识", 1)).getId();
        } finally {
            BaseContext.remove();
        }

        BaseContext.setUserId(other);
        try {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> knowledgeService.delete(knowledgeId));
            assertEquals(2001, ex.getCode());
        } finally {
            BaseContext.remove();
        }
    }
}
