package com.knowledge.vo;

import lombok.Data;

@Data
public class LoginVO {

    private String accessToken;

    private String refreshToken;

    private UserVO user;
}
