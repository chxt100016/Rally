package com.rally.db.userFollow.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rally.db.userFollow.entity.UserFollowPO;
import com.rally.db.userFollow.service.UserFollowService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class UserFollowRepository {

    private final UserFollowService userFollowService;

    public UserFollowPO save(UserFollowPO po) {
        return userFollowService.insert(po);
    }

    public void delete(String followerId, String followingId) {
        userFollowService.remove(new LambdaQueryWrapper<UserFollowPO>()
                .eq(UserFollowPO::getFollowerId, followerId)
                .eq(UserFollowPO::getFollowingId, followingId));
    }

    public boolean exists(String followerId, String followingId) {
        return userFollowService.lambdaQuery()
                .eq(UserFollowPO::getFollowerId, followerId)
                .eq(UserFollowPO::getFollowingId, followingId)
                .exists();
    }

    public long countFollowing(String followerId) {
        return userFollowService.lambdaQuery()
                .eq(UserFollowPO::getFollowerId, followerId)
                .count();
    }

    public long countFollowers(String followingId) {
        return userFollowService.lambdaQuery()
                .eq(UserFollowPO::getFollowingId, followingId)
                .count();
    }

    public List<UserFollowPO> listFollowing(String followerId, String lastId, int limit) {
        return userFollowService.lambdaQuery()
                .eq(UserFollowPO::getFollowerId, followerId)
                .lt(StringUtils.isNotBlank(lastId), UserFollowPO::getBizId, lastId)
                .orderByDesc(UserFollowPO::getBizId)
                .last("LIMIT " + limit)
                .list();
    }

    public List<UserFollowPO> listFollowers(String followingId, String lastId, int limit) {
        return userFollowService.lambdaQuery()
                .eq(UserFollowPO::getFollowingId, followingId)
                .lt(StringUtils.isNotBlank(lastId), UserFollowPO::getBizId, lastId)
                .orderByDesc(UserFollowPO::getBizId)
                .last("LIMIT " + limit)
                .list();
    }

    public Set<String> filterFollowing(String followerId, Collection<String> targetIds) {
        if (targetIds == null || targetIds.isEmpty()) {
            return Set.of();
        }
        return userFollowService.lambdaQuery()
                .select(UserFollowPO::getFollowingId)
                .eq(UserFollowPO::getFollowerId, followerId)
                .in(UserFollowPO::getFollowingId, targetIds)
                .list()
                .stream()
                .map(UserFollowPO::getFollowingId)
                .collect(Collectors.toSet());
    }
}
