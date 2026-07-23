package com.rally.job;

import com.rally.domain.tournament.model.TournamentData;
import com.rally.tournament.TournamentMatchFacade;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 每日凌晨2点批量匹配 Job（资格赛+正赛逐轮匹配）
 */
@Slf4j
@Component
//@ConditionalOnProperty(name = "job.tournamentMatch.enabled", havingValue = "true")
public class TournamentMatchJob {

    @Resource
    private TournamentMatchFacade tournamentMatchFacade;

    @Scheduled(cron = "${job.tournamentMatch.cron:0 0 2 * * ?}")
    public void run() {
        List<TournamentData> tournaments = tournamentMatchFacade.listTournamentsToMatch();
        for (TournamentData tournament : tournaments) {
            try {
                tournamentMatchFacade.matchQualifier(tournament.getBizId());
                tournamentMatchFacade.matchMainRoundsAll(tournament.getBizId());
            } catch (Exception e) {
                log.error("赛事匹配失败 tournamentId={}", tournament.getBizId(), e);
            }
        }
    }
}
