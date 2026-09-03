package com.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserLoginDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 用户名里的空格视为不存在
     */
    public void setUsername(String username) {
        this.username = username == null ? null : username.replaceAll("\\s", "");
    }
}
