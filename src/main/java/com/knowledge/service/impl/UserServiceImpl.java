package com.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.common.context.BaseContext;
import com.knowledge.common.exception.BusinessException;
import com.knowledge.common.util.JwtUtil;
import com.knowledge.dto.UserLoginDTO;
import com.knowledge.dto.UserRegisterDTO;
import com.knowledge.dto.UserUpdateDTO;
import com.knowledge.entity.User;
import com.knowledge.mapper.UserMapper;
import com.knowledge.service.UserService;
import com.knowledge.vo.LoginVO;
import com.knowledge.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

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

    @Override
    public UserVO updateMe(UserUpdateDTO dto) {
        User user = new User();
        user.setId(BaseContext.getUserId());
        user.setNickname(dto.getNickname());
        user.setAvatar(dto.getAvatar());
        userMapper.updateById(user);
        return getMe();
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
