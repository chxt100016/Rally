package com.rally.domain.tournament.entry;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 由 {@code biz_id} 标识、以 {@code tournament_id+user_id} 为自然键的赛事报名聚合。
 * rally_tournament_entry 的状态变化只能通过 C1-C11 进入。
 */
public final class TournamentEntry {

    public static final String TOURNAMENT_ENTRY_IDENTITY_CONFLICT =
            "TOURNAMENT_ENTRY_IDENTITY_CONFLICT";
    public static final String TOURNAMENT_PARTNER_ALREADY_PAIRED =
            "TOURNAMENT_PARTNER_ALREADY_PAIRED";
    public static final String TOURNAMENT_ENTRY_PREFERENCE_INVALID =
            "TOURNAMENT_ENTRY_PREFERENCE_INVALID";
    public static final String TOURNAMENT_ENTRY_PROGRESS_INVALID =
            "TOURNAMENT_ENTRY_PROGRESS_INVALID";
    public static final String TOURNAMENT_REJECT_LIMIT_REACHED =
            "TOURNAMENT_REJECT_LIMIT_REACHED";
    public static final String TOURNAMENT_ENTRY_STATUS_ILLEGAL =
            "TOURNAMENT_ENTRY_STATUS_ILLEGAL";
    public static final String TOURNAMENT_ENTRY_VERSION_CONFLICT =
            "TOURNAMENT_ENTRY_VERSION_CONFLICT";

    private final TournamentEntryIdentity identity;
    private TournamentEntryState state;

    private TournamentEntry(
            TournamentEntryIdentity identity,
            TournamentEntryState state) {
        this.identity = identity;
        this.state = state;
    }

    /** C1：创建一份资格赛报名。任意状态的旧报名均阻止重新报名。 */
    public static TournamentEntry register(
            CreateTournamentEntryCommand command,
            TournamentEntryIdGenerator idGenerator,
            TournamentEntryPersistence persistence) {
        require(command != null, TOURNAMENT_ENTRY_IDENTITY_CONFLICT,
                "创建报名命令不能为空");
        require(idGenerator != null, TOURNAMENT_ENTRY_IDENTITY_CONFLICT,
                "报名 id 生成器不能为空");
        require(persistence != null, TOURNAMENT_ENTRY_IDENTITY_CONFLICT,
                "报名持久化端口不能为空");
        require(!command.partnerAlreadyPairedWithOther(),
                TOURNAMENT_PARTNER_ALREADY_PAIRED,
                "搭档已经与其他报名组队");
        if (command.registeredPartnerEntryNo() != null) {
            require(command.registeredPartnerEntryNo() == command.entryNo(),
                    TOURNAMENT_PARTNER_ALREADY_PAIRED,
                    "双打搭档必须共享报名编号");
        }

        TournamentEntryIdentity identity = new TournamentEntryIdentity(
                idGenerator.nextId(), command.tournamentId(), command.userId(),
                command.entryNo());
        require(persistence.findByTournamentAndUser(
                        identity.tournamentId(), identity.userId()) == null,
                TOURNAMENT_ENTRY_IDENTITY_CONFLICT,
                "同一用户已在本赛事报名");
        String partnerId = normalizePartnerId(command.partnerId(), identity.userId());
        TournamentEntryPreferences preferences = new TournamentEntryPreferences(
                command.preferredDistricts(), command.courtAbility(),
                command.availableTimes());
        TournamentEntry created = new TournamentEntry(identity,
                new TournamentEntryState(
                        null, identity.bizId(), identity.tournamentId(), identity.userId(),
                        partnerId, identity.entryNo(),
                        preferences.preferredDistricts(), preferences.courtAbility(),
                        preferences.availableTimes(), TournamentEntryStage.QUALIFY,
                        TournamentEntryStatus.WAITING, TournamentEntryRound.QUALIFIER,
                        0, 0, null, null, null, null, null));
        created.checkInvariants();

        TournamentEntryInsertResult result = persistence.insert(created.state);
        require(result != null && result.outcome() != null,
                TOURNAMENT_ENTRY_IDENTITY_CONFLICT,
                "报名插入没有返回有效结果");
        require(result.outcome() == TournamentEntryInsertResult.Outcome.CREATED,
                TOURNAMENT_ENTRY_IDENTITY_CONFLICT,
                "报名业务 id 或赛事用户自然键发生冲突");
        require(result.generatedId() != null && result.generatedId() > 0,
                TOURNAMENT_ENTRY_IDENTITY_CONFLICT,
                "报名插入未返回有效内部 id");
        created.state = created.state.withGeneratedId(result.generatedId());
        created.checkInvariants();
        return created;
    }

    /** 从一条完整表记录恢复报名聚合。 */
    public static TournamentEntry restore(TournamentEntryState state) {
        require(state != null, TOURNAMENT_ENTRY_IDENTITY_CONFLICT,
                "报名状态不能为空");
        TournamentEntry restored = new TournamentEntry(state.identity(), state);
        restored.checkInvariants();
        restored.requirePersistentId();
        return restored;
    }

    /** C2：非终态报名整组替换三项匹配偏好。 */
    public void replacePreferences(
            TournamentEntryPreferences replacement,
            TournamentEntryPersistence persistence) {
        require(!state.status().isTerminal(), TOURNAMENT_ENTRY_STATUS_ILLEGAL,
                "终态报名不能修改匹配偏好");
        require(replacement != null, TOURNAMENT_ENTRY_PREFERENCE_INVALID,
                "匹配偏好不能为空");
        TournamentEntryState candidate = state.withPreferences(replacement);
        save(candidate, persistence);
    }

    /**
     * C1：新成员注册事务为已存在的搭档补齐反向关系。空值可以首次绑定，
     * 相同搭档幂等成功，任何换绑或 entryNo 不一致均拒绝。
     */
    public void bindPartnerDuringRegistration(
            String partnerUserId,
            int sharedEntryNo,
            TournamentEntryPersistence persistence) {
        require(partnerUserId != null && !partnerUserId.isBlank(),
                TOURNAMENT_PARTNER_ALREADY_PAIRED,
                "搭档用户 id 不能为空");
        String normalizedPartnerId = normalizePartnerId(
                partnerUserId, identity.userId());
        require(sharedEntryNo == identity.entryNo(),
                TOURNAMENT_PARTNER_ALREADY_PAIRED,
                "双打搭档必须共享报名编号");
        require(state.partnerId() == null
                        || Objects.equals(state.partnerId(), normalizedPartnerId),
                TOURNAMENT_PARTNER_ALREADY_PAIRED,
                "报名已经绑定其他搭档");
        if (Objects.equals(state.partnerId(), normalizedPartnerId)) {
            return;
        }
        save(state.withPartnerId(normalizedPartnerId), persistence);
    }

    /** C3：只接受 WAITING→FROZEN 或 FROZEN→WAITING 的精确迁移。 */
    public void changeFrozenState(
            TournamentEntryStatus target,
            TournamentEntryPersistence persistence) {
        boolean freezing = state.status() == TournamentEntryStatus.WAITING
                && target == TournamentEntryStatus.FROZEN;
        boolean unfreezing = state.status() == TournamentEntryStatus.FROZEN
                && target == TournamentEntryStatus.WAITING;
        require(freezing || unfreezing, TOURNAMENT_ENTRY_STATUS_ILLEGAL,
                "冻结或解冻的来源状态不精确");
        save(progress(state.stage(), target, state.currentRound()), persistence);
    }

    /** C4：当前轮次一致时把 WAITING 报名锁入一场比赛。 */
    public void lockIntoMatch(
            String matchId,
            TournamentEntryRound tournamentCurrentRound,
            TournamentEntryPersistence persistence) {
        require(matchId != null && !matchId.isBlank(),
                TOURNAMENT_ENTRY_STATUS_ILLEGAL,
                "锁入比赛必须提供比赛 id");
        require(state.status() == TournamentEntryStatus.WAITING,
                TOURNAMENT_ENTRY_STATUS_ILLEGAL,
                "只有等待报名可以锁入比赛");
        require(state.currentRound() == tournamentCurrentRound,
                TOURNAMENT_ENTRY_PROGRESS_INVALID,
                "报名轮次与赛事当前轮次不一致");
        save(progress(state.stage(), TournamentEntryStatus.IN_MATCH,
                state.currentRound()), persistence);
    }

    /**
     * C5：比赛拒绝、超时或未订场取消后释放回匹配池。重复结算只跳过非 IN_MATCH
     * 报名；需要计数时先检查本赛段限额再递增。
     *
     * @return 是否实际释放了报名
     */
    public boolean releaseToWaiting(
            TournamentEntryReleaseReason reason,
            boolean incrementRejectCount,
            int rejectLimit,
            TournamentEntryPersistence persistence) {
        require(reason != null, TOURNAMENT_ENTRY_STATUS_ILLEGAL,
                "释放比赛报名必须提供原因");
        if (state.status() != TournamentEntryStatus.IN_MATCH) {
            return false;
        }
        int qualifierCount = state.qualifierRejectCount();
        int mainCount = state.mainDrawRejectCount();
        if (incrementRejectCount) {
            require(rejectLimit >= 0, TOURNAMENT_REJECT_LIMIT_REACHED,
                    "拒绝次数上限不能为负数");
            int current = state.stage() == TournamentEntryStage.QUALIFY
                    ? qualifierCount : mainCount;
            require(current < rejectLimit, TOURNAMENT_REJECT_LIMIT_REACHED,
                    "本赛段拒绝次数已达上限");
            if (state.stage() == TournamentEntryStage.QUALIFY) {
                qualifierCount++;
            } else {
                mainCount++;
            }
        }
        save(state.withProgress(state.stage(), TournamentEntryStatus.WAITING,
                state.currentRound(), qualifierCount, mainCount, state.paidTime()),
                persistence);
        return true;
    }

    /** C6：按已确认的完成比赛事实推进；为兼容现状，不复核调用前必须 IN_MATCH。 */
    public void settleCompletedMatch(
            SettleTournamentEntryCommand command,
            TournamentEntryPersistence persistence) {
        require(command != null && command.outcome() != null
                        && command.completedRound() != null
                        && command.completedTime() != null,
                TOURNAMENT_ENTRY_PROGRESS_INVALID,
                "比赛结算事实不完整");

        TournamentEntryState candidate;
        if (state.stage() == TournamentEntryStage.QUALIFY) {
            require(command.completedRound() == TournamentEntryRound.QUALIFIER,
                    TOURNAMENT_ENTRY_PROGRESS_INVALID,
                    "资格赛报名不能按正赛轮次结算");
            TournamentEntryStatus status = command.outcome()
                    == SettleTournamentEntryCommand.Outcome.WIN
                    ? TournamentEntryStatus.PAYING
                    : TournamentEntryStatus.WAITING;
            candidate = progress(TournamentEntryStage.QUALIFY, status,
                    TournamentEntryRound.QUALIFIER);
        } else {
            require(command.completedRound().isMainDrawRound(),
                    TOURNAMENT_ENTRY_PROGRESS_INVALID,
                    "正赛报名不能按资格赛轮次结算");
            if (command.outcome() == SettleTournamentEntryCommand.Outcome.LOSS) {
                candidate = progress(TournamentEntryStage.MAIN,
                        TournamentEntryStatus.ELIMINATED, state.currentRound());
            } else if (command.completedRound() == TournamentEntryRound.FINAL) {
                candidate = progress(TournamentEntryStage.MAIN,
                        TournamentEntryStatus.CHAMPION, TournamentEntryRound.FINAL);
            } else {
                TournamentEntryRound next = command.completedRound().nextMainDrawRound();
                require(next != null, TOURNAMENT_ENTRY_PROGRESS_INVALID,
                        "比赛轮次无法推进");
                candidate = progress(TournamentEntryStage.MAIN,
                        TournamentEntryStatus.WAITING, next);
            }
        }
        save(candidate, persistence);
    }

    /** C7：资格赛待支付报名在有效支付后锁定正赛席位。 */
    public void lockMainDrawSeat(
            LocalDateTime paidTime,
            int totalSlots,
            boolean mainDrawSeatOccupied,
            TournamentEntryPersistence persistence) {
        require(state.stage() == TournamentEntryStage.QUALIFY
                        && state.status() == TournamentEntryStatus.PAYING,
                TOURNAMENT_ENTRY_STATUS_ILLEGAL,
                "只有资格赛待支付报名可以锁定正赛席位");
        require(paidTime != null, TOURNAMENT_ENTRY_STATUS_ILLEGAL,
                "锁定正赛席位必须提供支付时间");
        require(mainDrawSeatOccupied, TOURNAMENT_ENTRY_STATUS_ILLEGAL,
                "正赛席位尚未原子占用");
        TournamentEntryRound firstRound =
                TournamentEntryRound.firstMainDrawRound(totalSlots);
        save(state.withProgress(TournamentEntryStage.MAIN,
                TournamentEntryStatus.WAITING, firstRound,
                state.qualifierRejectCount(), state.mainDrawRejectCount(), paidTime),
                persistence);
    }

    /** C8：资格赛结束且正赛席位已满时淘汰仍等待的资格赛报名。 */
    public void eliminateUnqualified(
            boolean qualifierCompleted,
            boolean mainDrawFull,
            TournamentEntryPersistence persistence) {
        require(qualifierCompleted && mainDrawFull,
                TOURNAMENT_ENTRY_PROGRESS_INVALID,
                "资格赛尚未完成或正赛席位尚未占满");
        require(state.stage() == TournamentEntryStage.QUALIFY
                        && state.status() == TournamentEntryStatus.WAITING,
                TOURNAMENT_ENTRY_STATUS_ILLEGAL,
                "只有资格赛等待报名可以被淘汰");
        save(progress(TournamentEntryStage.QUALIFY,
                TournamentEntryStatus.ELIMINATED, TournamentEntryRound.QUALIFIER),
                persistence);
    }

    /** C9：任意非终态报名主动退赛，其他字段原样保留。 */
    public void withdraw(TournamentEntryPersistence persistence) {
        require(!state.status().isTerminal(), TOURNAMENT_ENTRY_STATUS_ILLEGAL,
                "终态报名不能再次退赛");
        save(progress(state.stage(), TournamentEntryStatus.WITHDRAWN,
                state.currentRound()), persistence);
    }

    /**
     * C10：详情读取链路直接覆盖访问时间；报名不存在时更新无效果。
     *
     * @return 是否找到并更新了报名
     */
    public static boolean recordVisit(
            String tournamentId,
            String userId,
            LocalDateTime visitedAt,
            TournamentEntryPersistence persistence) {
        require(visitedAt != null, TOURNAMENT_ENTRY_STATUS_ILLEGAL,
                "访问时间不能为空");
        require(persistence != null, TOURNAMENT_ENTRY_IDENTITY_CONFLICT,
                "报名持久化端口不能为空");
        TournamentEntryState stored = persistence.findByTournamentAndUser(
                tournamentId, userId);
        if (stored == null) {
            return false;
        }
        TournamentEntry entry = restore(stored);
        entry.save(entry.state.withLastVisitTime(visitedAt), persistence);
        return true;
    }

    /**
     * C11：运营淘汰当前轮次未入赛报名。持久化端必须同时限定
     * bizId、预期轮次与 WAITING/FROZEN 来源状态，防止覆盖并发入赛或轮次推进。
     */
    public void eliminateUnmatched(
            TournamentEntryRound expectedCurrentRound,
            TournamentEntryPersistence persistence) {
        require(expectedCurrentRound != null,
                TOURNAMENT_ENTRY_VERSION_CONFLICT,
                "运营淘汰必须提供赛事当前轮次");
        require(state.status() == TournamentEntryStatus.WAITING
                        || state.status() == TournamentEntryStatus.FROZEN,
                TOURNAMENT_ENTRY_VERSION_CONFLICT,
                "只有等待或冻结报名可被运营淘汰");
        require(state.currentRound() == expectedCurrentRound,
                TOURNAMENT_ENTRY_VERSION_CONFLICT,
                "报名轮次与赛事当前轮次不一致");
        require(persistence != null,
                TOURNAMENT_ENTRY_VERSION_CONFLICT,
                "报名持久化端口不能为空");

        TournamentEntryState candidate = progress(
                state.stage(), TournamentEntryStatus.ELIMINATED,
                state.currentRound());
        TournamentEntry checked = new TournamentEntry(identity, candidate);
        checked.checkInvariants();
        requirePersistentId();
        require(persistence.eliminateUnmatchedByBizId(
                        candidate, expectedCurrentRound),
                TOURNAMENT_ENTRY_VERSION_CONFLICT,
                "运营淘汰报名时状态或轮次已变更");
        state = candidate;
        checkInvariants();
    }

    public TournamentEntryIdentity identity() {
        return identity;
    }

    public TournamentEntryState state() {
        return state;
    }

    /** I1-I8：恢复及每条命令后校验当前状态涉及的全部不变量。 */
    private void checkInvariants() {
        require(Objects.equals(identity, state.identity()),
                TOURNAMENT_ENTRY_IDENTITY_CONFLICT,
                "报名身份、赛事用户自然键或报名编号被修改");
        require(state.id() == null || state.id() > 0,
                TOURNAMENT_ENTRY_IDENTITY_CONFLICT,
                "报名内部 id 必须为正数");
        normalizePartnerId(state.partnerId(), identity.userId());
        TournamentEntryPreferences checkedPreferences = state.preferences();
        require(Objects.equals(checkedPreferences.preferredDistricts(),
                        state.preferredDistricts())
                        && checkedPreferences.courtAbility() == state.courtAbility()
                        && Objects.equals(checkedPreferences.availableTimes(),
                        state.availableTimes()),
                TOURNAMENT_ENTRY_PREFERENCE_INVALID,
                "报名偏好必须使用规范格式");
        require(state.stage() != null && state.status() != null
                        && state.currentRound() != null,
                TOURNAMENT_ENTRY_PROGRESS_INVALID,
                "报名进度不能为空");
        require(state.stage() == TournamentEntryStage.QUALIFY
                        ? state.currentRound() == TournamentEntryRound.QUALIFIER
                        : state.currentRound().isMainDrawRound(),
                TOURNAMENT_ENTRY_PROGRESS_INVALID,
                "报名阶段与轮次不一致");
        require(state.status() != TournamentEntryStatus.PAYING
                        || state.stage() == TournamentEntryStage.QUALIFY,
                TOURNAMENT_ENTRY_PROGRESS_INVALID,
                "待支付报名必须仍处于资格赛阶段");
        require(state.status() != TournamentEntryStatus.CHAMPION
                        || state.stage() == TournamentEntryStage.MAIN
                        && state.currentRound() == TournamentEntryRound.FINAL,
                TOURNAMENT_ENTRY_PROGRESS_INVALID,
                "冠军必须是完成决赛的正赛报名");
        require(state.qualifierRejectCount() >= 0
                        && state.mainDrawRejectCount() >= 0,
                TOURNAMENT_ENTRY_PROGRESS_INVALID,
                "报名拒绝次数不能为负数");
    }

    private TournamentEntryState progress(
            TournamentEntryStage stage,
            TournamentEntryStatus status,
            TournamentEntryRound round) {
        return state.withProgress(stage, status, round,
                state.qualifierRejectCount(), state.mainDrawRejectCount(),
                state.paidTime());
    }

    private void save(
            TournamentEntryState candidate,
            TournamentEntryPersistence persistence) {
        require(persistence != null, TOURNAMENT_ENTRY_IDENTITY_CONFLICT,
                "报名持久化端口不能为空");
        TournamentEntry checked = new TournamentEntry(identity, candidate);
        checked.checkInvariants();
        requirePersistentId();
        require(persistence.saveByBizId(candidate),
                TOURNAMENT_ENTRY_IDENTITY_CONFLICT,
                "保存报名时记录已不存在");
        state = candidate;
        checkInvariants();
    }

    private void requirePersistentId() {
        require(state.id() != null && state.id() > 0,
                TOURNAMENT_ENTRY_IDENTITY_CONFLICT,
                "报名操作需要有效内部 id");
    }

    private static String normalizePartnerId(String partnerId, String userId) {
        if (partnerId == null || partnerId.isBlank()) {
            return null;
        }
        String normalized = partnerId.strip();
        require(normalized.length() <= 32,
                TOURNAMENT_PARTNER_ALREADY_PAIRED,
                "搭档用户 id 长度不能超过 32");
        require(!Objects.equals(normalized, userId),
                TOURNAMENT_PARTNER_ALREADY_PAIRED,
                "报名用户不能选择自己作为搭档");
        return normalized;
    }

    private static void require(
            boolean condition,
            String errorIdentifier,
            String message) {
        if (!condition) {
            throw new TournamentEntryDomainException(
                    Objects.requireNonNull(errorIdentifier), message);
        }
    }
}
