package com.rally.domain.tour.tournamententry;

import java.util.Objects;

/**
 * 一个球员在一个职业签表中的参赛项聚合根。
 *
 * <p>资格采集只通过 C1 合并非空字段；来源遗漏不会删除记录或改变状态。
 * WITHDRAWN/RETIRED 只能通过带明确退出意图的 C2 产生。</p>
 */
public final class TourTournamentEntry {

    public static final String TOUR_ENTRY_IDENTITY_CONFLICT =
            "TOUR_ENTRY_IDENTITY_CONFLICT";
    public static final String TOUR_ENTRY_QUALIFICATION_INVALID =
            "TOUR_ENTRY_QUALIFICATION_INVALID";
    public static final String TOUR_ENTRY_STATUS_CONFLICT =
            "TOUR_ENTRY_STATUS_CONFLICT";

    private static final String DEFAULT_ENTRY_TYPE = "DIRECT";

    private final TourTournamentEntryIdentity identity;
    private TourTournamentEntryState state;

    private TourTournamentEntry(
            TourTournamentEntryIdentity identity,
            TourTournamentEntryState state) {
        this.identity = identity;
        this.state = state;
    }

    /**
     * C1：新增或刷新参赛资格。已有项按到达顺序合并非空字段；自然键竞争后
     * 重载胜出记录并应用同一补丁。
     */
    public static TourTournamentEntry saveOrRefresh(
            RefreshTourTournamentEntryCommand command,
            TourTournamentEntryPersistence persistence) {
        require(command != null,
                TOUR_ENTRY_QUALIFICATION_INVALID,
                "刷新参赛资格命令不能为空");
        require(persistence != null,
                TOUR_ENTRY_IDENTITY_CONFLICT,
                "参赛项持久化端口不能为空");

        TourTournamentEntryIdentity requested = TourTournamentEntryIdentity.of(
                command.drawId(), command.playerId());
        TourTournamentEntryQualificationPatch patch =
                TourTournamentEntryQualificationPatch.of(
                        command.seed(), command.entryType());

        TourTournamentEntryState existing = persistence.findByIdentity(requested);
        if (existing != null) {
            TourTournamentEntry restored = restore(existing);
            restored.refresh(requested, patch, persistence);
            return restored;
        }

        String initialEntryType = patch.entryType() == null
                ? DEFAULT_ENTRY_TYPE
                : patch.entryType();
        TourTournamentEntry created = new TourTournamentEntry(
                requested,
                new TourTournamentEntryState(
                        null,
                        requested.drawId(),
                        requested.playerId(),
                        patch.seed(),
                        initialEntryType,
                        TourTournamentEntryStatus.CONFIRMED,
                        null,
                        null));
        created.checkInvariants();

        TourTournamentEntryInsertResult result = persistence.insert(created.state);
        require(result != null && result.outcome() != null,
                TOUR_ENTRY_IDENTITY_CONFLICT,
                "参赛项插入没有返回有效结果");
        return switch (result.outcome()) {
            case CREATED -> {
                require(result.generatedId() != null && result.generatedId() > 0,
                        TOUR_ENTRY_IDENTITY_CONFLICT,
                        "参赛项插入未返回有效内部 id");
                created.state = created.state.withGeneratedId(result.generatedId());
                created.checkInvariants();
                yield created;
            }
            case IDENTITY_CONFLICT -> reloadAndRefresh(
                    requested, patch, persistence);
        };
    }

    /** 从一条完整表记录恢复参赛项。 */
    public static TourTournamentEntry restore(TourTournamentEntryState state) {
        require(state != null,
                TOUR_ENTRY_IDENTITY_CONFLICT,
                "参赛项状态不能为空");
        TourTournamentEntry restored = new TourTournamentEntry(
                state.identity(), state);
        restored.checkInvariants();
        restored.requirePersistentId();
        return restored;
    }

    /** C2：将 CONFIRMED 参赛项置为明确的 WITHDRAWN 或 RETIRED 终态。 */
    public void recordExit(
            RecordTourTournamentEntryExitCommand command,
            TourTournamentEntryPersistence persistence) {
        require(command != null,
                TOUR_ENTRY_STATUS_CONFLICT,
                "退出命令不能为空");
        require(persistence != null,
                TOUR_ENTRY_IDENTITY_CONFLICT,
                "参赛项持久化端口不能为空");
        require(command.explicitExitIntent(),
                TOUR_ENTRY_STATUS_CONFLICT,
                "缺少明确退出意图");
        TourTournamentEntryStatus targetStatus = command.targetStatus();
        require(targetStatus != null && targetStatus.isExited(),
                TOUR_ENTRY_STATUS_CONFLICT,
                "退出目标状态必须是 WITHDRAWN 或 RETIRED");

        if (state.status() == targetStatus) {
            checkInvariants();
            return;
        }
        require(state.status() == TourTournamentEntryStatus.CONFIRMED,
                TOUR_ENTRY_STATUS_CONFLICT,
                "已退出参赛项不能改为其他退出状态");

        requirePersistentId();
        boolean updated = persistence.updateStatus(state.id(), targetStatus);
        require(updated,
                TOUR_ENTRY_IDENTITY_CONFLICT,
                "记录退出时参赛项已经不存在");
        state = state.withStatus(targetStatus);
        checkInvariants();
    }

    public long id() {
        requirePersistentId();
        return state.id();
    }

    public TourTournamentEntryIdentity identity() {
        return identity;
    }

    public TourTournamentEntryState state() {
        return state;
    }

    public TourTournamentEntryStatus status() {
        return state.status();
    }

    private void refresh(
            TourTournamentEntryIdentity requested,
            TourTournamentEntryQualificationPatch patch,
            TourTournamentEntryPersistence persistence) {
        require(Objects.equals(identity, requested),
                TOUR_ENTRY_IDENTITY_CONFLICT,
                "命令试图修改参赛身份");
        require(state.status() == TourTournamentEntryStatus.CONFIRMED,
                TOUR_ENTRY_STATUS_CONFLICT,
                "已退出参赛项不能由采集补丁恢复");

        TourTournamentEntryState merged = state.merge(patch);
        TourTournamentEntry candidate = new TourTournamentEntry(identity, merged);
        candidate.checkInvariants();
        if (patch.isEmpty() || Objects.equals(state, merged)) {
            checkInvariants();
            return;
        }

        requirePersistentId();
        boolean updated = persistence.applyNonNullQualificationPatch(state.id(), patch);
        require(updated,
                TOUR_ENTRY_IDENTITY_CONFLICT,
                "刷新资格时参赛项已经不存在");
        state = merged;
        checkInvariants();
    }

    /** I1-I3：恢复以及每次 C1/C2 后校验当前状态涉及的全部不变量。 */
    private void checkInvariants() {
        // I1：状态必须始终承载建立时的复合自然键。
        require(Objects.equals(identity, state.identity()),
                TOUR_ENTRY_IDENTITY_CONFLICT,
                "参赛身份建立后不可修改");
        require(state.id() == null || state.id() > 0,
                TOUR_ENTRY_IDENTITY_CONFLICT,
                "参赛项内部 id 必须为正数");

        // I2：entryType 原样持有，只验证非空和 VARCHAR(10) 边界；seed 原值保留。
        TourTournamentEntryQualificationPatch.of(state.seed(), state.entryType());
        require(state.entryType() != null,
                TOUR_ENTRY_QUALIFICATION_INVALID,
                "持久化入围方式不能为空");

        // I3：状态只允许确认态和两个明确退出终态。
        require(state.status() != null,
                TOUR_ENTRY_STATUS_CONFLICT,
                "参赛状态不能为空");
    }

    private static TourTournamentEntry reloadAndRefresh(
            TourTournamentEntryIdentity requested,
            TourTournamentEntryQualificationPatch patch,
            TourTournamentEntryPersistence persistence) {
        TourTournamentEntryState concurrent = persistence.findByIdentity(requested);
        require(concurrent != null,
                TOUR_ENTRY_IDENTITY_CONFLICT,
                "自然键冲突后未能重载参赛项");
        TourTournamentEntry restored = restore(concurrent);
        restored.refresh(requested, patch, persistence);
        return restored;
    }

    private void requirePersistentId() {
        require(state.id() != null && state.id() > 0,
                TOUR_ENTRY_IDENTITY_CONFLICT,
                "参赛项操作需要有效内部 id");
    }

    private static void require(
            boolean condition, String errorIdentifier, String message) {
        if (!condition) {
            throw new TourTournamentEntryDomainException(
                    Objects.requireNonNull(errorIdentifier), message);
        }
    }
}
