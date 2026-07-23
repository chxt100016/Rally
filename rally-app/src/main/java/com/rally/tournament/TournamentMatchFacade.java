package com.rally.tournament;

import com.rally.domain.tournament.enums.TournamentEntryStageEnum;
import com.rally.domain.tournament.enums.TournamentRoundEnum;
import com.rally.domain.tournament.gateway.TournamentEntryRepository;
import com.rally.domain.tournament.gateway.TournamentRepository;
import com.rally.domain.tournament.model.MatchGroup;
import com.rally.domain.tournament.model.TournamentData;
import com.rally.domain.tournament.model.TournamentEntryData;
import com.rally.domain.tournament.service.RejectHistoryLookup;
import com.rally.domain.tournament.service.TournamentMatchAssembleService;
import com.rally.domain.tournament.service.TournamentMatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 每日凌晨批量匹配编排：资格赛 + 正赛逐轮匹配
 */
@Service
@RequiredArgsConstructor
public class TournamentMatchFacade {

    private final TournamentRepository tournamentRepository;
    private final TournamentEntryRepository tournamentEntryRepository;
    private final TournamentMatchingService tournamentMatchingService;
    private final TournamentMatchAssembleService tournamentMatchAssembleService;

    /**
     * 扫描所有已激活且资格赛已开始的赛事，逐个跑资格赛+正赛匹配
     */
    public List<TournamentData> listTournamentsToMatch() {
        return tournamentRepository.findActiveWithQualifierStarted(LocalDateTime.now());
    }

    /**
     * 资格赛匹配：席位已满则跳过
     */
    @Transactional
    public void matchQualifier(String tournamentId) {
        TournamentData tournament = tournamentRepository.findByBizId(tournamentId);
        if (tournament == null || tournament.getCurrentFilledSlots() >= tournament.getTotalSlots()) {
            return;
        }
        List<TournamentEntryData> candidates = tournamentEntryRepository.findWaitingByTournamentAndStage(tournamentId, TournamentEntryStageEnum.QUALIFY, TournamentRoundEnum.QUALIFIER);
        doMatch(tournamentId, candidates, TournamentRoundEnum.QUALIFIER, tournament.getQualifierGroupSize());
    }

    /**
     * 正赛匹配：逐一处理当前存在排队候选人的轮次，每轮两两匹配
     */
    @Transactional
    public void matchMainRoundsAll(String tournamentId) {
        List<TournamentRoundEnum> rounds = tournamentEntryRepository.findDistinctWaitingRounds(tournamentId, TournamentEntryStageEnum.MAIN);
        for (TournamentRoundEnum round : rounds) {
            List<TournamentEntryData> candidates = tournamentEntryRepository.findWaitingByTournamentAndStage(tournamentId, TournamentEntryStageEnum.MAIN, round);
            doMatch(tournamentId, candidates, round, 2);
        }
    }

    private void doMatch(String tournamentId, List<TournamentEntryData> candidates, TournamentRoundEnum round, int groupSize) {
        if (candidates.size() < groupSize) {
            return;
        }
        RejectHistoryLookup rejectHistoryLookup = tournamentMatchAssembleService.buildRejectHistoryLookup(tournamentId);
        List<MatchGroup> groups = tournamentMatchingService.group(candidates, groupSize, rejectHistoryLookup);
        tournamentMatchAssembleService.assemble(tournamentId, groups, round, groupSize);
    }
}
