package com.rally.tennis;

import com.rally.client.atp.AtpClient;
import com.rally.client.atp.model.AtpRankingsResponse;
import com.rally.client.tennistv.TennisTvClient;
import com.rally.client.tennistv.model.AtpDrawsResponse;
import com.rally.client.wta.WtaClient;
import com.rally.client.wta.model.WtaDrawsResponse;
import com.rally.client.wta.model.WtaMatchesResponse;
import com.rally.client.wta.model.WtaRankingsResponse;
import com.rally.db.tennis.entity.TennisTournamentPO;
import com.rally.tennis.model.Player;
import com.rally.tennis.model.TourEnums;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class TennisCollectService {

    @Resource
    private TennisTvClient tennisTvClient;

    @Resource
    private WtaClient wtaClient;

    @Resource
    private AtpClient atpClient;

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
        switch (TourEnums.valueOf(tour)) {
            case ATP -> parseAtpDraw(tournamentId, year);
            case WTA -> parseWtaDraw(tournamentId, year);
        }
    }

    private void parseAtpDraw(String tournamentId, int year) {
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
    }

    private void parseWtaDraw(String tournamentId, int year) {
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

    /**
     * 采集 ATP 和 WTA 球员排名
     */
    public void rank() {
        atpRank();
        wtaRank();
    }

    private void atpRank() {
        AtpRankingsResponse response = atpClient.getRankings(1, 100);
        if (response == null || response.getData() == null
                || response.getData().getRankings() == null
                || CollectionUtils.isEmpty(response.getData().getRankings().getPlayers())) {
            log.warn("ATP排名数据为空");
            return;
        }
        List<Player> players = response.getData().getRankings().getPlayers().stream()
                .map(this::fromAtpRanking)
                .toList();
        playerCollectService.savePlayers(players, "ATP");
        log.info("ATP排名采集完成: {}条", players.size());
    }

    private void wtaRank() {
        WtaRankingsResponse response = wtaClient.getRankings(1, 100);
        if (response == null || response.getData() == null
                || response.getData().getRankings() == null
                || CollectionUtils.isEmpty(response.getData().getRankings().getPlayers())) {
            log.warn("WTA排名数据为空");
            return;
        }
        List<Player> players = response.getData().getRankings().getPlayers().stream()
                .map(this::fromWtaRanking)
                .toList();
        playerCollectService.savePlayers(players, "WTA");
        log.info("WTA排名采集完成: {}条", players.size());
    }

    private Player fromAtpRanking(AtpRankingsResponse.PlayerRanking r) {
        Player p = new Player();
        p.setPlayerId(r.getPlayerId());
        p.setFirstName(r.getFirstName());
        p.setLastName(r.getLastName());
        p.setNationality(r.getNatlId());
        p.setRank(r.getRank());
        p.setPoints(r.getPoints());
        p.setBirthDate(parseDate(r.getBirthDate()));
        return p;
    }

    private Player fromWtaRanking(WtaRankingsResponse.PlayerRanking r) {
        Player p = new Player();
        p.setPlayerId(r.getPlayerId());
        p.setFirstName(r.getFirstName());
        p.setLastName(r.getLastName());
        p.setNationality(r.getNatlId());
        p.setRank(r.getRank());
        p.setPoints(r.getPoints());
        p.setBirthDate(parseDate(r.getBirthDate()));
        return p;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            // API 返回格式如 "2001-08-16T00:00:00"，取前 10 位
            return LocalDate.parse(dateStr.length() > 10 ? dateStr.substring(0, 10) : dateStr);
        } catch (Exception e) {
            log.debug("解析日期失败: {}", dateStr);
            return null;
        }
    }
}
