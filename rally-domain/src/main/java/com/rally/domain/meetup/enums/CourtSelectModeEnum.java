package com.rally.domain.meetup.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 球场选择模式
 */
@Getter
@AllArgsConstructor
public enum CourtSelectModeEnum {

    /** 文本搜索选择 */
    TEXT("文本搜索"),

    /** 地图选择 */
    MAP("地图选择"),

    /** 自由输入 */
    FREE("自由输入");

    private final String label;
}
