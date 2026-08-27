package com.rally.domain.social.userfollow;

/**
 * {@code user_follow} 的唯一写端口。
 *
 * <p>插入时不吸收、转换或重试任何持久化失败；
 * {@code uk_biz_id}、{@code uk_follow_rel} 冲突与其他数据库异常均向上游传播。</p>
 */
public interface UserFollowPersistence {

    /** 按方向读取现有关系，不存在时返回 {@code null}。 */
    UserFollowState findByDirection(String followerId, String followingId);

    /** 插入一条完整关注关系；失败时直接抛出持久化异常。 */
    void insert(UserFollowState state);

    /** 按方向物理删除；返回值只表示是否实际删除了一行。 */
    boolean deleteByDirection(String followerId, String followingId);
}
