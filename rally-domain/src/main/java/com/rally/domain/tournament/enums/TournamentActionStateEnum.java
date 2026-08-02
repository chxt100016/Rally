package com.rally.domain.tournament.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 落地页待办状态及其卡片文案。
 * 动态内容使用 {@code %s} 占位，由应用层注入对手/重订人及理由。
 */
@Getter
@AllArgsConstructor
public enum TournamentActionStateEnum {
    NOT_LOGGED_IN("未登录", "登录后查看", "登录后可报名参赛或查看个人赛事信息。"),
    NOT_REGISTERED("未报名", "立即报名参赛", "完成报名后进入资格赛匹配池，比赛开始后自动匹配对手。"),
    NOT_REGISTERED_CLOSED("报名关闭", "暂不可报名", "当前赛事暂不可报名。"),
    AWAIT_QUALIFIER_START("待匹配", "报名成功，等待赛事开始", "你已完成报名，赛事匹配即将开始。点击右上角「加入群聊」，及时关注赛事安排与开赛通知。"),
    AWAIT_PAYMENT("待支付", "支付报名费，锁定正赛席位", "你已晋级正赛，请在截止前完成支付以保留席位。"),
    AWAIT_COURT_BOOKER_SELECT("协商赛约", "本场比赛待确定订场人", "对阵%s。由一方认领订场，确定球场与时间后开赛。"),
    AWAIT_BOOKING("协商赛约", "你已认领订场，请提交场地信息", "对阵%s。确定好球场和时间后提交，对手确认即可开赛。"),
    AWAIT_BOOKING_REBOOK("协商赛约", "请重新提交场地信息", "%s因“%s”打回了上次赛约，请点击下面卡片进入约球修改赛约。"),
    AWAIT_BOOKING_OPPONENT("协商赛约", "对方订场中", "对阵%s，等待对方提交球场与时间，请耐心等候。"),
    AWAIT_SCHEDULE_CONFIRM("协商赛约", "对方已提交赛约，请确认", "对阵%s。请核对球场与时间后接受，或打回重订。"),
    AWAIT_OPPONENT_SCHEDULE_CONFIRM("协商赛约", "你已确认赛约，等待对方确认", "对阵%s，对方确认后即可开赛，请耐心等候。"),
    AWAIT_RESULT_SUBMIT("录入结果", "比赛已结束，请提交结果", "对阵%s。选择本场胜者提交，对手确认后晋级生效。"),
    AWAIT_RESULT_CONFIRM("录入结果", "对方已提交结果，请确认", "对阵%s，%s获胜。确认无误即晋级生效，如有异议可拒绝。"),
    AWAIT_OPPONENT_RESULT_CONFIRM("录入结果", "我已确认比赛结果，等待其他人确认", "对阵%s，%s获胜。对方确认后即可晋级生效，请耐心等候。"),
    WAITING_MATCH("匹配中", "正在为你匹配对手", "每日凌晨自动匹配，匹配成功后这里会出现你的待办。"),
    IN_OFFLINE_STAGE("线下赛", "恭喜晋级线下赛阶段", "你已成功晋级线下赛，官方将统一安排场地与赛程，请耐心等待通知。"),
    ELIMINATED("已淘汰", "本届赛事旅程结束", "感谢参与，期待下一次在球场相见。"),
    WITHDRAWN("已退出", "你已退出本赛事", "如需再次参赛，请关注后续赛事报名。"),
    END("已结束", "赛事已结束", "可查看下方签表与赛程进度。"),

    ;

    private final String label;
    private final String title;
    private final String subtitle;
}
