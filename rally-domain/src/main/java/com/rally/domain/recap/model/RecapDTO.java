package com.rally.domain.recap.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 赛后收集详情
 */
@Data
public class RecapDTO {

    // ==================== 当前用户已填评价（按 toUser 分组） ====================
    /** key = toUserId, value = 该用户对应的评价列表 */
    private Map<String, List<ReviewItem>> myReviews;

    // ==================== 比分 ====================
    private List<ScoreItem> scores;
    /** 比分版本号，下次 submit 回传做乐观锁 */
    private Integer scoreVersion;

    // ==================== 填写状态 ====================
    /** 当前用户是否已填比分 */
    private boolean scoreFilled;

    /**
     * 评价条目
     */
    @Data
    public static class ReviewItem {
        private String toUserId;
        private String type;
        private String value;
    }

    /**
     * 比分条目
     */
    @Data
    public static class ScoreItem {
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
