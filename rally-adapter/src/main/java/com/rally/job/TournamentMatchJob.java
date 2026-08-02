package com.rally.job;

import com.rally.tournament.TournamentAdminAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 每日凌晨2点批量匹配 Job（资格赛+正赛逐轮匹配）
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "job.tournamentMatch.enabled", havingValue = "true")
public class TournamentMatchJob {

    private final TournamentAdminAppService tournamentAdminAppService;

    @Scheduled(cron = "${job.tournamentMatch.cron:0 0 2 * * ?}")
    public void run() {
        tournamentAdminAppService.runTournamentMatch();
    }
}
