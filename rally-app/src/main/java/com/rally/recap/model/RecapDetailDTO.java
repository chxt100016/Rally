package com.rally.recap.model;

import lombok.Data;

import java.util.List;

/**
 * 赛后收集详情 DTO（嵌套在 MeetupDetail 返回中）
 */
@Data
public class RecapDetailDTO {

    // ==================== 参与人列表 ====================
    private List<ParticipantDTO> participants;

    // ==================== 当前用户已填评价 ====================
    private List<ReviewDTO> myReviews;

    // ==================== 比分 ====================
    private List<ScoreDTO> scores;
    private Integer scoreVersion;

    // ==================== 填写状态 ====================
    private boolean scoreFilled;
    private boolean reviewFilled;

    @Data
    public static class ParticipantDTO {
        private String userId;
        private String nickname;
        private String avatarUrl;
        private List<String> reviewedTypes;
    }

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
