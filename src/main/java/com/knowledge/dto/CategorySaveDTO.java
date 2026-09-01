package com.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategorySaveDTO {

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 32, message = "分类名称不能超过32字")
    private String name;

    private Integer sort;
}
