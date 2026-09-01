package com.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9]{3,32}$",
            message = "用户名必须为3~32位字母或数字")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64,
            message = "密码长度必须为6~64位")
    private String password;

    @Size(max = 32,
            message = "昵称长度不能超过32位")
    private String nickname;
}
