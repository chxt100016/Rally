package com.rally.tour;

import com.rally.domain.tour.model.TournamentData;
import com.rally.domain.tour.repository.TourTournamentRepository;
import com.rally.protourdata.playerrankingcollect.activity.CollectAtpPlayerRankingsActivity;
import com.rally.protourdata.playerrankingcollect.activity.CollectWtaPlayerRankingsActivity;
import com.rally.protourdata.tournamentcatalogcollect.activity.CollectAtpTournamentCatalogActivity;
import com.rally.protourdata.tournamentcatalogcollect.activity.CollectWtaTournamentCatalogActivity;
import com.rally.protourdata.tournamentresultcollect.activity.CollectCompletedMatchResultsActivity;
import com.rally.tour.model.TourEnums;
import com.rally.tour.parser.CollectType;
import com.rally.tour.parser.DrawParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourCollectFacade {

    private final MatchCollectManager matchCollectManager;
    private final TourTournamentRepository tourTournamentRepository;
    private final CollectAtpTournamentCatalogActivity collectAtpTournamentCatalogActivity;
    private final CollectWtaTournamentCatalogActivity collectWtaTournamentCatalogActivity;
    private final CollectAtpPlayerRankingsActivity collectAtpPlayerRankingsActivity;
    private final CollectWtaPlayerRankingsActivity collectWtaPlayerRankingsActivity;
    private final CollectCompletedMatchResultsActivity collectCompletedMatchResultsActivity;

    public void tournaments(int year) {
        collectAtpTournamentCatalogActivity.execute(year);
        collectWtaTournamentCatalogActivity.execute(year);
    }

    public void currentDraws() {
        List<TournamentData> tournaments = currentTournaments();
        if (CollectionUtils.isEmpty(tournaments)) {
            log.info("当前无进行中的赛事");
            return;
        }
        for (TournamentData tournament : tournaments) {
            try {
                draws(tournament);
            } catch (Exception exception) {
                log.error("采集签表失败, tournamentId={}", tournament.getTournamentId(), exception);
            }
        }
    }

    public void draws(TournamentData tournament) {
        DrawParams params = params(tournament);
        switch (TourEnums.valueOf(tournament.getTour())) {
            case ATP -> matchCollectManager.collect(
                    "GS".equals(tournament.getCategory())
                            ? CollectType.ATP_APP_DRAW
                            : CollectType.ATP_DRAW,
                    params);
            case WTA -> {
                matchCollectManager.collect(
                        "GS".equals(tournament.getCategory())
                                ? CollectType.ATP_APP_DRAW
                                : CollectType.WTA_DRAW,
                        params);
                collectCompletedMatchResultsActivity.execute(params);
            }
        }
    }

    public void completed(TournamentData tournament) {
        collectCompletedMatchResultsActivity.execute(params(tournament));
    }

    public void oop() {
        for (TournamentData tournament : currentTournaments()) {
            DrawParams params = params(tournament);
            if ("WTA".equals(tournament.getTour())) {
                matchCollectManager.collect(
                        "GS".equals(tournament.getCategory())
                                ? CollectType.ATP_SCHEDULE_FOR_WTA
                                : CollectType.WTA_SCHEDULE,
                        params);
            } else if ("ATP".equals(tournament.getTour())) {
                matchCollectManager.collect(
                        "GS".equals(tournament.getCategory())
                                ? CollectType.ATP_SCHEDULE
                                : CollectType.ATP_OOP,
                        params);
            }
        }
    }

    public void liveMatch() {
        for (TournamentData tournament : currentTournaments()) {
            matchCollectManager.collect(CollectType.ATP_APP_LIVE, params(tournament));
        }
    }

    public void matches(CollectType.Phase phase) {
        switch (phase) {
            case DRAW -> currentDraws();
            case OOP -> oop();
            case LIVE -> liveMatch();
        }
    }

    public void rank() {
        collectAtpPlayerRankingsActivity.execute();
        collectWtaPlayerRankingsActivity.execute();
    }

    public void draws(String tournamentId) {
        draws(tourTournamentRepository.findByTournamentId(tournamentId));
    }

    private List<TournamentData> currentTournaments() {
        return tourTournamentRepository.findCurrentTournaments(LocalDate.now());
    }

    private DrawParams params(TournamentData tournament) {
        return new DrawParams(
                tournament.getTournamentId(), tournament.getYear(), tournament.getTour());
    }
}
