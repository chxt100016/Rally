package com.rally.domain.notify.enums;

/**
 * 通知场景只表达业务语义。
 * 渠道、模板和内容格式由各通知渠道适配器自行映射。
 */
public enum NoticeScene {

    /** 报名成功通知（免审批活动加入即发；需审批活动审批通过后发）—— 收件人：参与人 */
    JOIN_SUCCESS,

    /** 活动申请审批提醒 —— 收件人：创建人（仅需审批活动） */
    PENDING_APPROVAL,

    /** 活动取消通知 —— 收件人：全体已加入参与人 */
    MEETUP_CANCEL,

    /** 组团成功提醒 —— 收件人：全体已加入参与人 */
    TEAM_SUCCESS,

    /** 成员退出通知 —— 收件人：创建人/全体已加入参与人 */
    MEMBER_QUIT,

    /** 赛事匹配成功通知 —— 收件人：本场所有参赛人 */
    TOURNAMENT_MATCHED,

    /** 赛事订场信息通知 —— 收件人：除订场人外的本场参赛人 */
    TOURNAMENT_BOOKING_SUBMITTED,

    /** 赛事拒绝通知 —— 收件人：除拒绝人外的本场参赛人 */
    TOURNAMENT_REJECTED
}
