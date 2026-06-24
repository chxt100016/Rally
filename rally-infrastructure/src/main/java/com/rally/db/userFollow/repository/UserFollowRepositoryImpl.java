package com.rally.db.userFollow.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.db.userFollow.convert.UserFollowConvertMapper;
import com.rally.db.userFollow.entity.UserFollowPO;
import com.rally.db.userFollow.service.UserFollowService;
import com.rally.domain.user.gateway.UserFollowRepository;
import com.rally.domain.user.model.UserFollowData;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserFollowRepositoryImpl implements UserFollowRepository {

    private final UserFollowService userFollowService;
    private static final UserFollowConvertMapper MAPPER = UserFollowConvertMapper.INSTANCE;

    @Override
    public void insert(UserFollowData data) {
        UserFollowPO po = MAPPER.toPO(data);
        po.setBizId(IdWorker.getIdStr());
        userFollowService.save(po);
    }

    @Override
    public void delete(String followerId, String followingId) {
        userFollowService.remove(new LambdaQueryWrapper<UserFollowPO>()
                .eq(UserFollowPO::getFollowerId, followerId)
                .eq(UserFollowPO::getFollowingId, followingId));
    }

    @Override
    public boolean exists(String followerId, String followingId) {
        return userFollowService.lambdaQuery()
                .eq(UserFollowPO::getFollowerId, followerId)
                .eq(UserFollowPO::getFollowingId, followingId)
                .exists();
    }

    @Override
    public long countFollowing(String followerId) {
        return userFollowService.lambdaQuery()
                .eq(UserFollowPO::getFollowerId, followerId)
                .count();
    }

    @Override
    public long countFollowers(String followingId) {
        return userFollowService.lambdaQuery()
                .eq(UserFollowPO::getFollowingId, followingId)
                .count();
    }

    @Override
    public List<UserFollowData> listFollowing(String followerId, String lastId, int limit) {
        List<UserFollowPO> poList = userFollowService.lambdaQuery()
                .eq(UserFollowPO::getFollowerId, followerId)
                .lt(StringUtils.isNotBlank(lastId), UserFollowPO::getBizId, lastId)
                .orderByDesc(UserFollowPO::getBizId)
                .last("LIMIT " + limit)
                .list();
        return MAPPER.toDataList(poList);
    }

    @Override
    public List<UserFollowData> listFollowers(String followingId, String lastId, int limit) {
        List<UserFollowPO> poList = userFollowService.lambdaQuery()
                .eq(UserFollowPO::getFollowingId, followingId)
                .lt(StringUtils.isNotBlank(lastId), UserFollowPO::getBizId, lastId)
                .orderByDesc(UserFollowPO::getBizId)
                .last("LIMIT " + limit)
                .list();
        return MAPPER.toDataList(poList);
    }

    @Override
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
