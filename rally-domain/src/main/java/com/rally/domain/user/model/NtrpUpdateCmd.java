package com.rally.domain.user.model;

import com.rally.domain.user.validation.NtrpStep;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 自评修改入参
 */
@Data
public class NtrpUpdateCmd {
    /** 目标自评 1.5~7.0 步长 0.5 */
    @NotNull(message = "自评分值不能为空")
    @DecimalMin(value = "1.5", message = "自评分值必须在 1.5-7.0 之间")
    @DecimalMax(value = "7.0", message = "自评分值必须在 1.5-7.0 之间")
    @NtrpStep
    private BigDecimal ntrpScore;
}
