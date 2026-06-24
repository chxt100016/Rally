package com.rally.domain.user.gateway;

import com.rally.domain.user.model.UserFollowData;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 用户关注关系网关
 */
public interface UserFollowRepository {

    /** 新增关注关系 */
    void insert(UserFollowData data);

    /** 删除关注关系 */
    void delete(String followerId, String followingId);

    /** 是否已关注 */
    boolean exists(String followerId, String followingId);

    /** 关注数（followerId 关注了多少人） */
    long countFollowing(String followerId);

    /** 被关注数（followingId 被多少人关注） */
    long countFollowers(String followingId);

    /** 关注列表（按 bizId 倒序游标分页，最多 limit 条） */
    List<UserFollowData> listFollowing(String followerId, String lastId, int limit);

    /** 被关注列表（按 bizId 倒序游标分页，最多 limit 条） */
    List<UserFollowData> listFollowers(String followingId, String lastId, int limit);

    /** 从 targetIds 中筛选出 followerId 已关注的子集 */
    Set<String> filterFollowing(String followerId, Collection<String> targetIds);
}
