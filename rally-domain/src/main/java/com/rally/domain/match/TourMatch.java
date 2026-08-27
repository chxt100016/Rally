package com.rally.domain.tour.match;

import java.util.Objects;

/**
 * 一场由 {@code draw_id+match_id} 标识的职业巡回赛比赛快照聚合。
 *
 * <p>所有 {@code tour_match} 变更只能通过 C1 进入；签表、赛事和球员
 * 都是聚合边界外引用。</p>
 */
public final class TourMatch {

    public static final String TOUR_MATCH_IDENTITY_CONFLICT =
            "TOUR_MATCH_IDENTITY_CONFLICT";
    public static final String TOUR_MATCH_SNAPSHOT_INVALID =
            "TOUR_MATCH_SNAPSHOT_INVALID";
    public static final String TOUR_MATCH_INDEX_CONFLICT =
            "TOUR_MATCH_INDEX_CONFLICT";

    private final TourMatchIdentity identity;
    private TourMatchState state;

    private TourMatch(TourMatchIdentity identity, TourMatchState state) {
        this.identity = identity;
        this.state = state;
    }

    /**
     * C1：新增或刷新比赛快照。同键时只合并非空补丁，
     * 新建的自然键竞争会重载胜出记录后再执行同一次合并。
     */
    public static TourMatch saveOrRefresh(
            RefreshTourMatchCommand command,
            TourMatchPersistence persistence) {
        require(command != null,
                TOUR_MATCH_IDENTITY_CONFLICT,
                "保存比赛快照命令不能为空");
        require(persistence != null,
                TOUR_MATCH_IDENTITY_CONFLICT,
                "比赛持久化端口不能为空");

        TourMatchIdentity requested = TourMatchIdentity.fromSource(
                command.drawId(), command.matchId());
        validateRequestedIndex(requested, command.matchIndex());
        TourMatchSnapshot patch = TourMatchSnapshot.from(command);

        TourMatchState existing = persistence.findByIdentity(requested);
        if (existing != null) {
            TourMatch restored = restore(existing);
            restored.refresh(command, patch, persistence);
            return restored;
        }

        String tournamentId = normalizeTournamentId(command.tournamentId(), true);
        int year = requireYear(command.year(), true);
        TourMatch created = new TourMatch(
                requested,
                initialState(requested, tournamentId, year, patch));
        created.checkInvariants();

        TourMatchInsertResult result = persistence.insert(created.state);
        require(result != null && result.outcome() != null,
                TOUR_MATCH_IDENTITY_CONFLICT,
                "比赛插入没有返回有效结果");
        return switch (result.outcome()) {
            case CREATED -> {
                require(result.generatedId() != null && result.generatedId() > 0,
                        TOUR_MATCH_IDENTITY_CONFLICT,
                        "比赛插入未返回有效内部 id");
                created.state = created.state.withGeneratedId(result.generatedId());
                created.checkInvariants();
                yield created;
            }
            case IDENTITY_CONFLICT -> reloadAndRefresh(
                    requested, command, patch, persistence);
        };
    }

    /** 从一条完整表记录恢复聚合。 */
    public static TourMatch restore(TourMatchState state) {
        require(state != null,
                TOUR_MATCH_IDENTITY_CONFLICT,
                "比赛快照状态不能为空");
        TourMatchIdentity identity = state.identity();
        TourMatch restored = new TourMatch(identity, normalizeRestoredStatus(state));
        restored.checkInvariants();
        restored.requirePersistentId();
        return restored;
    }

    public long id() {
        requirePersistentId();
        return state.id();
    }

    public TourMatchIdentity identity() {
        return identity;
    }

    public TourMatchState state() {
        return state;
    }

    public TourMatchStatus status() {
        return TourMatchStatus.restore(state.status());
    }

    private void refresh(
            RefreshTourMatchCommand command,
            TourMatchSnapshot patch,
            TourMatchPersistence persistence) {
        requireSameIdentity(TourMatchIdentity.fromSource(
                command.drawId(), command.matchId()));
        validateQueryIdentity(command.tournamentId(), command.year());
        validateRequestedIndex(identity, command.matchIndex());

        TourMatchState merged = merge(state, patch);
        TourMatch candidate = new TourMatch(identity, merged);
        candidate.checkInvariants();
        if (Objects.equals(state, merged)) {
            return;
        }
        requirePersistentId();
        boolean updated = persistence.replaceSnapshot(merged);
        require(updated,
                TOUR_MATCH_IDENTITY_CONFLICT,
                "刷新快照时比赛已不存在");
        state = merged;
        checkInvariants();
    }

    /** I1-I3：恢复及 C1 后校验涉及的全部不变量。 */
    private void checkInvariants() {
        require(Objects.equals(identity, state.identity()),
                TOUR_MATCH_IDENTITY_CONFLICT,
                "比赛自然键建立后不可修改");
        require(state.id() == null || state.id() > 0,
                TOUR_MATCH_IDENTITY_CONFLICT,
                "比赛内部 id 必须为正数");
        normalizeTournamentId(state.tournamentId(), true);
        requireYear(state.year(), true);

        Integer derivedIndex = identity.deriveMatchIndex();
        require(state.matchIndex() == null
                        || Objects.equals(state.matchIndex(), derivedIndex),
                TOUR_MATCH_INDEX_CONFLICT,
                "比赛序号必须由 match_id 的数字部分派生");
        validateStoredSnapshot(state);
    }

    private void validateQueryIdentity(String incomingTournamentId, Integer incomingYear) {
        String normalizedTournamentId = normalizeTournamentId(incomingTournamentId, false);
        if (normalizedTournamentId != null) {
            require(Objects.equals(state.tournamentId(), normalizedTournamentId),
                    TOUR_MATCH_IDENTITY_CONFLICT,
                    "同一比赛键的赛事编号冲突");
        }
        if (incomingYear != null) {
            require(incomingYear > 0 && incomingYear == state.year(),
                    TOUR_MATCH_IDENTITY_CONFLICT,
                    "同一比赛键的赛事年份冲突");
        }
    }

    private static TourMatchState initialState(
            TourMatchIdentity identity,
            String tournamentId,
            int year,
            TourMatchSnapshot patch) {
        return new TourMatchState(
                null,
                identity.matchId(),
                identity.deriveMatchIndex(),
                identity.drawId(),
                tournamentId,
                year,
                patch.roundNumber(),
                patch.roundName(),
                patch.player1Id(),
                patch.player2Id(),
                patch.winnerId(),
                patch.scheduledAt(),
                patch.scheduledAtText(),
                patch.startedAt(),
                patch.endedAt(),
                patch.court(),
                patch.courtSeq(),
                patch.status() == null
                        ? TourMatchStatus.UNKNOWN.name()
                        : patch.status().name(),
                patch.durationMinutes(),
                patch.description(),
                patch.matchDate(),
                patch.setsJson(),
                null,
                null);
    }

    private static TourMatchState merge(
            TourMatchState existing,
            TourMatchSnapshot patch) {
        return new TourMatchState(
                existing.id(),
                existing.matchId(),
                existing.matchIndex() == null
                        ? existing.identity().deriveMatchIndex()
                        : existing.matchIndex(),
                existing.drawId(),
                existing.tournamentId(),
                existing.year(),
                choose(patch.roundNumber(), existing.roundNumber()),
                choose(patch.roundName(), existing.roundName()),
                choose(patch.player1Id(), existing.player1Id()),
                choose(patch.player2Id(), existing.player2Id()),
                choose(patch.winnerId(), existing.winnerId()),
                choose(patch.scheduledAt(), existing.scheduledAt()),
                choose(patch.scheduledAtText(), existing.scheduledAtText()),
                choose(patch.startedAt(), existing.startedAt()),
                choose(patch.endedAt(), existing.endedAt()),
                choose(patch.court(), existing.court()),
                choose(patch.courtSeq(), existing.courtSeq()),
                patch.status() == null ? existing.status() : patch.status().name(),
                choose(patch.durationMinutes(), existing.durationMinutes()),
                choose(patch.description(), existing.description()),
                choose(patch.matchDate(), existing.matchDate()),
                choose(patch.setsJson(), existing.setsJson()),
                existing.createTime(),
                existing.updateTime());
    }

    private static TourMatch reloadAndRefresh(
            TourMatchIdentity requested,
            RefreshTourMatchCommand command,
            TourMatchSnapshot patch,
            TourMatchPersistence persistence) {
        TourMatchState concurrent = persistence.findByIdentity(requested);
        require(concurrent != null,
                TOUR_MATCH_IDENTITY_CONFLICT,
                "自然键冲突后未能重载比赛快照");
        TourMatch restored = restore(concurrent);
        restored.refresh(command, patch, persistence);
        return restored;
    }

    private static TourMatchState normalizeRestoredStatus(TourMatchState state) {
        String normalizedStatus = TourMatchStatus.restore(state.status()).name();
        if (Objects.equals(normalizedStatus, state.status())) {
            return state;
        }
        return new TourMatchState(
                state.id(), state.matchId(), state.matchIndex(), state.drawId(),
                state.tournamentId(), state.year(), state.roundNumber(), state.roundName(),
                state.player1Id(), state.player2Id(), state.winnerId(), state.scheduledAt(),
                state.scheduledAtText(), state.startedAt(), state.endedAt(), state.court(),
                state.courtSeq(), normalizedStatus, state.durationMinutes(), state.description(),
                state.matchDate(), state.setsJson(), state.createTime(), state.updateTime());
    }

    private static void validateRequestedIndex(
            TourMatchIdentity identity,
            Integer requestedIndex) {
        require(requestedIndex == null
                        || requestedIndex >= 0
                        && Objects.equals(requestedIndex, identity.deriveMatchIndex()),
                TOUR_MATCH_INDEX_CONFLICT,
                "命令中的比赛序号不是由 match_id 派生");
    }

    private static void validateStoredSnapshot(TourMatchState state) {
        RefreshTourMatchCommand stored = new RefreshTourMatchCommand(
                state.drawId(), state.matchId(), state.tournamentId(), state.year(),
                state.matchIndex(), state.roundNumber(), state.roundName(), state.player1Id(),
                state.player2Id(), state.winnerId(), state.scheduledAt(), state.scheduledAtText(),
                state.startedAt(), state.endedAt(), state.court(), state.courtSeq(),
                state.status(), state.durationMinutes(), state.description(), state.matchDate(),
                state.setsJson());
        TourMatchSnapshot.from(stored);
        require(state.status() != null
                        && Objects.equals(state.status(), TourMatchStatus.restore(state.status()).name()),
                TOUR_MATCH_SNAPSHOT_INVALID,
                "存储的比赛状态必须是规范状态");
    }

    private static String normalizeTournamentId(String value, boolean required) {
        if (value == null || value.isBlank()) {
            require(!required,
                    TOUR_MATCH_IDENTITY_CONFLICT,
                    "新建比赛必须提供赛事编号");
            return null;
        }
        String normalized = value.strip();
        require(normalized.length() <= 50,
                TOUR_MATCH_IDENTITY_CONFLICT,
                "赛事编号长度不能超过 50");
        return normalized;
    }

    private static int requireYear(Integer value, boolean required) {
        require(value != null || !required,
                TOUR_MATCH_IDENTITY_CONFLICT,
                "新建比赛必须提供赛事年份");
        if (value == null) {
            return 0;
        }
        require(value > 0,
                TOUR_MATCH_IDENTITY_CONFLICT,
                "赛事年份必须为正数");
        return value;
    }

    private void requireSameIdentity(TourMatchIdentity expected) {
        require(Objects.equals(identity, expected),
                TOUR_MATCH_IDENTITY_CONFLICT,
                "命令试图修改比赛自然键");
    }

    private void requirePersistentId() {
        require(state.id() != null && state.id() > 0,
                TOUR_MATCH_IDENTITY_CONFLICT,
                "比赛操作需要有效内部 id");
    }

    private static <T> T choose(T incoming, T existing) {
        return incoming == null ? existing : incoming;
    }

    private static void require(
            boolean condition,
            String errorIdentifier,
            String message) {
        if (!condition) {
            throw new TourMatchDomainException(
                    Objects.requireNonNull(errorIdentifier), message);
        }
    }
}
