package com.rally.domain.meetup.peerreview;

import java.time.LocalDateTime;

/** {@code rally_review} 一条评价维度记录对应的不可变实体状态。 */
public record PeerReviewItem(
        Long id,
        String businessId,
        String meetupId,
        String fromUserId,
        String toUserId,
        PeerReviewType reviewType,
        String reviewValue,
        LocalDateTime createTime,
        LocalDateTime updateTime) {

    /** 从数据库的小写维度和值恢复记录。 */
    public static PeerReviewItem restore(
            Long id,
            String businessId,
            String meetupId,
            String fromUserId,
            String toUserId,
            String storedReviewType,
            String storedReviewValue,
            LocalDateTime createTime,
            LocalDateTime updateTime) {
        PeerReviewType type = PeerReviewType.fromStorageCode(storedReviewType);
        return new PeerReviewItem(
                id,
                businessId,
                meetupId,
                fromUserId,
                toUserId,
                type,
                PeerReviewSet.normalizeValue(type, storedReviewValue),
                createTime,
                updateTime);
    }

    static PeerReviewItem create(
            String businessId,
            String meetupId,
            String fromUserId,
            PeerReviewSubmission submission,
            String normalizedValue) {
        return new PeerReviewItem(
                null,
                businessId,
                meetupId,
                fromUserId,
                submission.toUserId(),
                submission.reviewType(),
                normalizedValue,
                null,
                null);
    }

    /** 覆盖只改变评价值；编号、双方用户、维度与创建时间保持稳定。 */
    PeerReviewItem withReviewValue(String normalizedValue) {
        return new PeerReviewItem(
                id,
                businessId,
                meetupId,
                fromUserId,
                toUserId,
                reviewType,
                normalizedValue,
                createTime,
                null);
    }

    public String storedReviewType() {
        return reviewType.storageCode();
    }
}
