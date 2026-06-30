package com.rally.domain.recap.gateway;

import com.rally.domain.recap.model.ReviewData;
import com.rally.domain.recap.model.ReviewSubmitCmd;

import java.util.List;

public interface ReviewRepository {

    void submitReviewItems(String meetupId, String fromUserId, String toUserId, List<ReviewSubmitCmd.ReviewItem> targetReviews);

    List<ReviewData> listReviewsByMeetupAndFrom(String meetupId, String fromUserId);

    List<ReviewData> findByDimension(String rallyMeetupId, String fromUserId, String toUserId, String reviewType);

    List<ReviewData> listByToUserAndType(String toUserId, String reviewType);

    List<ReviewData> listAllByToUser(String toUserId);
}
