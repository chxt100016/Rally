package com.rally.db.tournament.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.rally.db.tournament.entity.MatchParticipantPO;
import com.rally.db.tournament.entity.TournamentMatchPO;
import com.rally.db.tournament.service.MatchParticipantMybatisService;
import com.rally.db.tournament.service.TournamentMatchMybatisService;
import com.rally.domain.tournament.match.TournamentMatchCancellationTarget;
import com.rally.domain.tournament.match.TournamentMatchConfirmStatus;
import com.rally.domain.tournament.match.TournamentMatchInsertResult;
import com.rally.domain.tournament.match.TournamentMatchParticipant;
import com.rally.domain.tournament.match.TournamentMatchPersistence;
import com.rally.domain.tournament.match.TournamentMatchRejectPhase;
import com.rally.domain.tournament.match.TournamentMatchRound;
import com.rally.domain.tournament.match.TournamentMatchState;
import com.rally.domain.tournament.match.TournamentMatchStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 新赛事比赛聚合的 MyBatis 持久化适配器。
 */
@Component
@RequiredArgsConstructor
public class TournamentMatchPersistenceAdapter implements TournamentMatchPersistence {

    private final TournamentMatchMybatisService matchService;
    private final MatchParticipantMybatisService participantService;

    @Override
    public TournamentMatchState findByBizId(String bizId) {
        TournamentMatchPO po = matchService.lambdaQuery()
                .eq(TournamentMatchPO::getBizId, bizId)
                .one();
        return toState(po);
    }

    @Override
    public TournamentMatchCancellationTarget findLatestByTournamentIdAndMatchNoForUpdate(
            String tournamentId,
            int matchNo) {
        TournamentMatchPO match = matchService.lambdaQuery()
                .eq(TournamentMatchPO::getTournamentId, tournamentId)
                .eq(TournamentMatchPO::getMatchNo, matchNo)
                .last("FOR UPDATE")
                .one();
        if (match == null) {
            return null;
        }
        List<TournamentMatchParticipant> participants = participantService.lambdaQuery()
                .eq(MatchParticipantPO::getMatchId, match.getBizId())
                .last("FOR UPDATE")
                .list()
                .stream()
                .map(this::toParticipant)
                .toList();
        return new TournamentMatchCancellationTarget(toState(match), participants);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TournamentMatchInsertResult insert(
            TournamentMatchState state,
            List<TournamentMatchParticipant> participants) {
        TournamentMatchPO match = toMatchPo(state);
        try {
            matchService.save(match);
        } catch (DuplicateKeyException exception) {
            return TournamentMatchInsertResult.identityConflict();
        }
        if (!participants.isEmpty()) {
            participantService.saveBatch(participants.stream()
                    .map(this::toParticipantPo)
                    .toList());
        }
        return TournamentMatchInsertResult.created(match.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean replaceWithVersion(
            TournamentMatchState state,
            List<TournamentMatchParticipant> participants,
            int expectedVersion) {
        boolean updated = matchService.lambdaUpdate()
                .eq(TournamentMatchPO::getBizId, state.bizId())
                .eq(TournamentMatchPO::getVersion, expectedVersion)
                .update(toMatchPo(state));
        if (!updated) {
            return false;
        }
        participantService.lambdaUpdate()
                .eq(MatchParticipantPO::getMatchId, state.bizId())
                .remove();
        if (!participants.isEmpty()) {
            participantService.saveBatch(participants.stream()
                    .map(this::toParticipantPo)
                    .toList());
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUnsubmittedWithVersion(String bizId, int expectedVersion) {
        int removed = matchService.getBaseMapper().delete(Wrappers
                .<TournamentMatchPO>lambdaQuery()
                .eq(TournamentMatchPO::getBizId, bizId)
                .eq(TournamentMatchPO::getVersion, expectedVersion)
                .in(TournamentMatchPO::getStatus,
                        TournamentMatchStatus.MATCHED.name(),
                        TournamentMatchStatus.BOOKING.name()));
        if (removed != 1) {
            return false;
        }
        participantService.getBaseMapper().delete(Wrappers
                .<MatchParticipantPO>lambdaQuery()
                .eq(MatchParticipantPO::getMatchId, bizId));
        return true;
    }

    @Override
    public boolean terminateByAdminWithVersion(
            TournamentMatchState state,
            int expectedVersion) {
        if (state.status() != TournamentMatchStatus.REJECTED
                || state.version() != expectedVersion + 1) {
            return false;
        }
        return matchService.lambdaUpdate()
                .eq(TournamentMatchPO::getBizId, state.bizId())
                .eq(TournamentMatchPO::getVersion, expectedVersion)
                .notIn(TournamentMatchPO::getStatus,
                        TournamentMatchStatus.COMPLETED.name(),
                        TournamentMatchStatus.REJECTED.name())
                .set(TournamentMatchPO::getStatus,
                        TournamentMatchStatus.REJECTED.name())
                .set(TournamentMatchPO::getVersion, state.version())
                .update();
    }

    @Override
    @Deprecated
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteNotCompletedWithParticipants(String bizId) {
        int removed = matchService.getBaseMapper().delete(Wrappers
                .<TournamentMatchPO>lambdaQuery()
                .eq(TournamentMatchPO::getBizId, bizId)
                .ne(TournamentMatchPO::getStatus, TournamentMatchStatus.COMPLETED.name()));
        if (removed != 1) {
            return false;
        }
        participantService.getBaseMapper().delete(Wrappers
                .<MatchParticipantPO>lambdaQuery()
                .eq(MatchParticipantPO::getMatchId, bizId));
        return true;
    }

    private TournamentMatchState toState(TournamentMatchPO po) {
        if (po == null) {
            return null;
        }
        return new TournamentMatchState(
                po.getId(),
                po.getBizId(),
                po.getTournamentId(),
                po.getMatchNo(),
                enumValue(TournamentMatchRound.class, po.getRound()),
                po.getGroupSize(),
                po.getCourtBookerId(),
                po.getCourtBookerSelectedTime(),
                po.getScheduleSubmittedTime(),
                po.getMeetupId(),
                po.getWinnerEntryNo(),
                po.getSubmittedBy(),
                po.getSubmittedTime(),
                enumValue(TournamentMatchRejectPhase.class, po.getRejectPhase()),
                po.getRejectReasonCode(),
                po.getRejectedBy(),
                po.getRejectedTime(),
                po.getLastRebookBy(),
                po.getLastRebookReasonCode(),
                po.getLastRebookTime(),
                enumValue(TournamentMatchStatus.class, po.getStatus()),
                po.getMatchedTime(),
                po.getCompletedTime(),
                po.getVersion(),
                po.getCreateTime(),
                po.getUpdateTime());
    }

    private TournamentMatchParticipant toParticipant(MatchParticipantPO po) {
        return new TournamentMatchParticipant(
                po.getId(),
                po.getBizId(),
                po.getMatchId(),
                po.getTournamentId(),
                po.getUserId(),
                po.getEntryNo(),
                enumValue(TournamentMatchConfirmStatus.class, po.getConfirmStatus()),
                po.getConfirmTime(),
                enumValue(TournamentMatchConfirmStatus.class, po.getResultConfirmStatus()),
                po.getResultConfirmTime(),
                po.getCreateTime(),
                po.getUpdateTime());
    }

    private TournamentMatchPO toMatchPo(TournamentMatchState state) {
        TournamentMatchPO po = new TournamentMatchPO();
        po.setId(state.id());
        po.setBizId(state.bizId());
        po.setTournamentId(state.tournamentId());
        po.setMatchNo(state.matchNo());
        po.setRound(enumName(state.round()));
        po.setGroupSize(state.groupSize());
        po.setCourtBookerId(state.courtBookerId());
        po.setCourtBookerSelectedTime(state.courtBookerSelectedTime());
        po.setScheduleSubmittedTime(state.scheduleSubmittedTime());
        po.setMeetupId(state.meetupId());
        po.setWinnerEntryNo(state.winnerEntryNo());
        po.setSubmittedBy(state.submittedBy());
        po.setSubmittedTime(state.submittedTime());
        po.setRejectPhase(enumName(state.rejectPhase()));
        po.setRejectReasonCode(state.rejectReasonCode());
        po.setRejectedBy(state.rejectedBy());
        po.setRejectedTime(state.rejectedTime());
        po.setLastRebookBy(state.lastRebookBy());
        po.setLastRebookReasonCode(state.lastRebookReasonCode());
        po.setLastRebookTime(state.lastRebookTime());
        po.setStatus(enumName(state.status()));
        po.setMatchedTime(state.matchedTime());
        po.setCompletedTime(state.completedTime());
        po.setVersion(state.version());
        po.setCreateTime(state.createTime());
        po.setUpdateTime(state.updateTime());
        return po;
    }

    private MatchParticipantPO toParticipantPo(TournamentMatchParticipant participant) {
        MatchParticipantPO po = new MatchParticipantPO();
        po.setId(participant.id());
        po.setBizId(participant.bizId());
        po.setMatchId(participant.matchId());
        po.setTournamentId(participant.tournamentId());
        po.setUserId(participant.userId());
        po.setEntryNo(participant.entryNo());
        po.setConfirmStatus(enumName(participant.confirmStatus()));
        po.setConfirmTime(participant.confirmTime());
        po.setResultConfirmStatus(enumName(participant.resultConfirmStatus()));
        po.setResultConfirmTime(participant.resultConfirmTime());
        po.setCreateTime(participant.createTime());
        po.setUpdateTime(participant.updateTime());
        return po;
    }

    private static String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value) {
        return value == null ? null : Enum.valueOf(type, value);
    }
}
