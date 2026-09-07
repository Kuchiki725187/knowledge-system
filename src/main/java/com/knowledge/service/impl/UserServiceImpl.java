package com.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.common.context.BaseContext;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.common.oss.OssStorageService;
import com.knowledge.common.util.JwtUtil;
import com.knowledge.dto.RefreshTokenDTO;
import com.knowledge.dto.UserLoginDTO;
import com.knowledge.dto.UserRegisterDTO;
import com.knowledge.dto.UserUpdateDTO;
import com.knowledge.entity.User;
import com.knowledge.mapper.UserMapper;
import com.knowledge.service.UserService;
import com.knowledge.vo.LoginVO;
import com.knowledge.vo.UserVO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OssStorageService ossStorageService;

    @Override
    public UserVO register(UserRegisterDTO dto) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BusinessException(1001, "用户名已存在");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(1001, "用户名已存在");
        }
        return toUserVO(user);
    }

    @Override
    public LoginVO login(UserLoginDTO dto) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(1002, "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(1003, "账号已禁用");
        }

        return buildLoginVO(user);
    }

    /**
     * 刷新 token:校验 refreshToken -> 重新签发新一轮双 token(轮换机制,旧 refreshToken 作废)
     */
    @Override
    public LoginVO refresh(RefreshTokenDTO dto) {
        // 1.解析 refreshToken;过期/被篡改统一按"登录已过期"处理(不给攻击者区分线索)
        Claims claims;
        try {
            claims = jwtUtil.parse(dto.getRefreshToken());
        } catch (JwtException e) {
            // JwtException 是所有解析失败的基类(含过期),统一按登录过期处理
            throw new BusinessException(401, "登录已过期，请重新登录");
        }

        // 2.校验 token 类型必须是 refresh,防止拿 access token 冒充
        if (!"refresh".equals(claims.get("type"))) {
            throw new BusinessException(401, "登录已过期，请重新登录");
        }

        // 3.查用户并校验状态(账号被禁用后,refresh 也不放行)
        User user = userMapper.selectById(Long.parseLong(claims.getSubject()));
        if (user == null || (user.getStatus() != null && user.getStatus() == 0)) {
            throw new BusinessException(401, "登录已过期，请重新登录");
        }

        // 4.签发新双 token
        return buildLoginVO(user);
    }

    /**
     * 组装登录/刷新响应:双 token + 用户信息(两处共用,避免重复)
     */
    private LoginVO buildLoginVO(User user) {
        LoginVO vo = new LoginVO();
        vo.setAccessToken(jwtUtil.createAccessToken(user.getId()));
        vo.setRefreshToken(jwtUtil.createRefreshToken(user.getId()));
        vo.setUser(toUserVO(user));
        return vo;
    }

    @Override
    public UserVO getMe() {
        User user = userMapper.selectById(BaseContext.getUserId());
        if (user == null) {
            throw new BusinessException(1004, "用户不存在");
        }
        return toUserVO(user);
    }

    /**
     * 修改个人资料:用户名/密码/昵称/头像,传了才改(全部可空)
     * 改密码必须校验旧密码,防止 token 窃取者直接改密劫持账号
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateMe(UserUpdateDTO dto) {
        Long userId = BaseContext.getUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(1004, "用户不存在");
        }

        // 1.改用户名:查重需排除自己;数据库唯一索引兜底并发冲突
        if (StringUtils.hasText(dto.getUsername()) && !dto.getUsername().equals(user.getUsername())) {
            Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, dto.getUsername())
                    .ne(User::getId, userId));
            if (count > 0) {
                throw new BusinessException(1001, "用户名已存在");
            }
            user.setUsername(dto.getUsername());
        }

        // 2.改密码:新密码传了才走这里,且必须校验旧密码
        if (StringUtils.hasText(dto.getPassword())) {
            if (!StringUtils.hasText(dto.getOldPassword())
                    || !passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
                throw new BusinessException(1005, "原密码错误");
            }
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        // 3.昵称/头像:传了才更新(空串传过来也视为用户主动清空昵称/头像)
        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }

        userMapper.updateById(user);
        return getMe();
    }

    /**
     * 上传头像到 OSS,返回可访问 URL(落库由 updateMe 的 avatar 字段完成,职责解耦)
     */
    @Override
    public String uploadAvatar(MultipartFile file) {
        Long userId = BaseContext.getUserId();
        return ossStorageService.uploadImage(file, "avatar", userId);
    }

    private UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        return vo;
    }
}
