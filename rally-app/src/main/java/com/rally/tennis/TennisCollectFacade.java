package com.rally.tennis;

import com.rally.domain.tennis.gateway.TennisTournamentGateway;
import com.rally.domain.tennis.model.TournamentData;
import com.rally.tennis.model.TourEnums;
import com.rally.tennis.parser.CollectType;
import com.rally.tennis.parser.DrawParams;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class TennisCollectFacade {

    @Resource
    private TournamentCollectService tournamentCollectService;

    @Resource
    private PlayerCollectService playerCollectService;

    @Resource
    private MatchCollectManager matchCollectManager;

    @Resource
    private TennisTournamentGateway tennisTournamentGateway;

    public void tournaments(int year) {
        tournamentCollectService.collectTournament(year);
    }

    public void currentDraws() {
        List<TournamentData> tournaments = tournamentCollectService.current();
        if (CollectionUtils.isEmpty(tournaments)) {
            log.info("当前无进行中的赛事");
            return;
        }
        for (TournamentData tournament : tournaments) {
            try {
                this.draws(tournament);
            } catch (Exception e) {
                log.error("采集签表失败, tournamentId={}", tournament.getTournamentId(), e);
            }
        }
    }

    public void draws(TournamentData tournament) {
        DrawParams params = new DrawParams(tournament.getTournamentId(), tournament.getYear(), tournament.getTour());
        switch (TourEnums.valueOf(tournament.getTour())) {
            case ATP -> {
                if (tournament.getCategory().equals("GS")) {
                    matchCollectManager.collect(CollectType.ATP_APP_DRAW, params);
                } else {
                    matchCollectManager.collect(CollectType.ATP_DRAW, params);
                }
            }
            case WTA -> {
                if (tournament.getCategory().equals("GS")) {
                    matchCollectManager.collect(CollectType.ATP_APP_DRAW, params);
                    matchCollectManager.collect(CollectType.ATP_APP_COMPLETED, params);
                } else {
                    matchCollectManager.collect(CollectType.WTA_DRAW, params);
                }
            }
        }
    }

    public void completed(TournamentData tournament) {
        DrawParams params = new DrawParams(tournament.getTournamentId(), tournament.getYear(), tournament.getTour());
        matchCollectManager.collect(CollectType.ATP_APP_COMPLETED, params);
    }

    public void oop() {
        List<TournamentData> current = tournamentCollectService.current();
        for (TournamentData item : current) {
            if ("WTA".equals(item.getTour())) {
                if (item.getCategory().equals("GS")) {
                    matchCollectManager.collect(CollectType.ATP_SCHEDULE_FOR_WTA, new DrawParams(item.getTournamentId(), item.getYear(), item.getTour()));
                } else {
                    matchCollectManager.collect(CollectType.WTA_SCHEDULE, new DrawParams(item.getTournamentId(), item.getYear(), item.getTour()));
                }
            } else if ("ATP".equals(item.getTour())) {
                if (item.getCategory().equals("GS")) {
                    matchCollectManager.collect(CollectType.ATP_SCHEDULE, new DrawParams(item.getTournamentId(), item.getYear(), item.getTour()));
                } else {
                    matchCollectManager.collect(CollectType.ATP_OOP, new DrawParams(item.getTournamentId(), item.getYear(), item.getTour()));
                }
            }
        }
    }

    public void liveMatch() {
        List<TournamentData> tournaments = tournamentCollectService.current();
        if (CollectionUtils.isEmpty(tournaments)) return;
        for (TournamentData tournament : tournaments) {
            matchCollectManager.collect(CollectType.ATP_APP_LIVE, new DrawParams(tournament.getTournamentId(), tournament.getYear(), tournament.getTour()));
        }
    }

    public void rank() {
        playerCollectService.atpRank();
        playerCollectService.wtaRank();
    }

    public void draws(String tournamentId) {
        TournamentData byTournamentId = tennisTournamentGateway.findByTournamentId(tournamentId);
        this.draws(byTournamentId);
    }
}
