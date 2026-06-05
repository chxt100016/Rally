package com.rally.domain.user.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Onboarding 提交入参
 */
@Data
public class OnboardingCmd {
    /** 性别：MALE/FEMALE/undisclosed */
    private String gender;
    /** 生日（选填） */
    private LocalDate birthday;
    /** NTRP 自评 1.5~7.0 步长 0.5 */
    @NotNull(message = "NTRP 自评分不能为空")
    @NotBlank(message = "NTRP 自评分不能为空")
    private BigDecimal ntrpScore;
    /** 城市编码 */
    @NotBlank(message = "城市编码不能为空")
    private String cityCode;
    /** 视频 key 列表（≥1） */
    @NotEmpty(message = "视频列表不能为空")
    private List<String> videoKeys;
}
