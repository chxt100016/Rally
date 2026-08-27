package com.rally.protourdata.playerrankingcollect.activity;

import com.rally.client.wta.WtaClient;
import com.rally.client.wta.model.WtaRankingsResponse;
import com.rally.domain.tour.model.PlayerData;
import com.rally.domain.tour.repository.TourPlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务活动 collect-wta-player-rankings：采集并刷新 WTA 前 200 名球员。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollectWtaPlayerRankingsActivity {

    private static final String WTA = "WTA";
    private static final int FROM_RANK = 1;
    private static final int TO_RANK = 200;

    private final WtaClient wtaClient;
    private final TourPlayerRepository tourPlayerRepository;

    public void execute() {
        // A1：ATP 提交或空来源跳过后，固定请求 WTA 1..200。
        WtaRankingsResponse response = wtaClient.getRankings(FROM_RANK, TO_RANK);
        if (response == null
                || response.getData() == null
                || response.getData().getRankings() == null
                || CollectionUtils.isEmpty(response.getData().getRankings().getPlayers())) {
            log.warn("WTA排名数据为空");
            return;
        }

        List<WtaRankingsResponse.PlayerRanking> sourcePlayers =
                response.getData().getRankings().getPlayers();

        // A2：沿用 main 的 WTA 字段映射与日期容错，并强制使用 WTA 身份空间。
        // 缺 playerId 的记录不入库；批内重复保持首次顺序，后续非 null 字段覆盖。
        Map<String, PlayerData> deduplicated = new LinkedHashMap<>();
        for (WtaRankingsResponse.PlayerRanking source : sourcePlayers) {
            PlayerData player = toPlayer(source);
            if (player.getPlayerId() == null || player.getTour() == null) {
                continue;
            }
            deduplicated.merge(playerKey(player), player,
                    CollectWtaPlayerRankingsActivity::mergeNonNull);
        }

        // A3：仓储以 (tour, playerId) 新增或仅非 null 覆盖；不捕获来源、
        // 转换、查询或保存异常，使 WTA 当批回滚且不影响已提交 ATP。
        tourPlayerRepository.saveOrUpdateBatch(new ArrayList<>(deduplicated.values()));
        log.info("WTA排名采集完成: {}条", sourcePlayers.size());
    }

    private PlayerData toPlayer(WtaRankingsResponse.PlayerRanking source) {
        PlayerData player = new PlayerData();
        player.setPlayerId(source.getPlayerId());
        player.setTour(WTA);
        player.setFirstName(source.getFirstName());
        player.setLastName(source.getLastName());
        player.setNationality(source.getNatlId());
        player.setRank(source.getRank());
        player.setPoints(source.getPoints());
        player.setBirthDate(parseDate(source.getBirthDate()));
        return player;
    }

    private LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(date.length() > 10 ? date.substring(0, 10) : date);
        } catch (Exception exception) {
            log.debug("解析日期失败: {}", date);
            return null;
        }
    }

    private static String playerKey(PlayerData player) {
        return player.getTour() + ":" + player.getPlayerId();
    }

    private static PlayerData mergeNonNull(PlayerData existing, PlayerData incoming) {
        if (incoming.getFirstName() != null) existing.setFirstName(incoming.getFirstName());
        if (incoming.getLastName() != null) existing.setLastName(incoming.getLastName());
        if (incoming.getNationality() != null) existing.setNationality(incoming.getNationality());
        if (incoming.getBirthDate() != null) existing.setBirthDate(incoming.getBirthDate());
        if (incoming.getGender() != null) existing.setGender(incoming.getGender());
        if (incoming.getRank() != null) existing.setRank(incoming.getRank());
        if (incoming.getPoints() != null) existing.setPoints(incoming.getPoints());
        if (incoming.getHand() != null) existing.setHand(incoming.getHand());
        return existing;
    }
}
