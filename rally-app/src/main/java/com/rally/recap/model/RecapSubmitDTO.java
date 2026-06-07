package com.rally.recap.model;

import com.rally.domain.recap.enums.RecapTypeEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 赛后收集提交入参 DTO
 */
@Data
public class RecapSubmitDTO {

    /** 约球 biz_id */
    @NotBlank(message = "约球ID不能为空")
    private String meetupId;

    /** 比分版本号，用于乐观锁校验 */
    private Integer scoreVersion;

    /** 比分列表 */
    @Valid
    private List<ScoreItem> scores;

    /** 评价列表 */
    @Valid
    private List<ReviewItem> reviews;

    @Data
    public static class ScoreItem {
        @NotNull(message = "盘号不能为空")
        private Integer setNum;

        @NotBlank(message = "赛制不能为空")
        private String setFormat;

        @NotBlank(message = "A侧选手1不能为空")
        private String sideAPlayer1;

        private String sideAPlayer2;

        @NotBlank(message = "B侧选手1不能为空")
        private String sideBPlayer1;

        private String sideBPlayer2;

        @NotNull(message = "A侧比分不能为空")
        private Integer sideAScore;

        @NotNull(message = "B侧比分不能为空")
        private Integer sideBScore;
    }

    @Data
    public static class ReviewItem {
        @NotBlank(message = "被评价人不能为空")
        private String toUserId;

        @NotNull(message = "评价类型不能为空")
        private RecapTypeEnum type;

        @NotBlank(message = "评价值不能为空")
        private String value;
    }
}
