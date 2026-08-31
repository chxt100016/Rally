package com.rally.domain.tournament.match;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 赛事单场比赛聚合。rally_tournament_match 与其全部参与者的变更只能通过 C1-C10 进入。
 */
public final class TournamentMatch {

    public static final String TOURNAMENT_MATCH_IDENTITY_CONFLICT =
            "TOURNAMENT_MATCH_IDENTITY_CONFLICT";
    public static final String TOURNAMENT_MATCH_PARTICIPANT_INVALID =
            "TOURNAMENT_MATCH_PARTICIPANT_INVALID";
    public static final String TOURNAMENT_MATCH_BOOKING_INVALID =
            "TOURNAMENT_MATCH_BOOKING_INVALID";
    public static final String TOURNAMENT_MATCH_CONFIRMATION_INVALID =
            "TOURNAMENT_MATCH_CONFIRMATION_INVALID";
    public static final String TOURNAMENT_RESULT_WINNER_REQUIRED =
            "TOURNAMENT_RESULT_WINNER_REQUIRED";
    public static final String TOURNAMENT_MATCH_VERSION_CONFLICT =
            "TOURNAMENT_MATCH_VERSION_CONFLICT";
    public static final String MEETUP_EXPIRED = "MEETUP_EXPIRED";
    public static final String TOURNAMENT_MATCH_REJECTION_FORBIDDEN =
            "TOURNAMENT_MATCH_REJECTION_FORBIDDEN";
    public static final String TOURNAMENT_MATCH_NOT_FOUND =
            "TOURNAMENT_MATCH_NOT_FOUND";
    public static final String TOURNAMENT_MATCH_CANCEL_FORBIDDEN =
            "TOURNAMENT_MATCH_CANCEL_FORBIDDEN";

    private TournamentMatchState state;
    private List<TournamentMatchParticipant> participants;

    private TournamentMatch(
            TournamentMatchState state,
            List<TournamentMatchParticipant> participants) {
        this.state = state;
        this.participants = List.copyOf(participants);
    }

    /** C1：原子创建比赛根和全部参与者。 */
    public static TournamentMatch create(
            CreateTournamentMatchCommand command,
            TournamentMatchIdGenerator idGenerator,
            TournamentMatchPersistence persistence) {
        require(command != null, TOURNAMENT_MATCH_IDENTITY_CONFLICT,
                "创建比赛命令不能为空");
        require(idGenerator != null, TOURNAMENT_MATCH_IDENTITY_CONFLICT,
                "比赛 id 生成器不能为空");
        require(persistence != null, TOURNAMENT_MATCH_IDENTITY_CONFLICT,
                "比赛持久化端口不能为空");
        require(command.participants() != null,
                TOURNAMENT_MATCH_PARTICIPANT_INVALID,
                "比赛参与者不能为空");

        String bizId = requiredId(idGenerator.nextId(), "比赛业务 id");
        String tournamentId = requiredId(command.tournamentId(), "赛事 id");
        LocalDateTime matchedTime = requireTime(
                command.matchedTime(), "匹配时间不能为空");
        List<TournamentMatchParticipant> participants = new ArrayList<>();
        for (CreateTournamentMatchParticipant source : command.participants()) {
            require(source != null, TOURNAMENT_MATCH_PARTICIPANT_INVALID,
                    "比赛参与者不能为空");
            participants.add(new TournamentMatchParticipant(
                    null, requiredId(idGenerator.nextId(), "参与者业务 id"), bizId,
                    tournamentId, requiredId(source.userId(), "参与者用户 id"),
                    source.entryNo(), TournamentMatchConfirmStatus.PENDING, null,
                    TournamentMatchConfirmStatus.PENDING, null, null, null));
        }

        String bookerId = optionalId(command.uniqueCourtBookerId(), "订场人用户 id");
        TournamentMatchStatus initialStatus = bookerId == null
                ? TournamentMatchStatus.MATCHED : TournamentMatchStatus.BOOKING;
        TournamentMatchState state = new TournamentMatchState(
                null, bizId, tournamentId, command.matchNo(), command.round(),
                command.groupSize(), bookerId, bookerId == null ? null : matchedTime,
                null, null, null, null, null, null, null, null, null,
                null, null, null, initialStatus, matchedTime, null, 0, null, null);
        TournamentMatch created = new TournamentMatch(state, participants);
        created.checkInvariants();

        TournamentMatchInsertResult result = persistence.insert(
                created.state, created.participants);
        require(result != null && result.outcome() != null,
                TOURNAMENT_MATCH_IDENTITY_CONFLICT,
                "比赛插入没有返回有效结果");
        require(result.outcome() == TournamentMatchInsertResult.Outcome.CREATED,
                TOURNAMENT_MATCH_IDENTITY_CONFLICT,
                "比赛业务 id 或赛事内编号发生冲突");
        require(result.generatedId() != null && result.generatedId() > 0,
                TOURNAMENT_MATCH_IDENTITY_CONFLICT,
                "比赛插入未返回有效内部 id");
        created.state = created.state.withGeneratedId(result.generatedId());
        created.checkInvariants();
        return created;
    }

    /** 从比赛根及其全部参与者恢复聚合。 */
    public static TournamentMatch restore(
            TournamentMatchState state,
            List<TournamentMatchParticipant> participants) {
        require(state != null, TOURNAMENT_MATCH_IDENTITY_CONFLICT,
                "比赛根状态不能为空");
        require(participants != null, TOURNAMENT_MATCH_PARTICIPANT_INVALID,
                "比赛参与者不能为空");
        TournamentMatch restored = new TournamentMatch(state, participants);
        restored.requirePersistentId();
        restored.checkInvariants();
        return restored;
    }

    /** C2：MATCHED 参与者认领订场职责。 */
    public void selectCourtBooker(
            String userId,
            LocalDateTime selectedTime,
            int version,
            TournamentMatchPersistence persistence) {
        require(state.status() == TournamentMatchStatus.MATCHED,
                TOURNAMENT_MATCH_BOOKING_INVALID,
                "当前比赛不能认领订场人");
        String normalizedUserId = requiredId(userId, "订场人用户 id");
        participant(normalizedUserId);
        save(state.bookBy(normalizedUserId,
                        requireTime(selectedTime, "订场人确定时间不能为空")),
                participants, version, persistence);
    }

    /**
     * C3：BOOKING 首次或重订提交时推进聚合；SCHEDULED 编辑只校验身份和赛约，聚合不写且不比较 version。
     */
    public TournamentScheduleSubmissionOutcome submitOrModifySchedule(
            String userId,
            String meetupId,
            LocalDateTime submittedTime,
            int version,
            TournamentMatchPersistence persistence) {
        require(state.status() == TournamentMatchStatus.BOOKING
                        || state.status() == TournamentMatchStatus.SCHEDULED,
                TOURNAMENT_MATCH_BOOKING_INVALID,
                "当前比赛不能提交或修改赛约");
        String normalizedUserId = requiredId(userId, "订场人用户 id");
        String normalizedMeetupId = requiredId(meetupId, "赛约 id");
        require(Objects.equals(state.courtBookerId(), normalizedUserId),
                TOURNAMENT_MATCH_BOOKING_INVALID,
                "只有当前订场人可以提交或修改赛约");

        if (state.status() == TournamentMatchStatus.SCHEDULED) {
            require(Objects.equals(state.meetupId(), normalizedMeetupId),
                    TOURNAMENT_MATCH_BOOKING_INVALID,
                    "修改的赛约与比赛关联赛约不一致");
            return TournamentScheduleSubmissionOutcome.EXTERNAL_MEETUP_ONLY;
        }

        LocalDateTime time = requireTime(submittedTime, "赛约提交时间不能为空");
        List<TournamentMatchParticipant> updated = mapParticipants(participant ->
                Objects.equals(participant.userId(), normalizedUserId)
                        ? participant.confirmSchedule(time)
                        : participant.resetScheduleConfirmation());
        save(state.schedule(normalizedMeetupId, time), updated, version, persistence);
        return TournamentScheduleSubmissionOutcome.AGGREGATE_ADVANCED;
    }

    /** C4：接受赛约；可读取的赛约开始时间严格早于确认时间时拒绝。 */
    public void confirmSchedule(
            String userId,
            LocalDateTime confirmedTime,
            int version,
            LocalDateTime meetupStartTime,
            TournamentMatchPersistence persistence) {
        require(state.status() == TournamentMatchStatus.SCHEDULED,
                TOURNAMENT_MATCH_CONFIRMATION_INVALID,
                "当前比赛不能确认赛约");
        LocalDateTime time = requireTime(confirmedTime, "赛约确认时间不能为空");
        require(meetupStartTime == null || !meetupStartTime.isBefore(time),
                MEETUP_EXPIRED, "赛约已过期");
        String normalizedUserId = requiredId(userId, "参与者用户 id");
        participant(normalizedUserId);

        List<TournamentMatchParticipant> updated = mapParticipants(participant ->
                Objects.equals(participant.userId(), normalizedUserId)
                        ? participant.confirmSchedule(time) : participant);
        boolean allConfirmed = updated.stream().allMatch(participant ->
                participant.confirmStatus() == TournamentMatchConfirmStatus.CONFIRMED);
        TournamentMatchState candidate = allConfirmed
                ? state.withStatus(TournamentMatchStatus.PENDING_PLAY)
                : state.withStatus(TournamentMatchStatus.SCHEDULED);
        save(candidate, updated, version, persistence);
    }

    /** C5：请求重订，保留订场人及原赛约，只重置确认集合并记录最近一次原因。 */
    public void requestRebook(
            String userId,
            String reasonCode,
            LocalDateTime requestedTime,
            int version,
            TournamentMatchPersistence persistence) {
        require(state.status() == TournamentMatchStatus.SCHEDULED,
                TOURNAMENT_MATCH_BOOKING_INVALID,
                "当前比赛不能请求重订");
        String normalizedUserId = requiredId(userId, "参与者用户 id");
        participant(normalizedUserId);
        String normalizedReason = requiredCode(reasonCode, "重订理由");
        LocalDateTime time = requireTime(requestedTime, "重订时间不能为空");
        List<TournamentMatchParticipant> updated = mapParticipants(
                TournamentMatchParticipant::resetScheduleConfirmation);
        save(state.rebook(normalizedUserId, normalizedReason, time),
                updated, version, persistence);
    }

    /**
     * C6：参与者提交合法胜方，提交人先行确认赛果，其余人重置为待确认。
     *
     * <p>PENDING_PLAY 首次提交或 PENDING_CONFIRM 下覆盖重提均可执行；无论原状态是哪个，
     * 均统一覆盖 winnerEntryNo、submittedBy/submittedTime，并把全部参与者（含此前已
     * CONFIRMED 的）重置为 PENDING，仅提交人本次 CONFIRMED。</p>
     */
    public void submitResult(
            String userId,
            int winnerEntryNo,
            LocalDateTime submittedTime,
            int version,
            TournamentMatchPersistence persistence) {
        require(state.status() == TournamentMatchStatus.PENDING_PLAY
                        || state.status() == TournamentMatchStatus.PENDING_CONFIRM,
                TOURNAMENT_RESULT_WINNER_REQUIRED,
                "当前比赛不能提交赛果");
        String normalizedUserId = requiredId(userId, "赛果提交人用户 id");
        participant(normalizedUserId);
        require(participants.stream().anyMatch(participant ->
                        participant.entryNo() == winnerEntryNo),
                TOURNAMENT_RESULT_WINNER_REQUIRED,
                "胜方报名编号不属于本场比赛");
        LocalDateTime time = requireTime(submittedTime, "赛果提交时间不能为空");
        List<TournamentMatchParticipant> updated = mapParticipants(participant ->
                Objects.equals(participant.userId(), normalizedUserId)
                        ? participant.confirmResult(time)
                        : participant.resetResultConfirmation());
        save(state.submitResult(winnerEntryNo, normalizedUserId, time),
                updated, version, persistence);
    }

    /** C7：参与者确认赛果；最后一人确认时完成比赛。 */
    public void confirmResult(
            String userId,
            LocalDateTime confirmedTime,
            int version,
            TournamentMatchPersistence persistence) {
        require(state.status() == TournamentMatchStatus.PENDING_CONFIRM,
                TOURNAMENT_MATCH_CONFIRMATION_INVALID,
                "当前比赛不能确认赛果");
        String normalizedUserId = requiredId(userId, "参与者用户 id");
        participant(normalizedUserId);
        LocalDateTime time = requireTime(confirmedTime, "赛果确认时间不能为空");
        List<TournamentMatchParticipant> updated = mapParticipants(participant ->
                Objects.equals(participant.userId(), normalizedUserId)
                        ? participant.confirmResult(time) : participant);
        boolean allConfirmed = updated.stream().allMatch(participant ->
                participant.resultConfirmStatus()
                        == TournamentMatchConfirmStatus.CONFIRMED);
        TournamentMatchState candidate;
        if (allConfirmed) {
            requireWinnerPresent();
            candidate = state.complete(time);
        } else {
            candidate = state.withStatus(TournamentMatchStatus.PENDING_CONFIRM);
        }
        save(candidate, updated, version, persistence);
    }

    /** C7：确认超时后补齐仍待确认的参与者并完成比赛。 */
    public void autoCompleteResult(
            boolean timeoutReached,
            LocalDateTime completedTime,
            int version,
            TournamentMatchPersistence persistence) {
        require(state.status() == TournamentMatchStatus.PENDING_CONFIRM
                        && timeoutReached,
                TOURNAMENT_MATCH_CONFIRMATION_INVALID,
                "赛果尚未达到自动完成条件");
        requireWinnerPresent();
        LocalDateTime time = requireTime(completedTime, "比赛完成时间不能为空");
        List<TournamentMatchParticipant> updated = mapParticipants(participant ->
                participant.resultConfirmStatus() == TournamentMatchConfirmStatus.PENDING
                        ? participant.confirmResult(time) : participant);
        save(state.complete(time), updated, version, persistence);
    }

    /** C8：按活动提供的已验证场景事实终止任意非终态比赛。 */
    public void reject(
            RejectTournamentMatchCommand command,
            TournamentMatchPersistence persistence) {
        require(command != null && command.source() != null,
                TOURNAMENT_MATCH_REJECTION_FORBIDDEN,
                "比赛终止事实不完整");
        require(!state.status().isTerminal() && command.eligible(),
                TOURNAMENT_MATCH_REJECTION_FORBIDDEN,
                "比赛状态或场景条件不允许终止");

        List<TournamentMatchParticipant> updated = participants;
        if (command.source() == TournamentMatchRejectionSource.USER) {
            validateUserRejection(command);
            TournamentMatchParticipant initiator = participant(command.rejectedBy());
            updated = mapParticipants(current -> {
                if (!Objects.equals(current.userId(), initiator.userId())) {
                    return current;
                }
                return command.phase() == TournamentMatchRejectPhase.RESULT_REJECT
                        ? current.rejectResult(command.rejectedTime())
                        : current.rejectSchedule(command.rejectedTime());
            });
        } else if (command.source() == TournamentMatchRejectionSource.TIMEOUT) {
            require(command.rejectedTime() != null,
                    TOURNAMENT_MATCH_REJECTION_FORBIDDEN,
                    "超时终止必须提供发生时间");
        }
        validateOptionalAudit(command);
        save(state.reject(command), updated, command.version(), persistence);
    }

    /** C9：仅对尚未提交赛约的比赛执行版本化物理删除。 */
    public void cancelUnsubmitted(
            int version,
            TournamentMatchPersistence persistence) {
        require(state.status() == TournamentMatchStatus.MATCHED
                        || state.status() == TournamentMatchStatus.BOOKING,
                TOURNAMENT_MATCH_BOOKING_INVALID,
                "只有未提交赛约的比赛可以取消");
        requireVersion(version);
        require(persistence != null, TOURNAMENT_MATCH_VERSION_CONFLICT,
                "比赛持久化端口不能为空");
        require(persistence.deleteUnsubmittedWithVersion(state.bizId(), version),
                TOURNAMENT_MATCH_VERSION_CONFLICT,
                "比赛删除条件或版本已变化");
    }

    /**
     * C10：按赛事内自然键锁定最新比赛，产生联动快照后版本化软终止。
     *
     * <p>这一运营终止能力允许参与者列表为空，以便处理历史异常数据；
     * 不改变常规聚合恢复仍必须满足完整对阵的约束。</p>
     */
    public static TournamentMatchCancellationSnapshot cancel(
            CancelTournamentMatchCommand command,
            TournamentMatchPersistence persistence) {
        require(command != null, TOURNAMENT_MATCH_IDENTITY_CONFLICT,
                "取消比赛命令不能为空");
        require(persistence != null, TOURNAMENT_MATCH_VERSION_CONFLICT,
                "比赛持久化端口不能为空");
        String tournamentId = requiredId(command.tournamentId(), "赛事 id");
        require(command.matchNo() > 0, TOURNAMENT_MATCH_IDENTITY_CONFLICT,
                "比赛编号必须为正数");

        TournamentMatchCancellationTarget target =
                persistence.findLatestByTournamentIdAndMatchNoForUpdate(
                        tournamentId, command.matchNo());
        require(target != null, TOURNAMENT_MATCH_NOT_FOUND,
                "指定比赛不存在");
        TournamentMatchState latest = target.state();
        require(latest != null
                        && Objects.equals(latest.tournamentId(), tournamentId)
                        && latest.matchNo() == command.matchNo(),
                TOURNAMENT_MATCH_IDENTITY_CONFLICT,
                "锁定的比赛与请求自然键不一致");
        require(latest.status() != null, TOURNAMENT_MATCH_IDENTITY_CONFLICT,
                "比赛状态不能为空");
        require(latest.status() != TournamentMatchStatus.COMPLETED,
                TOURNAMENT_MATCH_CANCEL_FORBIDDEN,
                "已完成比赛不能取消");

        TournamentMatchCancellationSnapshot snapshot = cancellationSnapshot(target);
        if (latest.status() == TournamentMatchStatus.REJECTED) {
            return snapshot;
        }
        require(latest.version() >= 0, TOURNAMENT_MATCH_IDENTITY_CONFLICT,
                "比赛版本非法");
        TournamentMatchState terminated = latest.terminateByAdmin();
        require(persistence.terminateByAdminWithVersion(
                        terminated, latest.version()),
                TOURNAMENT_MATCH_VERSION_CONFLICT,
                "比赛状态或版本已变化");
        return snapshot;
    }

    public TournamentMatchState state() {
        return state;
    }

    public List<TournamentMatchParticipant> participants() {
        return participants;
    }

    private static TournamentMatchCancellationSnapshot cancellationSnapshot(
            TournamentMatchCancellationTarget target) {
        TournamentMatchState latest = target.state();
        String matchId = requiredId(latest.bizId(), "比赛业务 id");
        String tournamentId = requiredId(latest.tournamentId(), "赛事 id");
        String meetupId = optionalId(latest.meetupId(), "赛约 id");
        List<TournamentMatchCancellationParticipant> participantSnapshots =
                target.participants().stream().map(participant -> {
                    require(participant != null,
                            TOURNAMENT_MATCH_PARTICIPANT_INVALID,
                            "比赛参与者不能为空");
                    require(Objects.equals(participant.matchId(), matchId)
                                    && Objects.equals(
                                    participant.tournamentId(), tournamentId)
                                    && participant.entryNo() > 0,
                            TOURNAMENT_MATCH_PARTICIPANT_INVALID,
                            "参与者比赛身份不一致");
                    return new TournamentMatchCancellationParticipant(
                            requiredId(participant.userId(), "参与者用户 id"),
                            participant.entryNo());
                }).toList();
        return new TournamentMatchCancellationSnapshot(
                tournamentId, matchId, latest.matchNo(), meetupId,
                participantSnapshots);
    }

    private void validateUserRejection(RejectTournamentMatchCommand command) {
        require(command.phase() != null
                        && command.reasonCode() != null
                        && command.rejectedBy() != null
                        && command.rejectedTime() != null,
                TOURNAMENT_MATCH_REJECTION_FORBIDDEN,
                "人工拒绝必须提供阶段、理由、人员和时间");
        boolean schedulePhase = command.phase()
                == TournamentMatchRejectPhase.SCHEDULE_REJECT
                && (state.status() == TournamentMatchStatus.MATCHED
                || state.status() == TournamentMatchStatus.BOOKING
                || state.status() == TournamentMatchStatus.SCHEDULED);
        boolean resultPhase = command.phase()
                == TournamentMatchRejectPhase.RESULT_REJECT
                && state.status() == TournamentMatchStatus.PENDING_CONFIRM;
        require(schedulePhase || resultPhase,
                TOURNAMENT_MATCH_REJECTION_FORBIDDEN,
                "人工拒绝阶段与比赛状态不匹配");
    }

    private void validateOptionalAudit(RejectTournamentMatchCommand command) {
        optionalId(command.rejectedBy(), "拒绝人用户 id");
        optionalCode(command.reasonCode(), "拒绝理由");
    }

    /** I1-I8：恢复及每个命令后校验涉及的全部不变量。 */
    private void checkInvariants() {
        require(state != null, TOURNAMENT_MATCH_IDENTITY_CONFLICT,
                "比赛根状态不能为空");
        requiredId(state.bizId(), "比赛业务 id");
        requiredId(state.tournamentId(), "赛事 id");
        require(state.id() == null || state.id() > 0,
                TOURNAMENT_MATCH_IDENTITY_CONFLICT,
                "比赛内部 id 必须为正数");
        require(state.matchNo() > 0 && state.round() != null,
                TOURNAMENT_MATCH_IDENTITY_CONFLICT,
                "比赛编号必须为正数且轮次受支持");
        require(state.version() >= 0 && state.matchedTime() != null,
                TOURNAMENT_MATCH_IDENTITY_CONFLICT,
                "比赛版本或匹配时间非法");
        require(state.groupSize() == 2 || state.groupSize() == 3,
                TOURNAMENT_MATCH_PARTICIPANT_INVALID,
                "比赛对阵单元数只接受 2 或 3");

        Set<String> users = new HashSet<>();
        Set<Integer> entryNos = new HashSet<>();
        for (TournamentMatchParticipant participant : participants) {
            validateParticipant(participant);
            require(users.add(participant.userId()),
                    TOURNAMENT_MATCH_PARTICIPANT_INVALID,
                    "同一比赛内参与者用户 id 重复");
            entryNos.add(participant.entryNo());
        }
        require(entryNos.size() == state.groupSize(),
                TOURNAMENT_MATCH_PARTICIPANT_INVALID,
                "参与者报名编号数量与对阵单元数不一致");
        if (state.winnerEntryNo() != null) {
            require(entryNos.contains(state.winnerEntryNo()),
                    TOURNAMENT_MATCH_PARTICIPANT_INVALID,
                    "胜方报名编号不属于本场比赛");
        }

        boolean requiresBooker = state.status() == TournamentMatchStatus.BOOKING
                || state.status() == TournamentMatchStatus.SCHEDULED
                || state.status() == TournamentMatchStatus.PENDING_PLAY
                || state.status() == TournamentMatchStatus.PENDING_CONFIRM
                || state.status() == TournamentMatchStatus.COMPLETED;
        if (requiresBooker) {
            require(state.courtBookerId() != null
                            && state.courtBookerSelectedTime() != null
                            && users.contains(state.courtBookerId()),
                    TOURNAMENT_MATCH_BOOKING_INVALID,
                    "订场阶段必须同时记录合法订场人和选定时间");
        }
        boolean requiresSchedule = state.status() == TournamentMatchStatus.SCHEDULED
                || state.status() == TournamentMatchStatus.PENDING_PLAY
                || state.status() == TournamentMatchStatus.PENDING_CONFIRM
                || state.status() == TournamentMatchStatus.COMPLETED;
        if (requiresSchedule) {
            require(state.meetupId() != null
                            && state.scheduleSubmittedTime() != null,
                    TOURNAMENT_MATCH_BOOKING_INVALID,
                    "已提交阶段必须同时记录赛约和提交时间");
        }
        if (state.status() == TournamentMatchStatus.PENDING_CONFIRM
                || state.status() == TournamentMatchStatus.COMPLETED) {
            require(state.winnerEntryNo() != null
                            && state.submittedBy() != null
                            && users.contains(state.submittedBy())
                            && state.submittedTime() != null,
                    TOURNAMENT_RESULT_WINNER_REQUIRED,
                    "待确认或完成比赛必须有合法赛果提交事实");
        }
        if (state.status() == TournamentMatchStatus.COMPLETED) {
            require(state.completedTime() != null,
                    TOURNAMENT_RESULT_WINNER_REQUIRED,
                    "完成比赛必须记录完成时间");
        }
    }

    private void validateParticipant(TournamentMatchParticipant participant) {
        require(participant != null,
                TOURNAMENT_MATCH_PARTICIPANT_INVALID,
                "比赛参与者不能为空");
        requiredId(participant.bizId(), "参与者业务 id");
        require(Objects.equals(state.bizId(), participant.matchId())
                        && Objects.equals(state.tournamentId(), participant.tournamentId())
                        && participant.entryNo() > 0,
                TOURNAMENT_MATCH_PARTICIPANT_INVALID,
                "参与者比赛身份不一致");
        requiredId(participant.userId(), "参与者用户 id");
        validateConfirmation(participant.confirmStatus(), participant.confirmTime());
        validateConfirmation(participant.resultConfirmStatus(),
                participant.resultConfirmTime());
    }

    private void validateConfirmation(
            TournamentMatchConfirmStatus status,
            LocalDateTime time) {
        require(status != null,
                TOURNAMENT_MATCH_CONFIRMATION_INVALID,
                "确认状态不能为空");
        require(status == TournamentMatchConfirmStatus.PENDING || time != null,
                TOURNAMENT_MATCH_CONFIRMATION_INVALID,
                "非待确认状态必须记录确认时间");
    }

    private TournamentMatchParticipant participant(String userId) {
        return participants.stream()
                .filter(participant -> Objects.equals(participant.userId(), userId))
                .findFirst()
                .orElseThrow(() -> new TournamentMatchDomainException(
                        TOURNAMENT_MATCH_PARTICIPANT_INVALID,
                        "用户不是本场比赛参与者"));
    }

    private List<TournamentMatchParticipant> mapParticipants(
            java.util.function.UnaryOperator<TournamentMatchParticipant> mapper) {
        return participants.stream().map(mapper).toList();
    }

    private void save(
            TournamentMatchState candidateState,
            List<TournamentMatchParticipant> candidateParticipants,
            int expectedVersion,
            TournamentMatchPersistence persistence) {
        requireVersion(expectedVersion);
        require(persistence != null, TOURNAMENT_MATCH_VERSION_CONFLICT,
                "比赛持久化端口不能为空");
        TournamentMatch candidate = new TournamentMatch(
                candidateState, candidateParticipants);
        candidate.requirePersistentId();
        candidate.checkInvariants();
        require(persistence.replaceWithVersion(
                        candidate.state, candidate.participants, expectedVersion),
                TOURNAMENT_MATCH_VERSION_CONFLICT,
                "比赛版本已变化");
        state = candidate.state;
        participants = candidate.participants;
    }

    private void requireVersion(int expectedVersion) {
        require(expectedVersion == state.version(),
                TOURNAMENT_MATCH_VERSION_CONFLICT,
                "命令版本与已加载比赛版本不一致");
    }

    private void requireWinnerPresent() {
        require(state.winnerEntryNo() != null
                        && participants.stream().anyMatch(participant ->
                        participant.entryNo() == state.winnerEntryNo()),
                TOURNAMENT_RESULT_WINNER_REQUIRED,
                "完成比赛前必须有合法胜方");
    }

    private void requirePersistentId() {
        require(state.id() != null && state.id() > 0,
                TOURNAMENT_MATCH_IDENTITY_CONFLICT,
                "已保存比赛必须有有效内部 id");
    }

    private static String requiredId(String value, String fieldName) {
        require(value != null && !value.isBlank(),
                TOURNAMENT_MATCH_IDENTITY_CONFLICT,
                fieldName + "不能为空");
        String normalized = value.strip();
        require(normalized.length() <= 32,
                TOURNAMENT_MATCH_IDENTITY_CONFLICT,
                fieldName + "长度不能超过 32");
        return normalized;
    }

    private static String optionalId(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return requiredId(value, fieldName);
    }

    private static String requiredCode(String value, String fieldName) {
        String normalized = optionalCode(value, fieldName);
        require(normalized != null, TOURNAMENT_MATCH_CONFIRMATION_INVALID,
                fieldName + "不能为空");
        return normalized;
    }

    private static String optionalCode(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        require(normalized.length() <= 32,
                TOURNAMENT_MATCH_CONFIRMATION_INVALID,
                fieldName + "长度不能超过 32");
        return normalized;
    }

    private static LocalDateTime requireTime(LocalDateTime value, String message) {
        require(value != null, TOURNAMENT_MATCH_CONFIRMATION_INVALID, message);
        return value;
    }

    private static void require(
            boolean condition,
            String errorIdentifier,
            String message) {
        if (!condition) {
            throw new TournamentMatchDomainException(errorIdentifier, message);
        }
    }
}
