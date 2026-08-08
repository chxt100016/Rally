package com.rally.domain.tournament.enums;

/**
 * 赛事报名和解冻场景下的用户限制原因（可叠加）。
 * <p>
 * NOT_REGISTERED 返回报名所需的登录、档案、NTRP 和手机号限制；
 * FROZEN 仅返回手机号限制，具体文案由前端拼装。
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
    /** 尚未绑定手机号 */
    PHONE_MISSING,
    ;
}
