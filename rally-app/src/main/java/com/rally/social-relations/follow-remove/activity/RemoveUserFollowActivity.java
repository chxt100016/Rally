package com.rally.socialrelations.followremove.activity;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rally.db.userFollow.entity.UserFollowPO;
import com.rally.db.userFollow.service.UserFollowService;
import com.rally.domain.social.userfollow.UnfollowUserCommand;
import com.rally.domain.social.userfollow.UserFollow;
import com.rally.domain.social.userfollow.UserFollowPersistence;
import com.rally.domain.social.userfollow.UserFollowState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 remove-user-follow：幂等删除当前用户指向目标编号的关注关系。
 */
@Component
@RequiredArgsConstructor
public class RemoveUserFollowActivity {

    private final UserFollowService userFollowService;

    public void execute(String currentUserId, String targetUserId) {
        // A1-A3：编号保持入口原值，不查询目标或关系，也不拒绝本人目标。
        // C2 直接按方向物理删除；零行和一行均按无数据成功收场。
        UserFollow.unfollow(
                new UnfollowUserCommand(currentUserId, targetUserId),
                new UnfollowPersistence());
    }

    /** 将现有 user_follow 写模型适配为聚合 C2 持久化端口。 */
    private final class UnfollowPersistence implements UserFollowPersistence {

        @Override
        public UserFollowState findByDirection(String followerId, String followingId) {
            throw new UnsupportedOperationException(
                    "remove-user-follow 不预查关注关系");
        }

        @Override
        public void insert(UserFollowState state) {
            throw new UnsupportedOperationException(
                    "remove-user-follow 只允许调用解除关注 C2");
        }

        @Override
        public boolean deleteByDirection(String followerId, String followingId) {
            return userFollowService.remove(new LambdaQueryWrapper<UserFollowPO>()
                    .eq(UserFollowPO::getFollowerId, followerId)
                    .eq(UserFollowPO::getFollowingId, followingId));
        }
    }
}
