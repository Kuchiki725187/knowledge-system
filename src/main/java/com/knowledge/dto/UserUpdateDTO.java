package com.knowledge.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateDTO {

    @Size(max = 32, message = "昵称长度不能超过32位")
    private String nickname;

    @Size(max = 255, message = "头像地址过长")
    private String avatar;
}
