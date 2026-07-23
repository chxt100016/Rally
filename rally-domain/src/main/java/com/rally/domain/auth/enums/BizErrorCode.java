package com.rally.domain.auth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务错误码枚举
 * 前缀 1xxxx 表示认证相关错误
 * 前缀 2xxxx 表示业务相关错误
 */
@Getter
@AllArgsConstructor
public enum BizErrorCode {

    // ========== 认证相关 10001-19999 ==========
    UNAUTHORIZED(10001, "未登录，请先登录"),
    TOKEN_EXPIRED(10002, "登录已过期，请重新登录"),
    TOKEN_INVALID(10003, "登录凭证无效，请重新登录"),
    ACCESS_DENIED(10004, "无权限访问"),

    // ========== 通用业务 20001-29999 ==========
    PARAM_ERROR(20001, "参数错误"),
    DATA_NOT_FOUND(20002, "数据不存在"),
    DATA_DUPLICATE(20003, "数据已存在"),
    OPERATION_FAILED(20004, "操作失败"),

    // ========== 微信相关 30001-39999 ==========
    WECHAT_LOGIN_FAILED(30001, "微信登录失败"),
    WECHAT_AUTH_FAILED(30002, "微信授权失败"),

    // ========== 用户域 40001-40999 ==========
    ONBOARDING_INCOMPLETE(40001, "请先完善网球档案"),
    NTRP_COOLDOWN(40002, "自评修改冷却中"),
    VIDEO_LIMIT_EXCEEDED(40003, "视频数量已达上限"),
    VIDEO_NOT_OWNED(40004, "无权操作该视频"),
    NTRP_INVALID_VALUE(40005, "自评分值非法"),
    UNDER_REVIEW_LOCKED(40006, "核查期内不可进行该操作"),
    PROFILE_NOT_FOUND(40007, "档案不存在"),
    VIDEO_CALLBACK_INVALID(40008, "视频回调校验失败"),
    VIDEO_AT_LEAST_ONE(40009, "至少需要保留一个视频"),
    USER_INCOMPLETE(40010, "请先完善用户信息, 设置头像和昵称"),
    FOLLOW_SELF_NOT_ALLOWED(40010, "不能关注自己"),
    REGISTRATION_INCOMPLETE(40011, "请先完善个人信息和网球档案"),
    USER_NOT_EXIST(40012, "用户不存在"),
    USER_EXT_NOT_FOUND(40013, "用户扩展信息不存在"),
    USER_EXT_KEY_INVALID(40014, "扩展字段类型无效"),
    WECHAT_PAYMENT_CODE_EMPTY(40015, "微信付款码不能为空"),

    // ========== 约球域 41001-41999 ==========
    MEETUP_NOT_FOUND(41001, "约球不存在"),
    MEETUP_FULL(41002, "约球已满员"),
    MEETUP_EXPIRED(41003, "约球已开始或已结束，无法操作"),
    MEETUP_CLOSED(41004, "约球已关闭"),
    MEETUP_STATUS_ILLEGAL(41005, "约球状态不允许该操作"),
    MEETUP_ONGOING(41021, "约球进行中，无法报名"),
    CITY_CHANGE_FORBIDDEN(41019, "已发布的约球不可修改城市"),
    LOCATION_TIME_CHANGE_FORBIDDEN(41020, "已有参与者报名，不可修改时间、地点、持续时长"),
    JOIN_FORBIDDEN(41006, "当前不可报名"),
    ALREADY_JOINED(41007, "你已报名该约球"),
    NOT_JOINED(41008, "你未报名该约球"),
    TIME_CONFLICT(41009, "同一时间段已有其他约球，无法报名"),
    GENDER_NOT_MATCH(41010, "性别不符合该约球要求"),
    LEVEL_NOT_MATCH(41022, "水平不符合该约球要求"),
    EDIT_LOCKED(41011, "临近开始，约球信息已锁定不可编辑"),
    PUBLISH_LIMIT_EXCEEDED(41012, "今日发布已达上限"),
    LOW_REPUTATION_BANNED(41013, "信誉分过低，暂时无法报名"),
    WAITLIST_NOT_PENDING(41014, "该报名当前状态不可撤回"),
    WAITLIST_NOT_FOUND(41015, "报名记录不存在"),
    NOT_CREATOR(41016, "仅发布者可操作"),
    CITY_NOT_OPENED(41017, "该城市暂未开通"),
    CANNOT_JOIN_OWN(41018, "不能报名自己发布的约球"),

    // ========== 评价域 42001-42999 ==========
    REVIEW_DUPLICATE(42001, "重复评价"),
    REVIEW_DEADLINE_PASSED(42002, "评价已超期"),
    REVIEW_NOT_PARTICIPANT(42003, "非本场参与者"),
    REVIEW_SELF_FORBIDDEN(42004, "不可评价自己"),
    MEETUP_CANT_REVIEW(42005, "约球不可评价"),
    SCORE_FORMAT_INVALID(42010, "比分不合赛制"),
    SCORE_VERSION_CONFLICT(42011, "比分已被其他参与者更新，请刷新后重试"),
    SCORE_PLAYER_INVALID(42012, "比分选手非法"),
    SCORE_SET_DUPLICATE(42013, "盘号重复"),

    // ========== 聊天域 41101-41199 ==========
    CHAT_USER_NOT_FOUND(41101, "聊天用户不存在"),
    CHAT_MESSAGE_EMPTY(41102, "消息内容不能为空"),
    ALREADY_JOINED_CHAT(41103, "你已加入该聊天"),

    // ========== 球场域 44001-44999 ==========
    COURT_NOT_FOUND(44001, "球场不存在"),

    // ========== 赛后收集域 43001-43999 ==========
    RECAP_NOT_FOUND(43001, "赛后收集数据不存在"),
    RECAP_SCORE_CONFLICT(43002, "比分已被其他参与者更新，请刷新后重试"),
    RECAP_REVIEW_INVALID_TYPE(43003, "评价类型不合法"),
    RECAP_REVIEW_INVALID_VALUE(43004, "评价值不合法"),
    SCORE_VERSION_MISMATCH(43005, "比分版本不一致，请刷新后重试"),
    RECAP_SCORE_NOT_FOUND(43006, "比分记录不存在"),
    INVALID_WIN_SIDE(43007, "获胜边无效"),

    // ========== 支付域 45001-45999 ==========
    PAYMENT_ORDER_NOT_FOUND(45001, "支付单不存在"),
    PAYMENT_ALREADY_PAID(45002, "支付单已支付"),
    PAYMENT_STATUS_ILLEGAL(45003, "支付单状态不允许该操作"),
    PAYMENT_NOT_PAYER(45004, "无权操作该支付单"),
    PAYMENT_CHANNEL_NOT_SUPPORTED(45005, "暂不支持该支付渠道"),
    PAYMENT_CREATE_FAILED(45006, "创建支付单失败"),
    COLLECTION_NOT_ALLOWED(45007, "当前不可发起收款"),
    SETTLEMENT_FAILED(45008, "分账失败"),
    SHARE_RECEIVER_BIND_FAILED(45009, "分账接收方绑定失败"),

    // ========== 赛事域 46001-46999 ==========
    TOURNAMENT_NOT_FOUND(46001, "赛事不存在"),
    TOURNAMENT_STATUS_ILLEGAL(46002, "赛事当前状态不允许该操作"),
    TOURNAMENT_CONFIG_INCOMPLETE(46003, "赛事配置不完整"),
    TOURNAMENT_TIME_ILLEGAL(46004, "赛事时间点设置不合法"),
    TOURNAMENT_ENTRY_NOT_FOUND(46005, "报名记录不存在"),
    TOURNAMENT_ALREADY_JOINED(46006, "您已报名该赛事"),
    TOURNAMENT_REGISTRATION_CLOSED(46007, "报名未开放或已截止"),
    TOURNAMENT_ENTRY_STATUS_ILLEGAL(46008, "报名当前状态不允许该操作"),
    TOURNAMENT_REFUND_NOT_SUPPORTED(46009, "正赛退出退款功能暂未开放，请联系客服"),
    TOURNAMENT_MATCH_VERSION_CONFLICT(46010, "比赛状态已变更，请刷新后重试"),
    TOURNAMENT_COURT_BOOKER_ALREADY_SELECTED(46011, "订场人已被选定"),
    TOURNAMENT_INVALID_COURT_BOOKER(46012, "只有待选订场人才能认领"),
    TOURNAMENT_NOT_COURT_BOOKER(46013, "只有订场人可以提交场地信息"),
    TOURNAMENT_INVALID_SCHEDULE_CONFIRM(46014, "当前状态不允许确认赛约"),
    TOURNAMENT_INVALID_RESULT_SUBMIT(46015, "当前状态不允许提交结果"),
    TOURNAMENT_INVALID_RESULT_CONFIRM(46016, "当前状态不允许确认结果"),
    TOURNAMENT_REJECT_LIMIT_REACHED(46017, "已达拒绝次数上限"),
    TOURNAMENT_INVALID_REJECT_REASON(46018, "无效的拒绝理由"),
    TOURNAMENT_REBOOK_REASON_REQUIRED(46019, "请提供打回重订理由"),
    TOURNAMENT_SCHEDULE_TIME_REQUIRED(46020, "请提供赛约时间"),
    TOURNAMENT_RESULT_WINNER_REQUIRED(46021, "请选择获胜方"),
    TOURNAMENT_SLOTS_FULL(46022, "正赛席位已满，暂无法支付");

    private final int code;
    private final String message;

}
