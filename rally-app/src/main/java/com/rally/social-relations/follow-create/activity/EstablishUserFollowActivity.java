package com.rally.socialrelations.followcreate.activity;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.db.userFollow.entity.UserFollowPO;
import com.rally.db.userFollow.service.UserFollowService;
import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.social.userfollow.FollowUserCommand;
import com.rally.domain.social.userfollow.UserFollow;
import com.rally.domain.social.userfollow.UserFollowPersistence;
import com.rally.domain.social.userfollow.UserFollowState;
import com.rally.domain.user.service.UserProfileDomainService;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 establish-user-follow：校验目标并幂等建立单向关注。
 */
@Component
@RequiredArgsConstructor
public class EstablishUserFollowActivity {

    private final UserProfileDomainService userProfileDomainService;
    private final UserFollowService userFollowService;

    public void execute(String currentUserId, String targetUserId) {
        // A1：保留 main 的完全相等判断和对外错误码。
        Assert.isTrue(!currentUserId.equals(targetUserId),
                BizErrorCode.FOLLOW_SELF_NOT_ALLOWED);

        // A2：只确认基础账户存在；缺少网球档案不影响关注。
        userProfileDomainService.get(targetUserId);

        // A3：不开启显式事务。C1 预查已存在时幂等成功；预查未命中
        // 后只插入一次，并发唯一键冲突与其他持久化异常原样向上传播。
        UserFollow.follow(
                new FollowUserCommand(currentUserId, targetUserId, true),
                IdWorker::getIdStr,
                new FollowPersistence());
    }

    /** 将现有 user_follow 写模型适配为聚合 C1 持久化端口。 */
    private final class FollowPersistence implements UserFollowPersistence {

        @Override
        public UserFollowState findByDirection(String followerId, String followingId) {
            UserFollowPO existing = userFollowService.lambdaQuery()
                    .eq(UserFollowPO::getFollowerId, followerId)
                    .eq(UserFollowPO::getFollowingId, followingId)
                    .one();
            if (existing == null) {
                return null;
            }
            return new UserFollowState(
                    existing.getId(),
                    existing.getBizId(),
                    existing.getFollowerId(),
                    existing.getFollowingId(),
                    existing.getCreateTime(),
                    existing.getUpdateTime());
        }

        @Override
        public void insert(UserFollowState state) {
            UserFollowPO created = new UserFollowPO();
            created.setBizId(state.bizId());
            created.setFollowerId(state.followerId());
            created.setFollowingId(state.followingId());
            userFollowService.save(created);
        }

        @Override
        public boolean deleteByDirection(String followerId, String followingId) {
            throw new UnsupportedOperationException(
                    "establish-user-follow 只允许调用关注 C1");
        }
    }
}
