package com.rally.domain.court.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CourtSourceEnum {
    USER_PUBLISH("用户发布"),
    SYSTEM("系统录入");

    public final String label;
}
