package com.rally.domain.system.enums;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 系统配置 key 枚举，集中维护 {@link com.rally.domain.system.SystemConfig} 读取的配置项、说明及默认值
 */
@Getter
@AllArgsConstructor
public enum SystemConfigKey {

    // ==================== NTRP / 复核期 ====================

    /** NTRP 调整后低分段（&lt;3.0）冷静期天数 */
    SCORE_NTRP_COOLDOWN_LOW_DAYS("score.ntrp.cooldown_low_days", "NTRP 低分段冷静期天数", "30"),
    /** NTRP 调整后中分段冷静期天数 */
    SCORE_NTRP_COOLDOWN_MID_DAYS("score.ntrp.cooldown_mid_days", "NTRP 中分段冷静期天数", "60"),
    /** NTRP 调整后高分段冷静期天数 */
    SCORE_NTRP_COOLDOWN_HIGH_DAYS("score.ntrp.cooldown_high_days", "NTRP 高分段冷静期天数", "90"),
    /** NTRP 变动达到该幅度触发复核期 */
    SCORE_REVIEW_PERIOD_TRIGGER_NTRP_DELTA("score.review_period.trigger_ntrp_delta", "触发复核期的 NTRP 变化阈值", "0.5"),
    /** 复核期内需完成的比赛场数 */
    SCORE_REVIEW_PERIOD_REQUIRED_MATCHES("score.review_period.required_matches", "复核期所需完成的比赛场数", "3"),
    /** 复核期内出现不良评价时对可信度的惩罚分值 */
    SCORE_REVIEW_PERIOD_PENALTY_CREDIBILITY("score.review_period.penalty_credibility", "复核期出现不良评价时的可信度惩罚分值", "50"),

    // ==================== 信誉分 ====================

    /** 未到场对信誉分的扣分 */
    SCORE_REPUTATION_NO_SHOW("score.reputation.no_show", "未到场对信誉分的扣分", "-25"),
    /** 迟到对信誉分的扣分 */
    SCORE_REPUTATION_LATE("score.reputation.late", "迟到对信誉分的扣分", "-10"),
    /** 按时到场对信誉分的加分 */
    SCORE_REPUTATION_ON_TIME("score.reputation.on_time", "按时到场对信誉分的加分", "2"),

    // ==================== 可信度 ====================

    /** 可信度计算的比赛统计时间窗口（天） */
    SCORE_CREDIBILITY_MATCH_WINDOW_DAYS("score.credibility.match_window_days", "可信度计算的比赛统计时间窗口天数", "90"),
    /** 每场已完成比赛贡献的可信度分数 */
    SCORE_CREDIBILITY_MATCH_PER_SCORE("score.credibility.match_per_score", "每场已完成比赛贡献的可信度分数", "6"),
    /** 比赛维度贡献的可信度分数上限 */
    SCORE_CREDIBILITY_MATCH_SCORE_CAP("score.credibility.match_score_cap", "比赛维度贡献的可信度分数上限", "60"),
    /** 每个视频贡献的可信度分数 */
    SCORE_CREDIBILITY_VIDEO_PER_SCORE("score.credibility.video_per_score", "每个视频贡献的可信度分数", "5"),
    /** 视频维度贡献的可信度分数上限 */
    SCORE_CREDIBILITY_VIDEO_CAP("score.credibility.video_cap", "视频维度贡献的可信度分数上限", "25"),

    // ==================== 校准度 ====================

    /** 单个评价目标允许计入的最大调低（lower）票数，超出部分剔除 */
    ANTI_ABUSE_LOWER_VOTE_MAX_PER_TARGET("anti_abuse.lower_vote_max_per_target", "对单个目标计入的调低票数上限（防滥用）", "3"),
    /** 校准度计算所需的最小有效投票数 */
    SCORE_CALIBRATION_MIN_VOTES("score.calibration.min_votes", "校准度计算所需的最小有效投票数", "10"),
    /** 投票数不足时给定的默认校准度分数 */
    SCORE_CALIBRATION_SCORE_INSUFFICIENT("score.calibration.score_insufficient", "投票数不足时的默认校准度分数", "80"),
    /** 校准度偏差档位阈值 T1 */
    SCORE_CALIBRATION_DEVIATION_T1("score.calibration.deviation_t1", "校准度偏差档位阈值 T1", "0.20"),
    /** 校准度偏差档位阈值 T2 */
    SCORE_CALIBRATION_DEVIATION_T2("score.calibration.deviation_t2", "校准度偏差档位阈值 T2", "0.50"),
    /** 偏差小于 T1 时的校准度分数 */
    SCORE_CALIBRATION_SCORE_UNDER_T1("score.calibration.score_under_t1", "偏差小于 T1 时的校准度分数", "100"),
    /** 偏差在 [T1, T2) 且自评偏高（BELOW）时的校准度分数 */
    SCORE_CALIBRATION_SCORE_BELOW_T1_T2("score.calibration.score_below_t1_t2", "偏差在 T1~T2 之间且自评偏高时的校准度分数", "75"),
    /** 偏差在 [T1, T2) 且自评偏低（ABOVE）时的校准度分数 */
    SCORE_CALIBRATION_SCORE_ABOVE_T1_T2("score.calibration.score_above_t1_t2", "偏差在 T1~T2 之间且自评偏低时的校准度分数", "50"),
    /** 偏差大于等于 T2 且自评偏高（BELOW）时的校准度分数 */
    SCORE_CALIBRATION_SCORE_BELOW_T2("score.calibration.score_below_t2", "偏差大于等于 T2 且自评偏高时的校准度分数", "55"),
    /** 偏差大于等于 T2 且自评偏低（ABOVE）时的校准度分数 */
    SCORE_CALIBRATION_SCORE_ABOVE_T2("score.calibration.score_above_t2", "偏差大于等于 T2 且自评偏低时的校准度分数", "20"),

    // ==================== 综合评级 ====================

    /** 信誉分在综合评分中的权重 */
    SCORE_WEIGHTS_REPUTATION("score.weights.reputation", "信誉分在综合评分中的权重", "0.5"),
    /** 可信度在综合评分中的权重 */
    SCORE_WEIGHTS_CREDIBILITY("score.weights.credibility", "可信度在综合评分中的权重", "0.3"),
    /** 校准度在综合评分中的权重 */
    SCORE_WEIGHTS_CALIBRATION("score.weights.calibration", "校准度在综合评分中的权重", "0.2"),
    /** 综合评分达到 S 档的分数阈值 */
    SCORE_RATING_S_THRESHOLD("score.rating.s_threshold", "综合评分 S 档的分数阈值", "1200"),
    /** 综合评分达到 A 档的分数阈值 */
    SCORE_RATING_A_THRESHOLD("score.rating.a_threshold", "综合评分 A 档的分数阈值", "1000"),
    /** 综合评分达到 B 档的分数阈值 */
    SCORE_RATING_B_THRESHOLD("score.rating.b_threshold", "综合评分 B 档的分数阈值", "800"),
    /** 用户注册初始信誉分 */
    SCORE_INIT_REPUTATION("score.init.reputation", "用户注册初始信誉分", "1000"),
    /** 用户注册初始可信度 */
    SCORE_INIT_CREDIBILITY("score.init.credibility", "用户注册初始可信度", "1000"),
    /** 用户注册初始校准度 */
    SCORE_INIT_CALIBRATION("score.init.calibration", "用户注册初始校准度", "800"),
    /** 最大分 */
    SCORE_MAX("score.max", "用户注册初始校准度", "1500"),

    // ==================== 评分说明文案 ====================

    /** 信誉分说明文案 */
    SCORE_INFO_REPUTATION("score.info.reputation", "信誉分说明文案", ""),
    /** 可信度说明文案 */
    SCORE_INFO_CREDIBILITY("score.info.credibility", "可信度说明文案", ""),
    /** 校准度说明文案 */
    SCORE_INFO_CALIBRATION("score.info.calibration", "校准度说明文案", ""),

    // ==================== 约球（Meetup） ====================

    /** 已开通城市编码列表 */
    MEETUP_CITY_OPENED_CODES("meetup.city.opened_codes", "已开通城市编码列表", "330100,330200"),
    /** 报名加入约球所需的最低信誉分 */
    MEETUP_JOIN_MIN_REPUTATION_SCORE("meetup.join.min_reputation_score", "报名加入约球所需的最低信誉分", "30"),
    /** 退出约球产生惩罚的距开始时间阈值（小时） */
    MEETUP_QUIT_PENALTY_THRESHOLD_HOURS("meetup.quit.penalty_threshold_hours", "退出约球产生惩罚的距开始时间阈值（小时）", "6"),
    /** 开始前不足该阈值退出对信誉分的处罚 */
    MEETUP_QUIT_PENALTY_UNDER_6H("meetup.quit.penalty_under_6h", "开始前不足阈值退出对信誉分的处罚", "25"),
    /** 约球开始前禁止编辑的提前分钟数 */
    MEETUP_EDIT_LOCK_MINUTES_BEFORE_START("meetup.edit_lock_minutes_before_start", "约球开始前禁止编辑的提前分钟数", "60"),
    /** 提前 24 小时以上关闭约球对发布者信誉分的处罚 */
    MEETUP_CANCEL_PENALTY_24H_OUT("meetup.cancel.penalty_24h_out", "提前 24 小时以上关闭约球的信誉分处罚", "5"),
    /** 提前 12~24 小时关闭约球对发布者信誉分的处罚 */
    MEETUP_CANCEL_PENALTY_12_24H("meetup.cancel.penalty_12_24h", "提前 12~24 小时关闭约球的信誉分处罚", "10"),
    /** 提前 6~12 小时关闭约球对发布者信誉分的处罚 */
    MEETUP_CANCEL_PENALTY_6_12H("meetup.cancel.penalty_6_12h", "提前 6~12 小时关闭约球的信誉分处罚", "15"),
    /** 提前不足 6 小时关闭约球对发布者信誉分的处罚 */
    MEETUP_CANCEL_PENALTY_UNDER_6H("meetup.cancel.penalty_under_6h", "提前不足 6 小时关闭约球的信誉分处罚", "25"),

    // ==================== 评价 ====================

    /** 约球结束后允许评价的截止天数 */
    REVIEW_DEADLINE_DAYS("review.deadline_days", "约球结束后允许评价的截止天数", "30"),
    REVIEW_DEFAULT_TAGS("review.default_tag", "约球评价可用默认标签", "正手好,反手好,拉球稳,发球好,跑得快,球很转"),

    // ==================== 防滥用 ====================

    /** 每日发布约球数量上限 */
    ANTI_ABUSE_PUBLISH_PER_DAY_LIMIT("anti_abuse.publish_per_day_limit", "每日发布约球数量上限", "50"),

    // ==================== 用户上传 ====================

    /** 用户可上传视频数量上限 */
    USER_VIDEO_MAX_COUNT("user.video.max_count", "用户可上传视频数量上限", "3"),
    /** 用户上传视频的文件大小上限（MB） */
    USER_VIDEO_MAX_SIZE_MB("user.video.max_size_mb", "用户上传视频的文件大小上限（MB）", "20"),
    /** 用户上传视频时间最大秒数（秒） */
    USER_VIDEO_MAX_SECOND("user.video.max_second", "用户上传视频时间最大秒数", "60"),
    /** 用户上传头像的文件大小上限（MB） */
    USER_AVATAR_MAX_SIZE_MB("user.avatar.max_size_mb", "用户上传头像的文件大小上限（MB）", "5"),
    USER_HINT_VIDEO("user.hint.video", "", "· 单个视频时长 ≤ 60 秒，大小 ≤ 20MB\n· 建议横屏拍摄，画面清晰稳定\n· 完整展示正手 / 反手 / 发球等技术动作\n· 每位用户最多上传 3 个视频"),
    USER_HINT_NTRP("user.hint.ntrp", "", ""),

    // ==================== 支付（Payment） ====================

    /** 微信支付手续费率（千 6） */
    PAYMENT_WECHAT_FEE_RATE("payment.wechat.fee_rate", "微信支付手续费率（千6）", "0.006"),
    /** 手续费展示文案 */
    PAYMENT_WECHAT_FEE_DESC("payment.wechat.fee_desc", "手续费展示文案", "含微信支付手续费 0.6%"),
    /** 待支付超时分钟数，0=不超时（默认） */
    PAYMENT_PAY_TIMEOUT_MINUTES("payment.pay_timeout_minutes", "待支付超时分钟数，0=不超时（默认）", "0"),
    /** 微信分账接收方数量上限，接近时触发 LRU 淘汰（后续） */
    PAYMENT_WECHAT_SHARE_RECEIVER_MAX("payment.wechat.share_receiver_max", "微信分账接收方数量上限", "20000"),

    // ==================== 系统 ====================

    /** 群聊二维码（base64） */
    SYSTEM_GROUP_QRCODE("system.group.qrcode", "群聊二维码 base64", ""),
    /** 启动页封面图存储 key */
    SYSTEM_SPLASH_COVER_KEY("system.splash.cover.key", "启动页封面图存储 key", "default/splash-cover.jpg")

    ;

    private static final Map<String, SystemConfigKey> KEY_MAP = new HashMap<>();

    static {
        for (SystemConfigKey k : values()) {
            KEY_MAP.put(k.key, k);
        }
    }

    /**
     * 根据配置 key 字符串查找对应的枚举项
     *
     * @param key 配置 key
     * @return 对应枚举项，未找到返回 null
     */
    public static SystemConfigKey getByKey(String key) {
        return KEY_MAP.get(key);
    }

    /** 配置项 key */
    private final String key;
    /** 配置项说明 */
    private final String desc;
    /** 配置项默认值（字符串形式，按使用方法解析为对应类型） */
    private final String defaultValue;
}
