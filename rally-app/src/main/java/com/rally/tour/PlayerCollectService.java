package com.rally.tour;

import com.rally.client.atp.AtpClient;
import com.rally.client.atp.model.AtpRankingsResponse;
import com.rally.client.wta.WtaClient;
import com.rally.client.wta.model.WtaRankingsResponse;
import com.rally.domain.tour.repository.TourPlayerRepository;
import com.rally.tour.convert.PlayerAppConvertMapper;
import com.rally.tour.model.Player;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class PlayerCollectService {

    @Resource
    private TourPlayerRepository tourPlayerRepository;

    @Resource
    private AtpClient atpClient;

    @Resource
    private WtaClient wtaClient;

    public void savePlayers(List<Player> players) {
        if (CollectionUtils.isEmpty(players)) return;
        players.stream().filter(p -> p.getTour() == null).forEach(p -> p.setTour("ATP"));
        tourPlayerRepository.saveOrUpdateBatch(PlayerAppConvertMapper.INSTANCE.toPlayerDataList(players));
    }

    public void savePlayers(List<Player> players, String tour) {
        if (CollectionUtils.isEmpty(players)) {
            return;
        }
        players.forEach(p -> p.setTour(tour));
        tourPlayerRepository.saveOrUpdateBatch(PlayerAppConvertMapper.INSTANCE.toPlayerDataList(players));
    }

    public void atpRank() {
        AtpRankingsResponse response = atpClient.getRankings(1, 200);
        if (response == null || response.getData() == null
                || response.getData().getRankings() == null
                || CollectionUtils.isEmpty(response.getData().getRankings().getPlayers())) {
            log.warn("ATP排名数据为空");
            return;
        }
        List<Player> players = response.getData().getRankings().getPlayers().stream()
                .map(this::fromAtpRanking)
                .toList();
        this.savePlayers(players, "ATP");
        log.info("ATP排名采集完成: {}条", players.size());
    }

    public void wtaRank() {
        WtaRankingsResponse response = wtaClient.getRankings(1, 200);
        if (response == null || response.getData() == null
                || response.getData().getRankings() == null
                || CollectionUtils.isEmpty(response.getData().getRankings().getPlayers())) {
            log.warn("WTA排名数据为空");
            return;
        }
        List<Player> players = response.getData().getRankings().getPlayers().stream()
                .map(this::fromWtaRanking)
                .toList();
        this.savePlayers(players, "WTA");
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
            return LocalDate.parse(dateStr.length() > 10 ? dateStr.substring(0, 10) : dateStr);
        } catch (Exception e) {
            log.debug("解析日期失败: {}", dateStr);
            return null;
        }
    }
}
