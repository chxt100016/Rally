package com.rally.domain.social.userfollow;

import java.util.Objects;
import java.util.Optional;

/**
 * 一个方向的用户关注聚合根。
 *
 * <p>数据表没有表示 ABSENT 的行，因此聚合在该状态下仅保留方向。
 * 对 {@code user_follow} 的新增与删除只能通过 C1/C2 进入。</p>
 */
public final class UserFollow {

    public static final String FOLLOW_IDENTITY_IMMUTABLE =
            "FOLLOW_IDENTITY_IMMUTABLE";
    public static final String FOLLOW_RELATION_CONFLICT =
            "FOLLOW_RELATION_CONFLICT";

    private static final int MAX_IDENTIFIER_LENGTH = 32;

    private final UserFollowDirection direction;
    private final UserFollowState state;

    private UserFollow(UserFollowDirection direction, UserFollowState state) {
        this.direction = direction;
        this.state = state;
    }

    /**
     * C1：关注目标用户。
     *
     * <p>串行重复关注直接恢复原关系。预查未命中后只尝试一次插入，
     * 唯一键冲突与其他持久化异常均保持原样向上游传播。</p>
     */
    public static UserFollow follow(
            FollowUserCommand command,
            UserFollowIdGenerator idGenerator,
            UserFollowPersistence persistence) {
        require(command != null, FOLLOW_RELATION_CONFLICT, "关注命令不能为空");
        require(idGenerator != null,
                FOLLOW_IDENTITY_IMMUTABLE,
                "关注业务编号生成器不能为空");
        require(persistence != null,
                FOLLOW_RELATION_CONFLICT,
                "关注持久化端口不能为空");

        UserFollowDirection direction = requirePresentDirection(
                command.currentUserId(), command.targetUserId());
        require(command.targetExists(),
                FOLLOW_RELATION_CONFLICT,
                "目标用户不存在");

        UserFollowState existing = persistence.findByDirection(
                direction.followerId(), direction.followingId());
        if (existing != null) {
            UserFollow restored = restore(existing);
            restored.requireSameDirection(direction);
            return restored;
        }

        String bizId = idGenerator.nextBizId();
        validateBusinessId(bizId);
        UserFollow created = new UserFollow(
                direction, UserFollowState.newlyCreated(bizId, direction));
        created.checkInvariants();

        persistence.insert(created.state);
        created.checkInvariants();
        return created;
    }

    /**
     * C2：按方向物理删除关注关系，删除零行也是幂等成功。
     *
     * <p>解除时不查验目标用户，也特意允许两个编号相同，
     * 以便对不可能存在的自关注关系给出 ABSENT 结果。</p>
     */
    public static UserFollow unfollow(
            UnfollowUserCommand command, UserFollowPersistence persistence) {
        require(command != null, FOLLOW_RELATION_CONFLICT, "解除关注命令不能为空");
        require(persistence != null,
                FOLLOW_RELATION_CONFLICT,
                "关注持久化端口不能为空");
        UserFollowDirection direction = requireAbsentDirection(
                command.currentUserId(), command.targetUserId());

        persistence.deleteByDirection(
                direction.followerId(), direction.followingId());
        UserFollow absent = new UserFollow(direction, null);
        absent.checkInvariants();
        return absent;
    }

    /** 从数据库中的一条完整记录恢复 PRESENT 聚合。 */
    public static UserFollow restore(UserFollowState state) {
        require(state != null,
                FOLLOW_IDENTITY_IMMUTABLE,
                "关注状态不能为空");
        UserFollow restored = new UserFollow(state.direction(), state);
        restored.checkInvariants();
        return restored;
    }

    public UserFollowStatus status() {
        return state == null ? UserFollowStatus.ABSENT : UserFollowStatus.PRESENT;
    }

    public UserFollowDirection direction() {
        return direction;
    }

    /** ABSENT 时返回空，避免伪造一条不存在的表记录。 */
    public Optional<UserFollowState> state() {
        return Optional.ofNullable(state);
    }

    /** I1-I2：恢复及每个命令后校验当前状态涉及的全部不变量。 */
    private void checkInvariants() {
        requireNotBlank(direction.followerId(),
                FOLLOW_RELATION_CONFLICT,
                "关注人编号不能为空");
        requireNotBlank(direction.followingId(),
                FOLLOW_RELATION_CONFLICT,
                "被关注人编号不能为空");

        if (state == null) {
            return;
        }

        // I1：业务编号非空且固化；不可变状态没有方向或编号修改入口。
        validateBusinessId(state.bizId());
        require(Objects.equals(direction, state.direction()),
                FOLLOW_IDENTITY_IMMUTABLE,
                "关注方向与关注状态不一致");

        // I2：PRESENT 关系的两端必须非空且不同；方向唯一由 uk_follow_rel 最终保护。
        require(!Objects.equals(direction.followerId(), direction.followingId()),
                FOLLOW_RELATION_CONFLICT,
                "不能关注本人");
    }

    private void requireSameDirection(UserFollowDirection expected) {
        require(Objects.equals(direction, expected),
                FOLLOW_RELATION_CONFLICT,
                "重载到的关注方向与命令不一致");
    }

    private static UserFollowDirection requirePresentDirection(
            String followerId, String followingId) {
        requireNotBlank(followerId,
                FOLLOW_RELATION_CONFLICT,
                "关注人编号不能为空");
        requireNotBlank(followingId,
                FOLLOW_RELATION_CONFLICT,
                "被关注人编号不能为空");
        require(followerId.length() <= MAX_IDENTIFIER_LENGTH,
                FOLLOW_RELATION_CONFLICT,
                "关注人编号超出存储长度");
        require(followingId.length() <= MAX_IDENTIFIER_LENGTH,
                FOLLOW_RELATION_CONFLICT,
                "被关注人编号超出存储长度");
        require(!Objects.equals(followerId, followingId),
                FOLLOW_RELATION_CONFLICT,
                "不能关注本人");
        return new UserFollowDirection(followerId, followingId);
    }

    private static UserFollowDirection requireAbsentDirection(
            String followerId, String followingId) {
        requireNotBlank(followerId,
                FOLLOW_RELATION_CONFLICT,
                "关注人编号不能为空");
        requireNotBlank(followingId,
                FOLLOW_RELATION_CONFLICT,
                "被关注人编号不能为空");
        return new UserFollowDirection(followerId, followingId);
    }

    private static void validateBusinessId(String bizId) {
        requireNotBlank(bizId,
                FOLLOW_IDENTITY_IMMUTABLE,
                "关注业务编号不能为空");
        require(bizId.length() <= MAX_IDENTIFIER_LENGTH,
                FOLLOW_IDENTITY_IMMUTABLE,
                "关注业务编号超出存储长度");
        for (int index = 0; index < bizId.length(); index++) {
            char current = bizId.charAt(index);
            require(current >= '0' && current <= '9',
                    FOLLOW_IDENTITY_IMMUTABLE,
                    "关注业务编号必须是十进制雪花编号");
        }
    }

    private static void requireNotBlank(
            String value, String errorIdentifier, String message) {
        require(value != null && !value.trim().isEmpty(), errorIdentifier, message);
    }

    private static void require(
            boolean condition, String errorIdentifier, String message) {
        if (!condition) {
            throw error(errorIdentifier, message);
        }
    }

    private static UserFollowDomainException error(
            String errorIdentifier, String message) {
        return new UserFollowDomainException(
                Objects.requireNonNull(errorIdentifier), message);
    }
}
