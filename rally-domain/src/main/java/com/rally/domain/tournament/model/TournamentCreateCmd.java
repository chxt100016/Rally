package com.rally.domain.tournament.model;

import com.rally.domain.tournament.enums.TournamentGenderLimitEnum;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建赛事草稿入参
 */
@Data
public class TournamentCreateCmd {

    /** 赛事名称 */
    @NotBlank(message = "请填写赛事名称")
    @Size(max = 128, message = "赛事名称不超过128字符")
    private String tournamentName;

    /** 活动海报图片key（对象存储） */
    private String posterKey;

    /** 城市编码 */
    @NotBlank(message = "请选择城市")
    private String cityCode;

    /** NTRP等级：3.0/3.5/4.0... */
    @NotBlank(message = "请选择NTRP等级")
    private String ntrpLevel;

    /** 性别限制 */
    @NotNull(message = "请选择性别限制")
    private TournamentGenderLimitEnum genderLimit;

    /** 正赛签位：16/32/64 */
    @NotNull(message = "请选择正赛签位数")
    private Integer totalSlots;

    /** 几强后转线下：4/8/16 */
    @NotNull(message = "请填写转线下轮次")
    private Integer offlineFromRound;

    /** 资格赛每组人数，默认2 */
    @NotNull(message = "请填写资格赛组人数")
    @Min(value = 2, message = "资格赛组人数至少2人")
    private Integer qualifierGroupSize;

    /** 报名费，单位：分 */
    @NotNull(message = "请填写报名费")
    @Min(value = 0, message = "报名费不能为负")
    private Long entryFee;

    /** 报名开始时间 */
    @NotNull(message = "请选择报名开始时间")
    private LocalDateTime registrationStartTime;

    /** 报名截止时间，可空 */
    private LocalDateTime registrationEndTime;

    /** 资格赛开始时间 */
    @NotNull(message = "请选择资格赛开始时间")
    private LocalDateTime qualifierStartTime;

    /** 资格赛截止时间，可空表示永久有效 */
    private LocalDateTime qualifierEndTime;

    /** 资格赛阶段拒绝次数上限 */
    @NotNull(message = "请填写资格赛拒绝次数上限")
    @Min(value = 0, message = "拒绝次数上限不能为负")
    private Integer qualifierRejectLimit;

    /** 正赛阶段拒绝次数上限 */
    @NotNull(message = "请填写正赛拒绝次数上限")
    @Min(value = 0, message = "拒绝次数上限不能为负")
    private Integer mainDrawRejectLimit;

    /** 比赛规则描述 */
    @Size(max = 5000, message = "比赛规则描述不超过5000字符")
    private String matchRuleDescription;
}
