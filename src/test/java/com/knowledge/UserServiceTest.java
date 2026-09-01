package com.knowledge;

import com.knowledge.common.exception.BusinessException;
import com.knowledge.dto.RefreshTokenDTO;
import com.knowledge.dto.UserLoginDTO;
import com.knowledge.dto.UserRegisterDTO;
import com.knowledge.service.UserService;
import com.knowledge.vo.LoginVO;
import com.knowledge.vo.UserVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 用户模块核心 Service 测试。
 * @SpringBootTest: 加载完整上下文(连真实 MySQL/Redis)
 * @Transactional:  每个测试事务自动回滚,测试数据不残留
 */
@SpringBootTest
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    private UserRegisterDTO buildRegisterDTO(String username) {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername(username);
        dto.setPassword("123456");
        dto.setNickname("测试用户");
        return dto;
    }

    @Test
    void register_shouldSucceed() {
        // 注册成功返回带 id 的用户信息
        UserRegisterDTO dto = buildRegisterDTO("tester_" + System.currentTimeMillis());
        UserVO vo = userService.register(dto);
        assertNotNull(vo.getId());
        assertEquals(dto.getUsername(), vo.getUsername());
    }

    @Test
    void register_duplicateUsername_shouldFail() {
        // 同名注册第二次报 1001
        UserRegisterDTO dto = buildRegisterDTO("dup_" + System.currentTimeMillis());
        userService.register(dto);
        BusinessException ex = assertThrows(BusinessException.class, () -> userService.register(dto));
        assertEquals(1001, ex.getCode());
    }

    @Test
    void login_wrongPassword_shouldFail() {
        // 密码错误统一报 1002(与用户不存在同提示,防账号枚举)
        UserRegisterDTO dto = buildRegisterDTO("login_" + System.currentTimeMillis());
        userService.register(dto);

        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setUsername(dto.getUsername());
        loginDTO.setPassword("wrong-password");
        BusinessException ex = assertThrows(BusinessException.class, () -> userService.login(loginDTO));
        assertEquals(1002, ex.getCode());
    }

    @Test
    void login_shouldReturnBothTokens() {
        UserRegisterDTO dto = buildRegisterDTO("login2_" + System.currentTimeMillis());
        userService.register(dto);

        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setUsername(dto.getUsername());
        loginDTO.setPassword("123456");
        LoginVO vo = userService.login(loginDTO);
        assertNotNull(vo.getAccessToken());
        assertNotNull(vo.getRefreshToken());
        assertNotNull(vo.getUser());
    }

    @Test
    void refresh_invalidToken_shouldFail() {
        // 非法 token 统一按登录过期处理
        RefreshTokenDTO dto = new RefreshTokenDTO();
        dto.setRefreshToken("not-a-valid-token");
        BusinessException ex = assertThrows(BusinessException.class, () -> userService.refresh(dto));
        assertEquals(401, ex.getCode());
    }
}
