package com.rally.domain.recap.model;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.model.RegistrationData;
import com.rally.domain.review.model.ReviewData;
import com.rally.domain.review.model.ScoreRecordData;
import com.rally.domain.utils.Assert;
import lombok.Getter;

import java.util.*;

/**
 * 赛后收集聚合根
 * <p>
 * 唯一标识：(userId, meetupId)
 * 某用户对某场活动的赛后收集上下文。
 * <p>
 * diff 判断已封装到 RecapGateway，聚合根仅保留数据持有和校验能力。
 */
@Getter
public class Recap {

    /** 当前用户 ID */
    private final String userId;
    /** 活动 ID */
    private final String meetupId;

    /** 活动上下文（只读引用） */
    private final MeetupData meetup;
    /** 参与人集合（校验被评价人合法性） */
    private final List<RegistrationData> participants;
    /** 当前用户的评价集合，键 = (toUser, type) */
    private final Map<String, ReviewData> myReviews;
    /** 比分快照 */
    private final ScoreBoardSnapshot scoreBoard;

    public Recap(String userId, String meetupId, MeetupData meetup,
                 List<RegistrationData> participants, List<ReviewData> reviews,
                 ScoreBoardSnapshot scoreBoard) {
        this.userId = userId;
        this.meetupId = meetupId;
        this.meetup = meetup;
        this.participants = participants != null ? participants : new ArrayList<>();
        this.myReviews = buildReviewMap(reviews);
        this.scoreBoard = scoreBoard;
    }

    // ======================== 校验类 ========================

    /**
     * 校验被评价人合法：toUser 须属于该活动参与人，且不为自己
     */
    public void assertValidToUser(String toUserId) {
        Assert.isTrue(!userId.equals(toUserId), BizErrorCode.REVIEW_SELF_FORBIDDEN);
        boolean isParticipant = participants.stream()
                .anyMatch(p -> toUserId.equals(p.getUserId()));
        Assert.isTrue(isParticipant, BizErrorCode.REVIEW_NOT_PARTICIPANT);
    }

    /**
     * 校验当前用户是参与者
     */
    public void assertIsParticipant() {
        boolean isParticipant = participants.stream()
                .anyMatch(p -> userId.equals(p.getUserId()));
        Assert.isTrue(isParticipant, BizErrorCode.REVIEW_NOT_PARTICIPANT);
    }

    // ======================== 内部方法 ========================

    /**
     * 构建评价 map，键 = (toUser, type)
     */
    private Map<String, ReviewData> buildReviewMap(List<ReviewData> reviews) {
        Map<String, ReviewData> map = new LinkedHashMap<>();
        if (reviews != null) {
            for (ReviewData r : reviews) {
                String key = r.getToUserId() + ":" + r.getReviewType().name();
                map.put(key, r);
            }
        }
        return map;
    }
}
