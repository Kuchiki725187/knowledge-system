package com.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class KnowledgeSaveDTO {

    @NotBlank(message = "标题不能为空")
    @Size(max = 128, message = "标题不能超过128字")
    private String title;

    @NotBlank(message = "正文不能为空")
    private String content;

    private Long categoryId;

    private List<String> tags;

    private Integer status;
}
