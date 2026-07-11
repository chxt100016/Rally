package com.rally.domain.user.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserExtCmd {
    @NotNull(message = "扩展字段类型不能为空")
    private String extKey;

    @NotBlank(message = "扩展字段值不能为空")
    private String extValue;
}
