package com.knowledge.controller;

import com.knowledge.common.result.Result;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<UserVO> register(@RequestBody @Valid UserRegisterDTO dto) {
        return Result.ok(userService.register(dto));
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid UserLoginDTO dto) {
        return Result.ok(userService.login(dto));
    }

    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.ok(userService.getMe());
    }

    @PutMapping("/me")
    public Result<UserVO> updateMe(@RequestBody @Valid UserUpdateDTO dto) {
        return Result.ok(userService.updateMe(dto));
    }
}
