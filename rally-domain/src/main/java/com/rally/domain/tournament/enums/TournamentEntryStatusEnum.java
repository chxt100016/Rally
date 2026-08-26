package com.rally.domain.tournament.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 报名状态枚举
 */
@AllArgsConstructor
@Getter
public enum TournamentEntryStatusEnum {
    WAITING("等待匹配"),
    FROZEN("冻结"),
    IN_MATCH("比赛中"),
    PAYING("待支付"),
    CHAMPION("冠军"),
    ELIMINATED("已淘汰"),
    WITHDRAWN("已退赛");

    public final String label;
}
