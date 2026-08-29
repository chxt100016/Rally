package com.rally.db.tournament.repository;

import com.alibaba.fastjson2.JSON;
import com.rally.db.tournament.entity.TournamentEntryPO;
import com.rally.db.tournament.service.TournamentEntryMybatisService;
import com.rally.domain.tournament.entry.TournamentEntryCourtAbility;
import com.rally.domain.tournament.entry.TournamentEntryInsertResult;
import com.rally.domain.tournament.entry.TournamentEntryPersistence;
import com.rally.domain.tournament.entry.TournamentEntryRound;
import com.rally.domain.tournament.entry.TournamentEntryStage;
import com.rally.domain.tournament.entry.TournamentEntryState;
import com.rally.domain.tournament.entry.TournamentEntryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.util.List;

/** rally_tournament_entry 聚合持久化适配器。 */
@Component
@RequiredArgsConstructor
public class TournamentEntryPersistenceAdapter implements TournamentEntryPersistence {

    private final TournamentEntryMybatisService tournamentEntryService;

    @Override
    public TournamentEntryState findByTournamentAndUser(
            String tournamentId,
            String userId) {
        TournamentEntryPO po = tournamentEntryService.lambdaQuery()
                .eq(TournamentEntryPO::getTournamentId, tournamentId)
                .eq(TournamentEntryPO::getUserId, userId)
                .one();
        return po == null ? null : toState(po);
    }

    @Override
    public TournamentEntryInsertResult insert(TournamentEntryState state) {
        TournamentEntryPO po = toPo(state);
        po.setId(null);
        try {
            if (!tournamentEntryService.save(po)) {
                return new TournamentEntryInsertResult(
                        TournamentEntryInsertResult.Outcome.IDENTITY_CONFLICT,
                        null);
            }
            return new TournamentEntryInsertResult(
                    TournamentEntryInsertResult.Outcome.CREATED,
                    po.getId());
        } catch (DuplicateKeyException exception) {
            return new TournamentEntryInsertResult(
                    TournamentEntryInsertResult.Outcome.IDENTITY_CONFLICT,
                    null);
        }
    }

    @Override
    public boolean saveByBizId(TournamentEntryState state) {
        return tournamentEntryService.lambdaUpdate()
                .eq(TournamentEntryPO::getBizId, state.bizId())
                .set(TournamentEntryPO::getPartnerId, state.partnerId())
                .set(TournamentEntryPO::getPreferredDistricts,
                        JSON.toJSONString(state.preferredDistricts()))
                .set(TournamentEntryPO::getCourtAbility, state.courtAbility().name())
                .set(TournamentEntryPO::getAvailableTimes,
                        JSON.toJSONString(state.availableTimes()))
                .set(TournamentEntryPO::getStage, state.stage().name())
                .set(TournamentEntryPO::getStatus, state.status().name())
                .set(TournamentEntryPO::getCurrentRound, state.currentRound().name())
                .set(TournamentEntryPO::getQualifierRejectCount,
                        state.qualifierRejectCount())
                .set(TournamentEntryPO::getMainDrawRejectCount,
                        state.mainDrawRejectCount())
                .set(TournamentEntryPO::getQualifiedTime, state.qualifiedTime())
                .set(TournamentEntryPO::getPaidTime, state.paidTime())
                .set(TournamentEntryPO::getLastVisitTime, state.lastVisitTime())
                .update();
    }

    @Override
    public boolean eliminateUnmatchedByBizId(
            TournamentEntryState state,
            TournamentEntryRound expectedCurrentRound) {
        return tournamentEntryService.lambdaUpdate()
                .eq(TournamentEntryPO::getBizId, state.bizId())
                .eq(TournamentEntryPO::getCurrentRound, expectedCurrentRound.name())
                .in(TournamentEntryPO::getStatus,
                        TournamentEntryStatus.WAITING.name(),
                        TournamentEntryStatus.FROZEN.name())
                .set(TournamentEntryPO::getStatus,
                        TournamentEntryStatus.ELIMINATED.name())
                .update();
    }

    private TournamentEntryState toState(TournamentEntryPO po) {
        return new TournamentEntryState(
                po.getId(),
                po.getBizId(),
                po.getTournamentId(),
                po.getUserId(),
                po.getPartnerId(),
                po.getEntryNo(),
                parseStringList(po.getPreferredDistricts()),
                TournamentEntryCourtAbility.valueOf(po.getCourtAbility()),
                parseStringList(po.getAvailableTimes()),
                TournamentEntryStage.valueOf(po.getStage()),
                TournamentEntryStatus.valueOf(po.getStatus()),
                TournamentEntryRound.valueOf(po.getCurrentRound()),
                po.getQualifierRejectCount(),
                po.getMainDrawRejectCount(),
                po.getQualifiedTime(),
                po.getPaidTime(),
                po.getLastVisitTime(),
                po.getCreateTime(),
                po.getUpdateTime());
    }

    private TournamentEntryPO toPo(TournamentEntryState state) {
        TournamentEntryPO po = new TournamentEntryPO();
        po.setId(state.id());
        po.setBizId(state.bizId());
        po.setTournamentId(state.tournamentId());
        po.setUserId(state.userId());
        po.setPartnerId(state.partnerId());
        po.setEntryNo(state.entryNo());
        po.setPreferredDistricts(JSON.toJSONString(state.preferredDistricts()));
        po.setCourtAbility(state.courtAbility().name());
        po.setAvailableTimes(JSON.toJSONString(state.availableTimes()));
        po.setStage(state.stage().name());
        po.setStatus(state.status().name());
        po.setCurrentRound(state.currentRound().name());
        po.setQualifierRejectCount(state.qualifierRejectCount());
        po.setMainDrawRejectCount(state.mainDrawRejectCount());
        po.setQualifiedTime(state.qualifiedTime());
        po.setPaidTime(state.paidTime());
        po.setLastVisitTime(state.lastVisitTime());
        po.setCreateTime(state.createTime());
        po.setUpdateTime(state.updateTime());
        return po;
    }

    private List<String> parseStringList(String json) {
        return JSON.parseArray(json, String.class);
    }
}
