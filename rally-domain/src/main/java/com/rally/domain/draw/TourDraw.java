package com.rally.domain.tour.draw;

import java.util.Objects;

/**
 * 一个赛事年份下一种项目的签表聚合根。
 *
 * <p>{@code tour_draw} 的建立与结构刷新只能经 C1/C2 进入；比赛与报名不属于
 * 本聚合，因此它们后续保存失败不会补偿删除已经建立的签表。</p>
 */
public final class TourDraw {

    public static final String TOUR_DRAW_IDENTITY_CONFLICT =
            "TOUR_DRAW_IDENTITY_CONFLICT";
    public static final String TOUR_DRAW_STRUCTURE_INVALID =
            "TOUR_DRAW_STRUCTURE_INVALID";

    private final TourDrawIdentity identity;
    private TourDrawState state;

    private TourDraw(TourDrawIdentity identity, TourDrawState state) {
        this.identity = identity;
        this.state = state;
    }

    /**
     * C1：关联已有签表，或用来源当前可用字段建立签表。
     * 自然键并发插入失败后重载胜出记录，收敛为相同内部 id。
     */
    public static TourDraw associate(
            AssociateTourDrawCommand command, TourDrawPersistence persistence) {
        require(command != null,
                TOUR_DRAW_IDENTITY_CONFLICT,
                "关联签表命令不能为空");
        require(persistence != null,
                TOUR_DRAW_IDENTITY_CONFLICT,
                "签表持久化端口不能为空");
        require(command.tournamentCollected(),
                TOUR_DRAW_IDENTITY_CONFLICT,
                "来源赛事尚未收录");

        TourDrawIdentity requested = TourDrawIdentity.fromSource(
                command.tournamentId(), command.year(), command.sourceDrawType());
        TourDrawState existing = persistence.findByIdentity(requested);
        if (existing != null) {
            TourDraw restored = restore(existing);
            restored.requireSameIdentity(requested);
            restored.refreshStructure(new RefreshTourDrawStructureCommand(
                    command.tournamentId(),
                    command.year(),
                    command.sourceDrawType(),
                    command.size(),
                    command.totalRounds()), persistence);
            return restored;
        }

        TourDraw created = new TourDraw(
                requested,
                TourDrawState.initial(requested, command.size(), command.totalRounds()));
        created.checkInvariants();
        TourDrawInsertResult result = persistence.insert(created.state);
        require(result != null && result.outcome() != null,
                TOUR_DRAW_IDENTITY_CONFLICT,
                "签表插入没有返回有效结果");

        return switch (result.outcome()) {
            case CREATED -> {
                require(result.generatedId() != null && result.generatedId() > 0,
                        TOUR_DRAW_IDENTITY_CONFLICT,
                        "签表插入未返回有效内部 id");
                created.state = created.state.withGeneratedId(result.generatedId());
                created.checkInvariants();
                yield created;
            }
            case IDENTITY_CONFLICT -> {
                TourDraw concurrent = reloadConcurrent(requested, persistence);
                concurrent.refreshStructure(new RefreshTourDrawStructureCommand(
                        command.tournamentId(),
                        command.year(),
                        command.sourceDrawType(),
                        command.size(),
                        command.totalRounds()), persistence);
                yield concurrent;
            }
        };
    }

    /** 从一条完整表记录恢复聚合。 */
    public static TourDraw restore(TourDrawState state) {
        require(state != null,
                TOUR_DRAW_IDENTITY_CONFLICT,
                "签表状态不能为空");
        TourDrawIdentity identity = state.identity();
        TourDraw restored = new TourDraw(identity, state);
        restored.checkInvariants();
        restored.requirePersistentId();
        return restored;
    }

    /**
     * C2：分别刷新签表结构。每个非 null 值覆盖对应字段，
     * null 保留存量；两项都空时仅保持身份。
     */
    public void refreshStructure(
            RefreshTourDrawStructureCommand command,
            TourDrawPersistence persistence) {
        require(command != null,
                TOUR_DRAW_STRUCTURE_INVALID,
                "刷新签表结构命令不能为空");
        require(persistence != null,
                TOUR_DRAW_STRUCTURE_INVALID,
                "签表持久化端口不能为空");

        TourDrawIdentity requested = TourDrawIdentity.fromSource(
                command.tournamentId(), command.year(), command.sourceDrawType());
        requireSameIdentity(requested);

        Integer size = command.size();
        Integer totalRounds = command.totalRounds();
        if (size == null && totalRounds == null) {
            checkInvariants();
            return;
        }
        requirePersistentId();
        boolean updated = persistence.refreshStructure(state.id(), size, totalRounds);
        require(updated,
                TOUR_DRAW_IDENTITY_CONFLICT,
                "刷新结构时签表已不存在");
        state = state.withStructure(new TourDrawStructure(
                size != null ? size : state.size(),
                totalRounds != null ? totalRounds : state.totalRounds()));
        checkInvariants();
    }

    public long id() {
        requirePersistentId();
        return state.id();
    }

    public TourDrawIdentity identity() {
        return identity;
    }

    public TourDrawState state() {
        return state;
    }

    public TourDrawStatus status() {
        if (state.size() == null && state.totalRounds() == null) {
            return TourDrawStatus.PLACEHOLDER;
        }
        if (state.size() == null
                || state.totalRounds() == null
                || state.size() == 0
                || state.totalRounds() == 0) {
            return TourDrawStatus.PARTIAL;
        }
        return TourDrawStatus.STRUCTURED;
    }

    /** I1-I3：恢复及每个命令后校验当前状态涉及的全部不变量。 */
    private void checkInvariants() {
        // I1/I2：状态必须始终承载建立时的原始自然键，无任何身份修改入口。
        TourDrawIdentity stateIdentity = state.identity();
        require(Objects.equals(identity, stateIdentity),
                TOUR_DRAW_IDENTITY_CONFLICT,
                "签表身份建立后不可修改");
        require(Objects.equals(state.drawType(), identity.drawTypeCode()),
                TOUR_DRAW_IDENTITY_CONFLICT,
                "持久化签表类型必须保留来源原始代码");
        require(state.id() == null || state.id() > 0,
                TOUR_DRAW_IDENTITY_CONFLICT,
                "签表内部 id 必须为正数");

        // I3：size/totalRounds 是来源原始的独立可空字段，不做数学关系验证。
    }

    private static TourDraw reloadConcurrent(
            TourDrawIdentity requested, TourDrawPersistence persistence) {
        TourDrawState concurrent = persistence.findByIdentity(requested);
        require(concurrent != null,
                TOUR_DRAW_IDENTITY_CONFLICT,
                "自然键冲突后未能重载签表");
        TourDraw restored = restore(concurrent);
        restored.requireSameIdentity(requested);
        return restored;
    }

    private void requireSameIdentity(TourDrawIdentity expected) {
        require(Objects.equals(identity, expected),
                TOUR_DRAW_IDENTITY_CONFLICT,
                "命令试图修改签表身份");
    }

    private void requirePersistentId() {
        require(state.id() != null && state.id() > 0,
                TOUR_DRAW_IDENTITY_CONFLICT,
                "签表操作需要有效内部 id");
    }

    private static void require(
            boolean condition, String errorIdentifier, String message) {
        if (!condition) {
            throw new TourDrawDomainException(
                    Objects.requireNonNull(errorIdentifier), message);
        }
    }
}
