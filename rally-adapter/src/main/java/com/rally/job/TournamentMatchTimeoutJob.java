package com.rally.job;

import com.rally.domain.tournament.enums.TournamentMatchStatusEnum;
import com.rally.domain.tournament.gateway.TournamentMatchRepository;
import com.rally.domain.tournament.model.TournamentMatch;
import com.rally.tournament.courtbookerselectiontimeout.activity.RejectTimeoutMatchActivity;
import com.rally.tournament.resultconfirmationtimeout.activity.CompleteTimeoutResultActivity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "job.tournamentMatchTimeout.enabled", havingValue = "true")
public class TournamentMatchTimeoutJob {

    private final TournamentMatchRepository matchRepository;
    private final RejectTimeoutMatchActivity rejectTimeoutMatchActivity;
    private final CompleteTimeoutResultActivity completeTimeoutResultActivity;

    @Scheduled(cron = "${job.tournamentMatchTimeout.cron:0 0 */2 * * ?}")
    public void processTimeoutMatches() {
        log.info("开始处理超时比赛");
        processMatchedTimeout();
        processPendingConfirmTimeout();
        log.info("超时比赛处理完成");
    }

    private void processMatchedTimeout() {
        LocalDateTime timeoutBefore = LocalDateTime.now().minusDays(3);
        List<TournamentMatch> matches = matchRepository.findTimeoutMatches(TournamentMatchStatusEnum.MATCHED, timeoutBefore);
        log.info("MATCHED状态超时比赛数量: {}", matches.size());
        for (TournamentMatch match : matches) {
            try {
                rejectTimeoutMatchActivity.execute(match.getMatchId());
            } catch (Exception e) {
                log.error("处理MATCHED超时比赛失败, matchId={}", match.getMatchId(), e);
            }
        }
    }

    private void processPendingConfirmTimeout() {
        LocalDateTime timeoutBefore = LocalDateTime.now().minusHours(48);
        List<TournamentMatch> matches = matchRepository.findTimeoutMatches(TournamentMatchStatusEnum.PENDING_CONFIRM, timeoutBefore);
        log.info("PENDING_CONFIRM状态超时比赛数量: {}", matches.size());
        for (TournamentMatch match : matches) {
            try {
                completeTimeoutResultActivity.execute(match.getMatchId(), LocalDateTime.now());
            } catch (Exception e) {
                log.error("处理PENDING_CONFIRM超时比赛失败, matchId={}", match.getMatchId(), e);
            }
        }
    }
}
