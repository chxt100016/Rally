package com.rally.domain.tournament.enums;

/**
 * 赛事未报名场景下的准入限制原因（可叠加）。
 * <p>
 * 仅在 actionState 为 NOT_REGISTERED 时计算；列表为空表示当前用户满足报名所需的
 * 登录、档案和 NTRP 等级条件，具体文案由前端拼装。
 */
public enum TournamentJoinRestrictionEnum {
    /** 尚未登录 */
    NOT_LOGGED_IN,
    /** NTRP 等级与赛事要求不符 */
    LEVEL_NOT_MATCH,
    /** 个人信息未完善（头像、昵称为默认值） */
    PROFILE_INCOMPLETE,
    /** 球员档案未完善 */
    ONBOARDING_INCOMPLETE,
    /** 个人信息和球员档案均未完善 */
    REGISTRATION_INCOMPLETE,
    ;
}
