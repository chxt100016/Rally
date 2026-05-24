package com.rally.tennis;

import com.rally.db.tennis.entity.TennisTournamentPO;
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

    public void tournaments(int year) {
        tournamentCollectService.collectTournament(year);
    }

    public void currentDraws() {
        List<TennisTournamentPO> tournaments = tournamentCollectService.current();
        if (CollectionUtils.isEmpty(tournaments)) {
            log.info("当前无进行中的赛事");
            return;
        }

        for (TennisTournamentPO tournament : tournaments) {
            try {
                this.draws(tournament.getTour(), tournament.getTournamentId(), tournament.getYear());
            } catch (Exception e) {
                log.error("采集签表失败, tournamentId={}", tournament.getTournamentId(), e);
            }
        }
    }

    public void draws(String tour, String tournamentId, int year) {
        DrawParams params = new DrawParams(tournamentId, year);
        switch (TourEnums.valueOf(tour)) {
            case ATP -> matchCollectManager.collect(CollectType.ATP_SCHEDULE, params);
            case WTA -> matchCollectManager.collect(CollectType.WTA_DRAW, params);
        }
    }

    public void oop() {
        List<TennisTournamentPO> current = tournamentCollectService.current();
        for (TennisTournamentPO item : current) {
            if ("WTA".equals(item.getTour())) {
                matchCollectManager.collect(CollectType.WTA_SCHEDULE, new DrawParams(item.getTournamentId(), item.getYear()));
            } else if ("ATP".equals(item.getTour())) {
                matchCollectManager.collect(CollectType.ATP_SCHEDULE, new DrawParams(item.getTournamentId(), item.getYear()));
            }
        }
    }

    public void liveMatch() {
        matchCollectManager.collect(CollectType.ATP_LIVE, null);

        List<TennisTournamentPO> tournaments = tournamentCollectService.current();
        if (CollectionUtils.isEmpty(tournaments)) return;

        for (TennisTournamentPO tournament : tournaments) {
            if (!"WTA".equals(tournament.getTour())) continue;
            try {
                matchCollectManager.collect(CollectType.WTA_LIVE,
                        new DrawParams(tournament.getTournamentId(), tournament.getYear()));
            } catch (Exception e) {
                log.error("采集WTA进行中比赛失败, tournamentId={}", tournament.getTournamentId(), e);
            }
        }
    }

    public void rank() {
        playerCollectService.atpRank();
        playerCollectService.wtaRank();
    }
}
