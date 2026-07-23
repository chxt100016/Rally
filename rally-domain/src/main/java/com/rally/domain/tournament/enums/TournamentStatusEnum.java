package com.rally.domain.tournament.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 赛事状态枚举（管理状态）
 */
@AllArgsConstructor
@Getter
public enum TournamentStatusEnum {
    DRAFT("草稿"),
    ACTIVE("已激活"),
    ABANDONED("已废弃");

    public final String label;
}
