package com.rally.domain.tournament.service;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.tournament.enums.TournamentRoundEnum;
import com.rally.domain.tournament.gateway.TournamentMatchRepository;
import com.rally.domain.tournament.gateway.TournamentRepository;
import com.rally.domain.tournament.model.TournamentData;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.function.ToIntFunction;

/**
 * 统一判断并推进赛事当前轮次。
 * 资格赛结束后等待全部正赛席位支付完成，再进入正赛首轮；正赛每轮全部完成后进入下一轮。
 */
@Service
@RequiredArgsConstructor
public class TournamentRoundProgressService {

    private final TournamentRepository tournamentRepository;
    private final TournamentMatchRepository tournamentMatchRepository;

    public void advanceIfReady(String tournamentId) {
        TournamentData tournament = tournamentRepository.findByBizId(tournamentId);
        Assert.notNull(tournament, BizErrorCode.TOURNAMENT_NOT_FOUND);
        TournamentRoundEnum targetRound = calculateTargetRound(tournament,
                round -> tournamentMatchRepository.countCompletedByTournamentAndRound(tournamentId, round));
        if (targetRound != null) {
            tournamentRepository.advanceCurrentRoundIfLater(tournamentId, targetRound);
        }
    }

    TournamentRoundEnum calculateTargetRound(TournamentData tournament, ToIntFunction<TournamentRoundEnum> completedCount) {
        int totalSlots = tournament.getTotalSlots();
        if (!isRoundCompleted(TournamentRoundEnum.QUALIFIER, totalSlots, completedCount)) {
            return null;
        }

        // 资格赛比赛已全部结束，但正赛席位尚未全部支付时，赛事仍停留在资格赛阶段。
        if (tournament.getCurrentFilledSlots() == null || tournament.getCurrentFilledSlots() < totalSlots) {
            return TournamentRoundEnum.QUALIFIER;
        }

        TournamentRoundEnum firstMainRound = TournamentRoundEnum.firstMainRound(totalSlots);
        TournamentRoundEnum targetRound = firstMainRound;
        for (TournamentRoundEnum round = firstMainRound; round != null; round = round.nextRound()) {
            if (!isRoundCompleted(round, totalSlots, completedCount)) {
                break;
            }
            TournamentRoundEnum nextRound = round.nextRound();
            targetRound = nextRound == null ? round : nextRound;
        }
        return targetRound;
    }

    private boolean isRoundCompleted(TournamentRoundEnum round, int totalSlots,
                                     ToIntFunction<TournamentRoundEnum> completedCount) {
        int requiredCount = round.requiredMatchCount(totalSlots);
        return requiredCount > 0
                && completedCount.applyAsInt(round) >= requiredCount;
    }
}
