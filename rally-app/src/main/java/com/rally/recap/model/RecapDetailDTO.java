package com.rally.recap.model;

import lombok.Data;

import java.util.List;

/**
 * 赛后收集详情 DTO
 */
@Data
public class RecapDetailDTO {

    // ==================== 当前用户已填评价 ====================
    private List<ReviewDTO> myReviews;

    // ==================== 比分 ====================
    private List<ScoreDTO> scores;
    private Integer scoreVersion;

    // ==================== 填写状态 ====================
    private boolean scoreFilled;

    @Data
    public static class ReviewDTO {
        private String toUserId;
        private String type;
        private String value;
    }

    @Data
    public static class ScoreDTO {
        private String bizId;
        private Integer setNum;
        private String setFormat;
        private String sideAPlayer1;
        private String sideAPlayer2;
        private String sideBPlayer1;
        private String sideBPlayer2;
        private Integer sideAScore;
        private Integer sideBScore;
    }
}
