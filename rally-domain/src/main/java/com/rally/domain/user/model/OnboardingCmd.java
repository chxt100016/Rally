package com.rally.domain.user.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Onboarding 提交入参
 */
@Data
public class OnboardingCmd {
    /** 性别：male/female/undisclosed */
    private String gender;
    /** 生日（选填） */
    private LocalDate birthday;
    /** NTRP 自评 1.5~7.0 步长 0.5 */
    private BigDecimal ntrpScore;
    /** 城市编码 */
    private String cityCode;
    /** 视频 key 列表（≥1） */
    private List<String> videoKeys;
}
