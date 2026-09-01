package com.knowledge.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RelationSaveDTO {

    @NotNull(message = "对方知识id不能为空")
    private Long targetId;

    private Integer relationType;
}
