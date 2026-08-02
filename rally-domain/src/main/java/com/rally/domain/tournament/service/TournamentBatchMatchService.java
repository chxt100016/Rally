package com.rally.domain.tournament.service;

import com.rally.domain.tournament.enums.TournamentEntryStageEnum;
import com.rally.domain.tournament.enums.TournamentRoundEnum;
import com.rally.domain.tournament.gateway.TournamentEntryRepository;
import com.rally.domain.tournament.gateway.TournamentRepository;
import com.rally.domain.tournament.model.MatchGroup;
import com.rally.domain.tournament.model.TournamentData;
import com.rally.domain.tournament.model.TournamentEntryData;
import com.rally.domain.tournament.model.TournamentMatch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 赛事批量匹配领域能力：查询待匹配赛事，并完成资格赛、正赛各轮次的分组落地。
 */
@Service
@RequiredArgsConstructor
public class TournamentBatchMatchService {

    private final TournamentRepository tournamentRepository;
    private final TournamentEntryRepository tournamentEntryRepository;
    private final TournamentMatchingService tournamentMatchingService;
    private final TournamentMatchAssembleService tournamentMatchAssembleService;

    /** 查询指定时间点已进入资格赛阶段的激活赛事。 */
    public List<TournamentData> listTournamentsToMatch(LocalDateTime matchTime) {
        return tournamentRepository.findActiveWithQualifierStarted(matchTime);
    }

    /** 资格赛匹配：席位已满或候选人不足时不产出比赛。 */
    @Transactional
    public List<TournamentMatch> matchQualifier(String tournamentId) {
        TournamentData tournament = tournamentRepository.findByBizId(tournamentId);
        if (tournament == null || tournament.getCurrentFilledSlots() >= tournament.getTotalSlots()) {
            return List.of();
        }
        List<TournamentEntryData> candidates = tournamentEntryRepository.findWaitingByTournamentAndStage(
                tournamentId, TournamentEntryStageEnum.QUALIFY, TournamentRoundEnum.QUALIFIER);
        return doMatch(tournamentId, candidates, TournamentRoundEnum.QUALIFIER, tournament.getQualifierGroupSize());
    }

    /** 正赛匹配：逐一处理当前存在排队候选人的轮次，每轮两两匹配。 */
    @Transactional
    public List<TournamentMatch> matchMainRoundsAll(String tournamentId) {
        List<TournamentMatch> matches = new ArrayList<>();
        List<TournamentRoundEnum> rounds = tournamentEntryRepository.findDistinctWaitingRounds(
                tournamentId, TournamentEntryStageEnum.MAIN);
        for (TournamentRoundEnum round : rounds) {
            List<TournamentEntryData> candidates = tournamentEntryRepository.findWaitingByTournamentAndStage(
                    tournamentId, TournamentEntryStageEnum.MAIN, round);
            matches.addAll(doMatch(tournamentId, candidates, round, 2));
        }
        return matches;
    }

    private List<TournamentMatch> doMatch(String tournamentId, List<TournamentEntryData> candidates,
                                          TournamentRoundEnum round, int groupSize) {
        if (candidates.size() < groupSize) {
            return List.of();
        }
        RejectHistoryLookup rejectHistoryLookup = tournamentMatchAssembleService.buildRejectHistoryLookup(tournamentId);
        List<MatchGroup> groups = tournamentMatchingService.group(candidates, groupSize, rejectHistoryLookup);
        return tournamentMatchAssembleService.assemble(tournamentId, groups, round, groupSize);
    }
}
