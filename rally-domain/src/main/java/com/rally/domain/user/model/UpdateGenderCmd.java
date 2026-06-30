package com.rally.domain.user.model;

import com.rally.domain.user.enums.GenderEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改性别入参
 */
@Data
public class UpdateGenderCmd {
    @NotNull(message = "性别不能为空")
    private GenderEnum gender;
}
