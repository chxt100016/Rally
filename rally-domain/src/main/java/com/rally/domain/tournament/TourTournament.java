package com.rally.domain.tour.tournament;

import java.util.Objects;

/**
 * 由 {@code tournament_id+year} 标识的职业赛事年度聚合根。
 *
 * <p>{@code tour_tournament} 的全部状态变更只能通过 C1/C2 进入。名录采集与
 * 图片内容生产是两种独立更新意图，任何命令都不能擦除另一方维护的字段。</p>
 */
public final class TourTournament {

    public static final String TOUR_TOURNAMENT_IDENTITY_CONFLICT =
            "TOUR_TOURNAMENT_IDENTITY_CONFLICT";
    public static final String TOUR_TOURNAMENT_PROFILE_INVALID =
            "TOUR_TOURNAMENT_PROFILE_INVALID";
    public static final String TOUR_TOURNAMENT_IMAGE_BINDING_INVALID =
            "TOUR_TOURNAMENT_IMAGE_BINDING_INVALID";

    private final TourTournamentIdentity identity;
    private TourTournamentState state;

    private TourTournament(TourTournamentIdentity identity, TourTournamentState state) {
        this.identity = identity;
        this.state = state;
    }

    /**
     * C1：新增或刷新一份完整来源主档。已有记录只替换名录字段；自然键并发
     * 插入失败后重载胜出记录，再按相同命令刷新，图片绑定始终保留。
     */
    public static TourTournament saveOrRefresh(
            RefreshTourTournamentCommand command,
            TourTournamentPersistence persistence) {
        require(command != null,
                TOUR_TOURNAMENT_PROFILE_INVALID,
                "刷新赛事名录命令不能为空");
        require(persistence != null,
                TOUR_TOURNAMENT_IDENTITY_CONFLICT,
                "赛事持久化端口不能为空");

        TourTournamentIdentity requested = TourTournamentIdentity.fromSource(
                command.tournamentId(), command.year());
        TourTournamentProfile profile = TourTournamentProfile.from(command);
        TourTournamentState existing = persistence.findByIdentity(requested);
        if (existing != null) {
            TourTournament restored = restore(existing);
            restored.refreshProfile(requested, profile, persistence);
            return restored;
        }

        TourTournament created = new TourTournament(
                requested,
                newState(requested, profile));
        created.checkInvariants();
        TourTournamentInsertResult result = persistence.insert(created.state);
        require(result != null && result.outcome() != null,
                TOUR_TOURNAMENT_IDENTITY_CONFLICT,
                "赛事插入没有返回有效结果");

        return switch (result.outcome()) {
            case CREATED -> {
                require(result.generatedId() != null && result.generatedId() > 0,
                        TOUR_TOURNAMENT_IDENTITY_CONFLICT,
                        "赛事插入未返回有效内部 id");
                created.state = created.state.withGeneratedId(result.generatedId());
                created.checkInvariants();
                yield created;
            }
            case IDENTITY_CONFLICT -> reloadAndRefresh(requested, profile, persistence);
        };
    }

    /** 从一条完整表记录恢复职业赛事年度聚合。 */
    public static TourTournament restore(TourTournamentState state) {
        require(state != null,
                TOUR_TOURNAMENT_IDENTITY_CONFLICT,
                "赛事年度状态不能为空");
        TourTournamentIdentity identity = state.identity();
        TourTournament restored = new TourTournament(identity, state);
        restored.checkInvariants();
        restored.requirePersistentId();
        return restored;
    }

    /**
     * C2：成对替换图片资源键。调用方找不到聚合时直接空操作；已加载聚合上的
     * 重复绑定幂等成功，其他情况只原子写两个图片列。
     */
    public void replaceImageBinding(
            ReplaceTourTournamentImagesCommand command,
            TourTournamentPersistence persistence) {
        require(persistence != null,
                TOUR_TOURNAMENT_IMAGE_BINDING_INVALID,
                "赛事持久化端口不能为空");
        TourTournamentImageBinding replacement =
                TourTournamentImageBinding.replacement(command);
        TourTournamentState candidate = state.withImageBinding(replacement);
        TourTournament checked = new TourTournament(identity, candidate);
        checked.checkInvariants();
        if (Objects.equals(state.imageBinding(), replacement)) {
            checkInvariants();
            return;
        }

        requirePersistentId();
        boolean updated = persistence.replaceImageBinding(state.id(), replacement);
        require(updated,
                TOUR_TOURNAMENT_IDENTITY_CONFLICT,
                "替换图片时赛事年度已经不存在");
        state = candidate;
        checkInvariants();
    }

    public long id() {
        requirePersistentId();
        return state.id();
    }

    public TourTournamentIdentity identity() {
        return identity;
    }

    public TourTournamentState state() {
        return state;
    }

    public TourTournamentStatus status() {
        return TourTournamentStatus.fromSource(state.status());
    }

    private void refreshProfile(
            TourTournamentIdentity requested,
            TourTournamentProfile profile,
            TourTournamentPersistence persistence) {
        require(Objects.equals(identity, requested),
                TOUR_TOURNAMENT_IDENTITY_CONFLICT,
                "命令试图修改赛事年度身份");
        TourTournamentState replacement = state.withProfile(profile);
        TourTournament checked = new TourTournament(identity, replacement);
        checked.checkInvariants();
        if (Objects.equals(state.profile(), profile)) {
            checkInvariants();
            return;
        }

        requirePersistentId();
        boolean updated = persistence.replaceCatalogProfile(state.id(), profile);
        require(updated,
                TOUR_TOURNAMENT_IDENTITY_CONFLICT,
                "刷新名录时赛事年度已经不存在");
        state = replacement;
        checkInvariants();
    }

    /** I1-I3：恢复以及每次 C1/C2 后校验当前状态涉及的全部不变量。 */
    private void checkInvariants() {
        // I1：状态只能承载建立时的 tournament_id+year，tour 不参与身份。
        require(Objects.equals(identity, state.identity()),
                TOUR_TOURNAMENT_IDENTITY_CONFLICT,
                "赛事年度身份建立后不可修改");
        require(state.id() == null || state.id() > 0,
                TOUR_TOURNAMENT_IDENTITY_CONFLICT,
                "赛事内部 id 必须为正数");

        // I2：从完整状态重建值对象，统一校验必填、日期、奖金及列容量。
        TourTournamentProfile checkedProfile = state.profile();
        require(Objects.equals(state.name(), checkedProfile.name())
                        && Objects.equals(state.tour(), checkedProfile.tour())
                        && Objects.equals(state.category(), checkedProfile.category())
                        && Objects.equals(state.surface(), checkedProfile.surface())
                        && Objects.equals(state.city(), checkedProfile.city())
                        && Objects.equals(state.country(), checkedProfile.country())
                        && Objects.equals(state.prizeMoneyText(), checkedProfile.prizeMoneyText())
                        && Objects.equals(state.status(), checkedProfile.status().databaseValue()),
                TOUR_TOURNAMENT_PROFILE_INVALID,
                "赛事主档必须使用规范格式");

        // I3：恢复时也验证图片为双空或双非空，命令只能成对替换。
        TourTournamentImageBinding checkedBinding = state.imageBinding();
        require(Objects.equals(state.imagePath(), checkedBinding.imagePath())
                        && Objects.equals(state.backgroundPath(), checkedBinding.backgroundPath()),
                TOUR_TOURNAMENT_IMAGE_BINDING_INVALID,
                "赛事图片绑定必须使用规范格式");
    }

    private static TourTournamentState newState(
            TourTournamentIdentity identity,
            TourTournamentProfile profile) {
        TourTournamentImageBinding emptyBinding = TourTournamentImageBinding.empty();
        return new TourTournamentState(
                null,
                identity.tournamentId(),
                identity.year(),
                profile.name(),
                profile.tour(),
                profile.category(),
                profile.surface(),
                profile.city(),
                profile.country(),
                profile.prizeMoney(),
                profile.prizeMoneyText(),
                profile.status().databaseValue(),
                profile.startDate(),
                profile.endDate(),
                emptyBinding.imagePath(),
                emptyBinding.backgroundPath(),
                null,
                null);
    }

    private static TourTournament reloadAndRefresh(
            TourTournamentIdentity requested,
            TourTournamentProfile profile,
            TourTournamentPersistence persistence) {
        TourTournamentState concurrent = persistence.findByIdentity(requested);
        require(concurrent != null,
                TOUR_TOURNAMENT_IDENTITY_CONFLICT,
                "自然键冲突后未能重载赛事年度");
        TourTournament restored = restore(concurrent);
        restored.refreshProfile(requested, profile, persistence);
        return restored;
    }

    private void requirePersistentId() {
        require(state.id() != null && state.id() > 0,
                TOUR_TOURNAMENT_IDENTITY_CONFLICT,
                "赛事操作需要有效内部 id");
    }

    private static void require(
            boolean condition, String errorIdentifier, String message) {
        if (!condition) {
            throw new TourTournamentDomainException(
                    Objects.requireNonNull(errorIdentifier), message);
        }
    }
}
