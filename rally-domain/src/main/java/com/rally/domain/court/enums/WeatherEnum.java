package com.rally.domain.court.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 天气（用于卡片背景样式选择，天气模块未上线时传 null 降级晴天）
 */
@AllArgsConstructor
@Getter
public enum WeatherEnum {
    CLEAR("晴"),
    RAIN("雨");
    public final String label;
}
