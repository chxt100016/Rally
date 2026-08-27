package com.rally.domain.meetup.scorerecord;

import java.util.Objects;

/**
 * 一场约球中一盘比分的聚合根。
 *
 * <p>这里刻意保留既有比分业务的宽松语义：新增只预检同场盘号并比较双方主分，
 * 修正只在内存比较已读取版本，删除则直接生成物理删除指令。</p>
 */
public final class ScoreRecord {

    public static final String SCORE_SET_DUPLICATE = "SCORE_SET_DUPLICATE";
    public static final String INVALID_WIN_SIDE = "INVALID_WIN_SIDE";
    public static final String RECAP_SCORE_NOT_FOUND = "RECAP_SCORE_NOT_FOUND";
    public static final String SCORE_VERSION_MISMATCH = "SCORE_VERSION_MISMATCH";

    /* 保留早期生成代码的导出常量，聚合不再以这些标识拒绝宽松输入。 */
    @Deprecated public static final String SCORE_LINEUP_INVALID = "SCORE_LINEUP_INVALID";
    @Deprecated public static final String SCORE_PLAYER_SNAPSHOT_INVALID = "SCORE_PLAYER_SNAPSHOT_INVALID";
    @Deprecated public static final String SCORE_GAME_INVALID = "SCORE_GAME_INVALID";
    @Deprecated public static final String SCORE_TIEBREAK_INVALID = "SCORE_TIEBREAK_INVALID";
    @Deprecated public static final String SCORE_RECORD_NOT_ALLOWED = "SCORE_RECORD_NOT_ALLOWED";
    @Deprecated public static final String SCORE_RECORD_MISMATCH = "SCORE_RECORD_MISMATCH";

    private ScoreRecordState state;
    private ScoreRecordStatus status;

    private ScoreRecord(ScoreRecordState state) {
        this.state = Objects.requireNonNull(state, "state");
        this.status = ScoreRecordStatus.RECORDED;
    }

    /**
     * C1：新增一盘比分。
     *
     * <p>盘号可为任意整数；阵容、快照和计分形态均按请求原样保存。业务编号唯一性
     * 不做额外预检，并发唯一冲突由数据库约束自然传播。</p>
     */
    public static ScoreRecord record(
            CreateScoreRecordCommand command,
            ScoreRecordIdGenerator idGenerator,
            ScoreRecordUniqueness uniqueness) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(idGenerator, "idGenerator");
        Objects.requireNonNull(uniqueness, "uniqueness");

        ScoreRecordDraft draft = Objects.requireNonNull(command.draft(), "draft");
        if (uniqueness.meetupSetExists(command.meetupId(), draft.setNumber(), null)) {
            throw new ScoreRecordDomainException(
                    SCORE_SET_DUPLICATE,
                    "同一约球盘号已存在");
        }

        ScoreWinSide winner = inferWinner(draft.sideAScore(), draft.sideBScore());
        ScoreRecordState candidate = new ScoreRecordState(
                null,
                idGenerator.nextScoreRecordId(),
                command.meetupId(),
                draft.setNumber(),
                draft.setFormat(),
                draft.matchType(),
                draft.meetupDate(),
                draft.lineup(),
                draft.sideAScore(),
                draft.sideBScore(),
                draft.sideATiebreakScore(),
                draft.sideBTiebreakScore(),
                winner,
                draft.recordedBy(),
                0,
                null,
                null);
        return new ScoreRecord(candidate);
    }

    /** 从仓储已读取的一条记录恢复聚合；读取本身不重新解释或收紧历史数据。 */
    public static ScoreRecord restore(ScoreRecordState state) {
        return new ScoreRecord(state);
    }

    /**
     * C2：先在内存比较读取版本，再形成不带版本条件、版本不递增的非空更新载荷。
     *
     * <p>{@code uniqueness} 仅为保留早期生成签名；main 的修正流程不做盘号唯一性预检。
     * 返回载荷中的 {@code expectedVersion} 只记录本次内存比较值，基础设施不得据此拼入
     * SQL 条件。</p>
     */
    public ScoreRecordUpdate correct(
            CorrectScoreRecordCommand command,
            ScoreRecordUniqueness uniqueness) {
        requireRecorded();
        Objects.requireNonNull(command, "command");

        if (!Objects.equals(state.meetupId(), command.meetupId())
                || !Objects.equals(state.businessId(), command.scoreRecordId())) {
            throw new ScoreRecordDomainException(
                    RECAP_SCORE_NOT_FOUND,
                    "比分记录不存在");
        }
        if (command.expectedVersion() != state.version()) {
            throw versionMismatch();
        }

        ScoreRecordDraft draft = Objects.requireNonNull(command.draft(), "draft");
        ScoreWinSide winner = inferWinner(draft.sideAScore(), draft.sideBScore());
        ScoreRecordState updatePayload = updatePayloadFromDraft(state, draft, winner);
        state = mergeNonNullFields(state, draft, winner);
        return new ScoreRecordUpdate(
                state.meetupId(),
                state.businessId(),
                command.expectedVersion(),
                updatePayload);
    }

    /**
     * C3：直接按命令中的 meetupId+scoreRecordId 形成物理删除指令。
     *
     * <p>不预读、不校验资格或归属，数据库删除命中零行也成功。</p>
     */
    public ScoreRecordRemoval remove(RemoveScoreRecordCommand command) {
        ScoreRecordRemoval removal = directRemoval(command);
        status = ScoreRecordStatus.REMOVED;
        return removal;
    }

    /** C3 的兼容入口。调用方不知道目标是否存在，仍必须执行 DELETE；零行由仓储接受。 */
    public static ScoreRecordRemoval removeMissing(RemoveScoreRecordCommand command) {
        return directRemoval(command);
    }

    public ScoreRecordState state() {
        return state;
    }

    public ScoreRecordStatus status() {
        return status;
    }

    /** 数据库唯一约束异常按系统错误自然传播，本兼容方法不再由命令流程调用。 */
    @Deprecated
    public static ScoreRecordDomainException setDuplicate(Throwable cause) {
        return new ScoreRecordDomainException(
                SCORE_SET_DUPLICATE,
                "比分业务编号或同场盘号已存在",
                cause);
    }

    /** 内存版本比较失败。实际 UPDATE 不得使用版本条件，也不得递增版本。 */
    public static ScoreRecordDomainException versionMismatch() {
        return new ScoreRecordDomainException(
                SCORE_VERSION_MISMATCH,
                "比分版本已变化");
    }

    /** 保留早期生成签名；实际更新影响行数不转换为版本冲突。 */
    @Deprecated
    public static ScoreRecordDomainException versionMismatch(Throwable cause) {
        return new ScoreRecordDomainException(
                SCORE_VERSION_MISMATCH,
                "比分版本已变化",
                cause);
    }

    private static ScoreRecordState mergeNonNullFields(
            ScoreRecordState current,
            ScoreRecordDraft draft,
            ScoreWinSide winner) {
        ScoreLineup incomingLineup = draft.lineup();
        ScoreLineup currentLineup = current.lineup();
        ScoreLineup mergedLineup = incomingLineup == null
                ? currentLineup
                : new ScoreLineup(
                        mergePlayer(playerAt(currentLineup, 0), incomingLineup.sideAPlayer1()),
                        mergePlayer(playerAt(currentLineup, 1), incomingLineup.sideAPlayer2()),
                        mergePlayer(playerAt(currentLineup, 2), incomingLineup.sideBPlayer1()),
                        mergePlayer(playerAt(currentLineup, 3), incomingLineup.sideBPlayer2()));

        return new ScoreRecordState(
                current.id(),
                current.businessId(),
                current.meetupId(),
                draft.setNumber(),
                nonNullOr(draft.setFormat(), current.setFormat()),
                nonNullOr(draft.matchType(), current.matchType()),
                nonNullOr(draft.meetupDate(), current.meetupDate()),
                mergedLineup,
                draft.sideAScore(),
                draft.sideBScore(),
                nonNullOr(draft.sideATiebreakScore(), current.sideATiebreakScore()),
                nonNullOr(draft.sideBTiebreakScore(), current.sideBTiebreakScore()),
                winner,
                nonNullOr(draft.recordedBy(), current.recordedBy()),
                current.version(),
                current.createTime(),
                current.updateTime());
    }

    /** 构造与 main 工厂相同的稀疏更新实体；null 字段交给默认更新策略跳过。 */
    private static ScoreRecordState updatePayloadFromDraft(
            ScoreRecordState current,
            ScoreRecordDraft draft,
            ScoreWinSide winner) {
        return new ScoreRecordState(
                null,
                current.businessId(),
                current.meetupId(),
                draft.setNumber(),
                draft.setFormat(),
                draft.matchType(),
                draft.meetupDate(),
                draft.lineup(),
                draft.sideAScore(),
                draft.sideBScore(),
                draft.sideATiebreakScore(),
                draft.sideBTiebreakScore(),
                winner,
                draft.recordedBy(),
                current.version(),
                null,
                null);
    }

    private static ScorePlayerSnapshot mergePlayer(
            ScorePlayerSnapshot current,
            ScorePlayerSnapshot incoming) {
        if (incoming == null) {
            return current;
        }
        return new ScorePlayerSnapshot(
                nonNullOr(incoming.userId(), current == null ? null : current.userId()),
                nonNullOr(incoming.nickname(), current == null ? null : current.nickname()),
                nonNullOr(incoming.avatarKey(), current == null ? null : current.avatarKey()),
                nonNullOr(incoming.gender(), current == null ? null : current.gender()));
    }

    private static ScorePlayerSnapshot playerAt(ScoreLineup lineup, int index) {
        if (lineup == null) {
            return null;
        }
        return switch (index) {
            case 0 -> lineup.sideAPlayer1();
            case 1 -> lineup.sideAPlayer2();
            case 2 -> lineup.sideBPlayer1();
            case 3 -> lineup.sideBPlayer2();
            default -> throw new IllegalArgumentException("unknown lineup slot");
        };
    }

    private static <T> T nonNullOr(T candidate, T fallback) {
        return candidate == null ? fallback : candidate;
    }

    private static ScoreWinSide inferWinner(Integer sideAScore, Integer sideBScore) {
        int a = Objects.requireNonNull(sideAScore, "sideAScore");
        int b = Objects.requireNonNull(sideBScore, "sideBScore");
        if (a == b) {
            throw new ScoreRecordDomainException(
                    INVALID_WIN_SIDE,
                    "比分不得相等");
        }
        return a > b ? ScoreWinSide.A : ScoreWinSide.B;
    }

    private static ScoreRecordRemoval directRemoval(RemoveScoreRecordCommand command) {
        Objects.requireNonNull(command, "command");
        return new ScoreRecordRemoval(
                command.meetupId(),
                command.scoreRecordId(),
                false);
    }

    private void requireRecorded() {
        if (status != ScoreRecordStatus.RECORDED) {
            throw new ScoreRecordDomainException(
                    RECAP_SCORE_NOT_FOUND,
                    "比分记录不存在");
        }
    }
}
