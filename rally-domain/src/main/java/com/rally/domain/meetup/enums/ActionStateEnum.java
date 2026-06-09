package com.rally.domain.meetup.enums;

/**
 * 详情/列表吸底按钮状态枚举
 * <p>
 * 访客视角：
 * - JOIN_DIRECT: 招募中 + 未报名 + join_mode=DIRECT → 「立即报名」
 * - APPLY_APPROVAL: 招募中 + 未报名 + join_mode=APPROVAL → 「申请加入」
 * - PENDING_REVIEW: 我的报名 status=pending → 「待审核」+「撤销」
 * - JOINED: 我的报名 status=JOINED → 「退出」+「群聊」
 * - FULL: 已满员且我未加入 → 「已满员」(灰)
 * <p>
 * 创建人视角：
 * - OWNER_EDITABLE: 未到编辑锁定时间且非终态 → 「编辑信息」可用
 * - OWNER_EDIT_LOCKED: 已过编辑锁定时间，未到终态 → 「编辑信息」灰
 * <p>
 * 通用终态：
 * - FINISHED: 真实状态 FINISHED → 全部置灰
 * - CLOSED: 真实状态 CLOSED → 全部置灰
 */
public enum ActionStateEnum {
    // 访客视角
    JOIN_DIRECT,
    APPLY_APPROVAL,
    PENDING_REVIEW,
    JOINED,
    FULL,
    // 创建人视角
    OWNER_EDITABLE,
    OWNER_EDIT_LOCKED,
    // 通用终态
    FINISHED,
    CLOSED
}
