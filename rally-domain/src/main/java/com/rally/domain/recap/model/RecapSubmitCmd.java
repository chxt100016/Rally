package com.rally.domain.recap.model;

import com.rally.domain.recap.enums.RecapTypeEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 赛后收集提交命令（一次提交全部比分 + 评价）
 */
@Data
public class RecapSubmitCmd {

    /** 约球 biz_id */
    @NotBlank(message = "约球ID不能为空")
    private String meetupId;

    // ==================== 比分部分 ====================

    /** 比分版本号，用于乐观锁校验 */
    private Integer scoreVersion;

    /** 比分列表（每盘一项） */
    @Valid
    private List<ScoreItem> scores;

    // ==================== 评价部分 ====================

    /** 评价列表 */
    @Valid
    private List<ReviewItem> reviews;

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
    }

    /**
     * 单条评价
     */
    @Data
    public static class ReviewItem {
        /** 被评价人 user_id */
        @NotBlank(message = "被评价人不能为空")
        private String toUserId;

        /** 评价类型 */
        @NotNull(message = "评价类型不能为空")
        private RecapTypeEnum type;

        /** 评价值 */
        @NotBlank(message = "评价值不能为空")
        private String value;
    }
}
