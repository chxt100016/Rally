package com.rally.domain.user.service;

import com.rally.domain.meetup.model.PageDTO;
import com.rally.domain.user.gateway.UserFollowRepository;
import com.rally.domain.user.model.FollowListCmd;
import com.rally.domain.user.model.UserFollowData;
import com.rally.domain.utils.Assert;
import com.rally.domain.auth.enums.BizErrorCode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 用户关注领域服务
 */
@Slf4j
@Service
public class UserFollowDomainService {

    @Resource
    private UserFollowRepository userFollowRepository;

    @Resource
    private UserProfileDomainService userProfileDomainService;

    /**
     * 关注（幂等：已关注则直接返回）
     */
    public void follow(String followerId, String targetUserId) {
        Assert.isTrue(!followerId.equals(targetUserId), BizErrorCode.FOLLOW_SELF_NOT_ALLOWED);
        userProfileDomainService.get(targetUserId);
        if (userFollowRepository.exists(followerId, targetUserId)) {
            return;
        }
        UserFollowData data = new UserFollowData();
        data.setFollowerId(followerId);
        data.setFollowingId(targetUserId);
        userFollowRepository.insert(data);
    }

    /**
     * 取消关注（幂等）
     */
    public void unfollow(String followerId, String targetUserId) {
        userFollowRepository.delete(followerId, targetUserId);
    }

    /** 关注数 */
    public long countFollowing(String userId) {
        return userFollowRepository.countFollowing(userId);
    }

    /** 被关注数 */
    public long countFollowers(String userId) {
        return userFollowRepository.countFollowers(userId);
    }

    /** 是否已关注 */
    public boolean isFollowed(String followerId, String targetUserId) {
        return userFollowRepository.exists(followerId, targetUserId);
    }

    /** 从 targetIds 中筛选出已关注的子集 */
    public Set<String> filterFollowing(String followerId, Collection<String> targetIds) {
        return userFollowRepository.filterFollowing(followerId, targetIds);
    }

    /** 关注列表（游标分页） */
    public PageDTO<UserFollowData> listFollowing(String userId, FollowListCmd cmd) {
        List<UserFollowData> rows = userFollowRepository.listFollowing(userId, cmd.getLastId(), cmd.getSize() + 1);
        return toPage(rows, cmd.getSize());
    }

    /** 被关注列表（游标分页） */
    public PageDTO<UserFollowData> listFollowers(String userId, FollowListCmd cmd) {
        List<UserFollowData> rows = userFollowRepository.listFollowers(userId, cmd.getLastId(), cmd.getSize() + 1);
        return toPage(rows, cmd.getSize());
    }

    /** 多查一条判断 hasMore 并裁剪 */
    private PageDTO<UserFollowData> toPage(List<UserFollowData> rows, int size) {
        boolean hasMore = rows.size() > size;
        List<UserFollowData> list = hasMore ? new ArrayList<>(rows.subList(0, size)) : rows;
        return new PageDTO<>(list, null, hasMore);
    }
}
