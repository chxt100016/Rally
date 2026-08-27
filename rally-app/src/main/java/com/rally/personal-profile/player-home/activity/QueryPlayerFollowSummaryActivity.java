package com.rally.personalprofile.playerhome.activity;

import com.rally.domain.user.model.PlayerHomeStatsDTO;
import com.rally.domain.user.service.UserFollowDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 query-player-follow-summary：汇总目标球员的关注概况。
 */
@Component
@RequiredArgsConstructor
public class QueryPlayerFollowSummaryActivity {

    private final UserFollowDomainService userFollowDomainService;

    public PlayerHomeStatsDTO execute(String queryingUserId, String targetUserId) {
        // A1-A3 按原顺序独立执行三次关系查询；本人和无关系场景不做特殊降级。
        return new PlayerHomeStatsDTO()
                .setFollowerCount(userFollowDomainService.countFollowers(targetUserId))
                .setFollowingCount(userFollowDomainService.countFollowing(targetUserId))
                .setIsFollowed(userFollowDomainService.isFollowed(queryingUserId, targetUserId));
    }
}
