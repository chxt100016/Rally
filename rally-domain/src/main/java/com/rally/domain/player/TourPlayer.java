package com.rally.domain.tour.player;

import java.util.Objects;

/**
 * 一名由 {@code tour+player_id} 标识的职业巡回赛球员资料聚合。
 *
 * <p>所有 {@code tour_player} 状态变更只能通过 C1 进入；排名、签表、赛程与比赛
 * 均不属于本聚合。</p>
 */
public final class TourPlayer {

    public static final String TOUR_PLAYER_IDENTITY_CONFLICT =
            "TOUR_PLAYER_IDENTITY_CONFLICT";
    public static final String TOUR_PLAYER_PROFILE_INVALID =
            "TOUR_PLAYER_PROFILE_INVALID";
    public static final String TOUR_PLAYER_RANKING_INVALID =
            "TOUR_PLAYER_RANKING_INVALID";

    private final TourPlayerIdentity identity;
    private TourPlayerState state;

    private TourPlayer(TourPlayerIdentity identity, TourPlayerState state) {
        this.identity = identity;
        this.state = state;
    }

    /**
     * C1：新增或刷新球员资料。已有球员只合并非空补丁；复合自然键竞争后
     * 重载胜出记录，再按同一命令合并。
     */
    public static TourPlayer saveOrRefresh(
            RefreshTourPlayerCommand command,
            TourPlayerPersistence persistence) {
        require(command != null,
                TOUR_PLAYER_PROFILE_INVALID,
                "刷新球员资料命令不能为空");
        require(persistence != null,
                TOUR_PLAYER_IDENTITY_CONFLICT,
                "球员持久化端口不能为空");

        TourPlayerIdentity requested = TourPlayerIdentity.fromSource(
                command.tour(), command.playerId());
        TourPlayerProfilePatch patch = TourPlayerProfilePatch.from(command);
        TourPlayerState existing = persistence.findByIdentity(requested);
        if (existing != null) {
            TourPlayer restored = restore(existing);
            restored.refresh(requested, patch, persistence);
            return restored;
        }

        TourPlayer created = new TourPlayer(
                requested,
                newState(requested, patch));
        created.checkInvariants();

        TourPlayerInsertResult result = persistence.insert(created.state);
        require(result != null && result.outcome() != null,
                TOUR_PLAYER_IDENTITY_CONFLICT,
                "球员插入没有返回有效结果");
        return switch (result.outcome()) {
            case CREATED -> {
                require(result.generatedId() != null && result.generatedId() > 0,
                        TOUR_PLAYER_IDENTITY_CONFLICT,
                        "球员插入未返回有效内部 id");
                created.state = created.state.withGeneratedId(result.generatedId());
                created.checkInvariants();
                yield created;
            }
            case IDENTITY_CONFLICT -> reloadAndRefresh(
                    requested, patch, persistence);
        };
    }

    /** 从一条完整表记录恢复 PROFILED 或 RANKED 聚合。 */
    public static TourPlayer restore(TourPlayerState state) {
        require(state != null,
                TOUR_PLAYER_IDENTITY_CONFLICT,
                "球员资料状态不能为空");
        TourPlayerIdentity identity = state.identity();
        TourPlayer restored = new TourPlayer(identity, state);
        restored.checkInvariants();
        restored.requirePersistentId();
        return restored;
    }

    public long id() {
        requirePersistentId();
        return state.id();
    }

    public TourPlayerIdentity identity() {
        return identity;
    }

    public TourPlayerState state() {
        return state;
    }

    public TourPlayerStatus status() {
        return state.rank() == null
                ? TourPlayerStatus.PROFILED
                : TourPlayerStatus.RANKED;
    }

    private void refresh(
            TourPlayerIdentity requested,
            TourPlayerProfilePatch patch,
            TourPlayerPersistence persistence) {
        require(Objects.equals(identity, requested),
                TOUR_PLAYER_IDENTITY_CONFLICT,
                "命令试图修改球员身份");

        TourPlayerState merged = state.merge(patch);
        TourPlayer candidate = new TourPlayer(identity, merged);
        candidate.checkInvariants();
        if (patch.isEmpty() || Objects.equals(state, merged)) {
            checkInvariants();
            return;
        }

        requirePersistentId();
        boolean updated = persistence.applyNonNullPatch(state.id(), patch);
        require(updated,
                TOUR_PLAYER_IDENTITY_CONFLICT,
                "刷新资料时球员已经不存在");
        state = merged;
        checkInvariants();
    }

    /** I1-I3：恢复及每次 C1 后校验当前状态涉及的全部不变量。 */
    private void checkInvariants() {
        // I1：状态必须始终承载建立时的规范复合自然键。
        require(Objects.equals(identity, state.identity()),
                TOUR_PLAYER_IDENTITY_CONFLICT,
                "球员身份建立后不可修改");
        require(state.id() == null || state.id() > 0,
                TOUR_PLAYER_IDENTITY_CONFLICT,
                "球员内部 id 必须为正数");

        // I2：表中必填姓名和全部可选资料必须保持规范格式。
        require(state.firstName() != null
                        && !state.firstName().isBlank()
                        && state.firstName().equals(state.firstName().strip())
                        && state.firstName().length() <= 50,
                TOUR_PLAYER_PROFILE_INVALID,
                "球员名不能为空且长度不能超过 50");
        require(state.lastName() != null
                        && !state.lastName().isBlank()
                        && state.lastName().equals(state.lastName().strip())
                        && state.lastName().length() <= 50,
                TOUR_PLAYER_PROFILE_INVALID,
                "球员姓不能为空且长度不能超过 50");
        require(state.nationality() == null
                        || state.nationality().matches("[A-Z]{3}"),
                TOUR_PLAYER_PROFILE_INVALID,
                "国籍必须是三位大写代码");
        require(state.gender() == null
                        || "M".equals(state.gender())
                        || "F".equals(state.gender()),
                TOUR_PLAYER_PROFILE_INVALID,
                "性别不是受支持的枚举值");
        require(state.hand() == null
                        || "RIGHT".equals(state.hand())
                        || "LEFT".equals(state.hand())
                        || "UNKNOWN".equals(state.hand()),
                TOUR_PLAYER_PROFILE_INVALID,
                "持拍手不是受支持的枚举值");

        // I3：排名与积分格式在合并后的完整状态上再次校验。
        require(state.rank() == null || state.rank() > 0,
                TOUR_PLAYER_RANKING_INVALID,
                "排名必须为正数");
        require(state.points() == null || state.points() >= 0,
                TOUR_PLAYER_RANKING_INVALID,
                "积分不得为负数");
    }

    private static TourPlayerState newState(
            TourPlayerIdentity identity,
            TourPlayerProfilePatch patch) {
        return new TourPlayerState(
                null,
                identity.playerId(),
                identity.tourCode(),
                patch.firstName(),
                patch.lastName(),
                patch.nationality(),
                patch.birthDate(),
                patch.gender(),
                patch.rank(),
                patch.points(),
                patch.hand(),
                null,
                null);
    }

    private static TourPlayer reloadAndRefresh(
            TourPlayerIdentity requested,
            TourPlayerProfilePatch patch,
            TourPlayerPersistence persistence) {
        TourPlayerState concurrent = persistence.findByIdentity(requested);
        require(concurrent != null,
                TOUR_PLAYER_IDENTITY_CONFLICT,
                "自然键冲突后未能重载球员资料");
        TourPlayer restored = restore(concurrent);
        restored.refresh(requested, patch, persistence);
        return restored;
    }

    private void requirePersistentId() {
        require(state.id() != null && state.id() > 0,
                TOUR_PLAYER_IDENTITY_CONFLICT,
                "球员操作需要有效内部 id");
    }

    private static void require(
            boolean condition, String errorIdentifier, String message) {
        if (!condition) {
            throw new TourPlayerDomainException(
                    Objects.requireNonNull(errorIdentifier), message);
        }
    }
}
