package com.rally.db.userFollow.gateway;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.db.userFollow.convert.UserFollowConvertMapper;
import com.rally.db.userFollow.entity.UserFollowPO;
import com.rally.db.userFollow.repository.UserFollowRepository;
import com.rally.domain.user.gateway.UserFollowGateway;
import com.rally.domain.user.model.UserFollowData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class UserFollowGatewayImpl implements UserFollowGateway {

    private final UserFollowRepository userFollowRepository;

    @Override
    public void insert(UserFollowData data) {
        UserFollowPO po = UserFollowConvertMapper.INSTANCE.toPO(data);
        po.setBizId(IdWorker.getIdStr());
        userFollowRepository.save(po);
    }

    @Override
    public void delete(String followerId, String followingId) {
        userFollowRepository.delete(followerId, followingId);
    }

    @Override
    public boolean exists(String followerId, String followingId) {
        return userFollowRepository.exists(followerId, followingId);
    }

    @Override
    public long countFollowing(String followerId) {
        return userFollowRepository.countFollowing(followerId);
    }

    @Override
    public long countFollowers(String followingId) {
        return userFollowRepository.countFollowers(followingId);
    }

    @Override
    public List<UserFollowData> listFollowing(String followerId, String lastId, int limit) {
        return UserFollowConvertMapper.INSTANCE.toDataList(userFollowRepository.listFollowing(followerId, lastId, limit));
    }

    @Override
    public List<UserFollowData> listFollowers(String followingId, String lastId, int limit) {
        return UserFollowConvertMapper.INSTANCE.toDataList(userFollowRepository.listFollowers(followingId, lastId, limit));
    }

    @Override
    public Set<String> filterFollowing(String followerId, Collection<String> targetIds) {
        return userFollowRepository.filterFollowing(followerId, targetIds);
    }
}
