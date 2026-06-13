package com.rally.domain.recap.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 比分提交命令（仅提交比分）
 */
@Data
public class ScoreSubmitCmd {

    /** 约球 biz_id */
    @NotBlank(message = "约球ID不能为空")
    private String meetupId;

    /** 比分版本号，用于乐观锁校验 */
    private Integer scoreVersion;

    /** 比分列表（每盘一项） */
    @Valid
    private List<ScoreItem> scores;

    /**
     * 单盘比分
     */
    @Data
    public static class ScoreItem {
        /** 盘号，从 1 开始 */
        @NotNull(message = "盘号不能为空")
        private Integer setNum;

        /** 赛制：GAMES_4 / GAMES_6 / TIEBREAK */
        @NotBlank(message = "赛制不能为空")
        private String setFormat;

        /** A 侧选手1 user_id */
        @NotBlank(message = "A侧选手1不能为空")
        private String sideAPlayer1;

        /** A 侧选手1昵称 */
        private String sideAPlayer1Nickname;

        /** A 侧选手1头像URL */
        private String sideAPlayer1Avatar;

        /** A 侧选手2 user_id（单打为 null） */
        private String sideAPlayer2;

        /** A 侧选手2昵称 */
        private String sideAPlayer2Nickname;

        /** A 侧选手2头像URL */
        private String sideAPlayer2Avatar;

        /** B 侧选手1 user_id */
        @NotBlank(message = "B侧选手1不能为空")
        private String sideBPlayer1;

        /** B 侧选手1昵称 */
        private String sideBPlayer1Nickname;

        /** B 侧选手1头像URL */
        private String sideBPlayer1Avatar;

        /** B 侧选手2 user_id（单打为 null） */
        private String sideBPlayer2;

        /** B 侧选手2昵称 */
        private String sideBPlayer2Nickname;

        /** B 侧选手2头像URL */
        private String sideBPlayer2Avatar;

        /** A 侧本盘比分 */
        @NotNull(message = "A侧比分不能为空")
        private Integer sideAScore;

        /** B 侧本盘比分 */
        @NotNull(message = "B侧比分不能为空")
        private Integer sideBScore;
    }
}
