package com.rally.protourdata.tournamentdrawcollect.activity;

import com.rally.domain.tour.model.PlayerData;
import com.rally.domain.tour.repository.TourPlayerRepository;
import com.rally.tour.convert.PlayerAppConvertMapper;
import com.rally.tour.model.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务活动 upsert-draw-players：保存一份来源签表中的球员资料。
 */
@Component
@RequiredArgsConstructor
public class UpsertDrawPlayersActivity {

    private static final String DEFAULT_TOUR = "ATP";
    private static final PlayerAppConvertMapper PLAYER_MAPPER =
            PlayerAppConvertMapper.INSTANCE;

    private final TourPlayerRepository tourPlayerRepository;

    /**
     * 沿用签表来源已标注的巡回赛；缺失时保留 main 的 ATP 默认值。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void execute(List<Player> sourcePlayers) {
        if (sourcePlayers == null || sourcePlayers.isEmpty()) {
            return;
        }
        sourcePlayers.stream()
                .filter(player -> player.getTour() == null)
                .forEach(player -> player.setTour(DEFAULT_TOUR));
        save(sourcePlayers);
    }

    /**
     * 来源路由已确定目标巡回赛时，强制使用该 tour 建立球员身份。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void execute(List<Player> sourcePlayers, String targetTour) {
        if (sourcePlayers == null || sourcePlayers.isEmpty()) {
            return;
        }
        sourcePlayers.forEach(player -> player.setTour(targetTour));
        save(sourcePlayers);
    }

    private void save(List<Player> sourcePlayers) {
        // A1：复用 main 的 Player -> PlayerData 映射，保留姓名、国籍、
        // 排名、积分、出生日期、性别与持拍手等来源字段。
        List<PlayerData> players = PLAYER_MAPPER.toPlayerDataList(sourcePlayers);

        // A2：以 tour+playerId 为身份，过滤无身份项；LinkedHashMap
        // 保留首次出现顺序，后续重复项只合并非 null 来源字段。
        Map<String, PlayerData> deduplicated = new LinkedHashMap<>();
        for (PlayerData player : players) {
            if (player == null || player.getPlayerId() == null || player.getTour() == null) {
                continue;
            }
            deduplicated.merge(playerKey(player), player,
                    UpsertDrawPlayersActivity::mergeNonNull);
        }

        // A3：空批次是无操作；非空批次由既有仓储按相同复合键
        // 新增或仅以非 null 字段刷新。异常不捕获，使本独立事务整批回滚。
        if (!deduplicated.isEmpty()) {
            tourPlayerRepository.saveOrUpdateBatch(
                    new ArrayList<>(deduplicated.values()));
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
