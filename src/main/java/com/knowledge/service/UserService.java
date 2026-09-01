package com.knowledge.service;

import com.knowledge.dto.UserLoginDTO;
import com.knowledge.dto.UserRegisterDTO;
import com.knowledge.dto.UserUpdateDTO;
import com.knowledge.vo.LoginVO;
import com.knowledge.vo.UserVO;

public interface UserService {

    UserVO register(UserRegisterDTO userRegisterDTO);

    LoginVO login(UserLoginDTO dto);

    UserVO getMe();

    UserVO updateMe(UserUpdateDTO dto);
}
