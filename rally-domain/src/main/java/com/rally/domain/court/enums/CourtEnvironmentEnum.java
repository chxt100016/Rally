package com.rally.domain.court.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CourtEnvironmentEnum {
    INDOOR("室内"),
    OUTDOOR("室外");
    public final String label;
}
