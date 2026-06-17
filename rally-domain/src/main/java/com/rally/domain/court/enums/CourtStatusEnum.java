package com.rally.domain.court.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CourtStatusEnum {
    COLLECTED("已收录，待审核"),
    ACTIVE("可用"),
    DISABLED("已停用");

    public final String label;
}
