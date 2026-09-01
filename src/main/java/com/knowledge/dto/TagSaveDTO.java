package com.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TagSaveDTO {

    @NotBlank(message = "标签名称不能为空")
    @Size(max = 32, message = "标签名称不能超过32字")
    private String name;
}
