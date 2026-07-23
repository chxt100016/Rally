package com.rally.domain.tournament.enums;

/**
 * 落地页待办状态枚举，驱动前端"当前待办卡片"渲染
 */
public enum TournamentActionStateEnum {
    NOT_REGISTERED,
    AWAIT_PAYMENT,
    AWAIT_COURT_BOOKER_SELECT,
    AWAIT_BOOKING,
    AWAIT_BOOKING_OPPONENT,
    AWAIT_SCHEDULE_CONFIRM,
    AWAIT_RESULT_SUBMIT,
    AWAIT_RESULT_CONFIRM,
    WAITING_MATCH,
    ELIMINATED,
    WITHDRAWN,
    QUALIFIED_MAIN_DRAW
}
