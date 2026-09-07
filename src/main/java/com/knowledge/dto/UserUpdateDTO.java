package com.knowledge.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改个人资料,所有字段可空 = "传了才改"(格式注解对 null 放行)
 */
@Data
public class UserUpdateDTO {

    @Pattern(regexp = "^[a-zA-Z0-9]{3,32}$",
            message = "用户名必须为3~32位字母或数字")
    private String username;

    @Size(min = 6, max = 64, message = "密码长度必须为6~64位")
    private String password;

    @Size(max = 32, message = "昵称长度不能超过32位")
    private String nickname;

    @Size(max = 255, message = "头像地址过长")
    private String avatar;

    /**
     * 原密码:修改密码时必填,用于验证身份(防止 token 窃取者直接改密)
     */
    private String oldPassword;

    /**
     * 用户名里的空格视为不存在(与注册/登录一致)
     */
    public void setUsername(String username) {
        this.username = username == null ? null : username.replaceAll("\\s", "");
    }
}
