package com.knowledge.service;

import com.knowledge.dto.RefreshTokenDTO;
import com.knowledge.dto.UserLoginDTO;
import com.knowledge.dto.UserRegisterDTO;
import com.knowledge.dto.UserUpdateDTO;
import com.knowledge.vo.LoginVO;
import com.knowledge.vo.UserVO;

public interface UserService {

    UserVO register(UserRegisterDTO dto);

    LoginVO login(UserLoginDTO dto);

    LoginVO refresh(RefreshTokenDTO dto);

    UserVO getMe();

    UserVO updateMe(UserUpdateDTO dto);
}
