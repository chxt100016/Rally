package com.rally.domain.recap.model;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.recap.enums.AttendanceEnum;
import com.rally.domain.recap.enums.NtrpVoteEnum;
import com.rally.domain.recap.enums.ReviewTypeEnum;
import com.rally.domain.utils.Assert;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 评价提交命令（仅提交评价）
 */
@Data
public class ReviewSubmitCmd {

    /** 约球 biz_id */
    @NotBlank(message = "约球ID不能为空")
    private String meetupId;

    /** 评价列表 */
    @Valid
    private List<ReviewItem> reviews;

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
        private ReviewTypeEnum type;

        /** 评价值 */
        @NotBlank(message = "评价值不能为空")
        private String value;
    }

    /**
     * 校验单条评价的 value 是否在对应类型枚举范围内
     */
    public static void assertValidReviewValue(ReviewTypeEnum type, String value) {
        boolean valid = switch (type) {
            case LEVEL_VOTE -> isValidEnum(NtrpVoteEnum.class, value);
            case ATTENDANCE_VOTE -> isValidEnum(AttendanceEnum.class, value);
            case TAG -> value != null && !value.isBlank();
        };
        Assert.isTrue(valid, BizErrorCode.RECAP_REVIEW_INVALID_VALUE);
    }

    private static <E extends Enum<E>> boolean isValidEnum(Class<E> enumClass, String value) {
        try {
            Enum.valueOf(enumClass, value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
