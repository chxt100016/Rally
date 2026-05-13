package com.rally.tennis;

import com.rally.client.tennistv.TennisTvClient;
import com.rally.client.tennistv.model.AtpDrawsResponse;
import com.rally.client.wta.WtaClient;
import com.rally.client.wta.model.WtaDrawsResponse;
import com.rally.client.wta.model.WtaMatchesResponse;
import com.rally.db.tennis.entity.TennisTournamentPO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class TennisCollectService {

    @Resource
    private TennisTvClient tennisTvClient;

    @Resource
    private WtaClient wtaClient;

    @Resource
    private TournamentCollectService tournamentCollectService;

    @Resource
    private DrawCollectService drawCollectService;

    @Resource
    private PlayerCollectService playerCollectService;

    @Resource
    private MatchCollectService matchCollectService;


    /**
     * 采集指定年份的赛事列表
     */
    public void tournaments(int year) {
        tournamentCollectService.collectTournament(year);
    }

    /**
     * 当前签表
     */
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

    /**
     * 指定签表
     */
    public void draws(String tour, String tournamentId, int year) {
        if (tour.equals("ATP")) {
            AtpDrawsResponse response = tennisTvClient.getDraws(tournamentId, year);
            if (response != null && response.getMS() != null && CollectionUtils.isNotEmpty(response.getMS().getRounds())) {
                // 签表
                Long drawId = this.drawCollectService.atp(response, tournamentId, year);
                // 比赛
                this.matchCollectService.atpFromDraw(response, tournamentId, drawId, year);
                // 球员赛事登记
                this.tournamentCollectService.atpTournamentEntry(response, tournamentId, year, drawId, "MS");
                // 球员
                this.playerCollectService.atpFromDraw(response);
            }
        } else if (tour.equals("WTA")) {
            WtaDrawsResponse response = wtaClient.getDraws(tournamentId, year);
            if (response != null && response.getData() != null
                    && CollectionUtils.isNotEmpty(response.getData().getResults())) {
                // 签表
                Long drawId = this.drawCollectService.wta(response, tournamentId, year);
                // 比赛
                this.matchCollectService.wtaFromDraw(response, tournamentId, drawId, year);
                // 球员赛事登记
                this.tournamentCollectService.wtaTournamentEntry(response, tournamentId, year, drawId);
                // 球员
                this.playerCollectService.wtaFromDraw(response);
            }
        }
    }

    /**
     * order of play
     */
    public void oop() {
        this.matchCollectService.atpFromOop();

        List<TennisTournamentPO> current = this.tournamentCollectService.current();
        for (TennisTournamentPO item : current) {
            this.matchCollectService.wtaFromOop(item.getTournamentId(), item.getYear());
        }

    }


    /**
     * 进行中比赛
     */
    public void liveMatch() {
        this.matchCollectService.atpFromLive();

        List<TennisTournamentPO> tournaments = tournamentCollectService.current();
        if (CollectionUtils.isEmpty(tournaments)) {
            return;
        }
        for (TennisTournamentPO tournament : tournaments) {
            if (!"WTA".equals(tournament.getTour())) {
                continue;
            }
            try {
                WtaMatchesResponse response = wtaClient.getLiveMatches(tournament.getTournamentId(), tournament.getYear());
                if (response != null && CollectionUtils.isNotEmpty(response.getMatches())) {
                    this.matchCollectService.wtaFromLive(response.getMatches());
                }
            } catch (Exception e) {
                log.error("采集WTA进行中比赛失败, tournamentId={}", tournament.getTournamentId(), e);
            }
        }
    }
}
