package com.knowledge.controller;

import com.knowledge.common.result.Result;
import com.knowledge.dto.RefreshTokenDTO;
import com.knowledge.dto.UserLoginDTO;
import com.knowledge.dto.UserRegisterDTO;
import com.knowledge.dto.UserUpdateDTO;
import com.knowledge.service.UserService;
import com.knowledge.vo.LoginVO;
import com.knowledge.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 注册
     * @param dto
     * @return
     */
    @PostMapping("/register")
    public Result<UserVO> register(@RequestBody @Valid UserRegisterDTO dto) {
        return Result.ok(userService.register(dto));
    }

    /**
     * 登录
     * @param dto
     * @return
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid UserLoginDTO dto) {
        return Result.ok(userService.login(dto));
    }

    /**
     * 刷新 token
     * @param dto
     * @return
     */
    @PostMapping("/refresh")
    public Result<LoginVO> refresh(@RequestBody @Valid RefreshTokenDTO dto) {
        return Result.ok(userService.refresh(dto));
    }

    /**
     * 获取当前用户信息
     * @return
     */
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.ok(userService.getMe());
    }

    /**
     * 更新当前用户信息
     * @param dto
     * @return
     */
    @PutMapping("/me")
    public Result<UserVO> updateMe(@RequestBody @Valid UserUpdateDTO dto) {
        return Result.ok(userService.updateMe(dto));
    }

    /**
     * 头像上传(OSS),返回可访问 URL
     */
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return Result.ok(userService.uploadAvatar(file));
    }
}
