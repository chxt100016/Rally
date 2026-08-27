package com.rally.user;

import com.rally.domain.user.model.PlayerHomeDTO;
import com.rally.personalprofile.playerhome.activity.QueryPlayerFollowSummaryActivity;
import com.rally.personalprofile.playerhome.activity.QueryPlayerMeetupSummaryActivity;
import com.rally.personalprofile.playerhome.activity.QueryPlayerPublicProfileActivity;
import com.rally.personalprofile.playerhome.activity.QueryPlayerReviewSummaryActivity;
import com.rally.personalprofile.playerhome.activity.QueryPlayerScoreSummaryActivity;
import com.rally.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlayerHomeAppService {

    private final QueryPlayerPublicProfileActivity queryPlayerPublicProfileActivity;
    private final QueryPlayerFollowSummaryActivity queryPlayerFollowSummaryActivity;
    private final QueryPlayerMeetupSummaryActivity queryPlayerMeetupSummaryActivity;
    private final QueryPlayerReviewSummaryActivity queryPlayerReviewSummaryActivity;
    private final QueryPlayerScoreSummaryActivity queryPlayerScoreSummaryActivity;

    public PlayerHomeDTO getPlayerHome(String targetUserId) {
        String queryingUserId = UserContext.get();
        return queryPlayerPublicProfileActivity.execute(targetUserId)
                .setStats(queryPlayerFollowSummaryActivity.execute(queryingUserId, targetUserId))
                .setMeetup(queryPlayerMeetupSummaryActivity.execute(targetUserId))
                .setReview(queryPlayerReviewSummaryActivity.execute(targetUserId))
                .setSetScore(queryPlayerScoreSummaryActivity.execute(targetUserId));
    }
}
