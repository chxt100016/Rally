package com.rally.domain.recap.model;

import com.rally.domain.meetup.enums.MatchTypeEnum;
import com.rally.domain.recap.enums.SetFormatEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 新增比分命令（一次新增一盘）
 */
@Data
public class ScoreAddCmd implements ScoreCmd {

    /** 约球 biz_id */
    @NotBlank(message = "约球ID不能为空")
    private String meetupId;

    /** 盘号，从 1 开始 */
    @NotNull(message = "盘号不能为空")
    private Integer setNum;

    /** 赛制：GAME / TIEBREAK */
    @NotNull(message = "赛制不能为空")
    private SetFormatEnum setFormatType;

    /** 比赛类型：SINGLE / DOUBLE / RALLY */
    @NotNull(message = "比赛类型不能为空")
    private MatchTypeEnum matchType;

    /** A 侧选手1 user_id */
    @NotBlank(message = "A侧选手1不能为空")
    private String sideAPlayer1;

    /** A 侧选手2 user_id（单打为 null） */
    private String sideAPlayer2;

    /** B 侧选手1 user_id */
    @NotBlank(message = "B侧选手1不能为空")
    private String sideBPlayer1;

    /** B 侧选手2 user_id（单打为 null） */
    private String sideBPlayer2;

    /** A 侧本盘比分 */
    @NotNull(message = "A侧比分不能为空")
    private Integer sideAScore;

    /** B 侧本盘比分 */
    @NotNull(message = "B侧比分不能为空")
    private Integer sideBScore;

    /** A 侧抢七比分（本盘 6:6 时填写） */
    private Integer sideATiebreakScore;

    /** B 侧抢七比分（本盘 6:6 时填写） */
    private Integer sideBTiebreakScore;
}
