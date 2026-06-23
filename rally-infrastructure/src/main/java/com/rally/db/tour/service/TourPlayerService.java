package com.rally.db.tour.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.tour.entity.TourPlayerPO;
import com.rally.db.tour.mapper.TourPlayerMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TourPlayerService extends ServiceImpl<TourPlayerMapper, TourPlayerPO> {

    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateBatch(List<TourPlayerPO> players) {
        if (CollectionUtils.isEmpty(players)) {
            return;
        }

        // ATP 和 WTA 的 playerId 来自不同系统可能重复，去重和查询都按 (playerId, tour) 组合键
        players = new java.util.ArrayList<>(players.stream()
                .filter(p -> p.getPlayerId() != null && p.getTour() != null)
                .collect(Collectors.toMap(
                        TourPlayerService::playerKey,
                        p -> p,
                        (a, b) -> b
                ))
                .values());

        // 按 tour 分组，每组用 in(playerId) 查询，避免笛卡尔积
        Map<String, List<TourPlayerPO>> playersByTour = players.stream()
                .collect(Collectors.groupingBy(TourPlayerPO::getTour));

        Map<String, TourPlayerPO> existMap = new java.util.HashMap<>();
        for (Map.Entry<String, List<TourPlayerPO>> entry : playersByTour.entrySet()) {
            String tour = entry.getKey();
            List<String> ids = entry.getValue().stream()
                    .map(TourPlayerPO::getPlayerId)
                    .toList();
            this.lambdaQuery()
                    .eq(TourPlayerPO::getTour, tour)
                    .in(TourPlayerPO::getPlayerId, ids)
                    .list()
                    .forEach(po -> existMap.put(playerKey(po), po));
        }

        List<TourPlayerPO> toInsert = players.stream()
                .filter(p -> !existMap.containsKey(playerKey(p)))
                .toList();

        List<TourPlayerPO> toUpdate = players.stream()
                .filter(p -> existMap.containsKey(playerKey(p)))
                .map(p -> {
                    TourPlayerPO po = existMap.get(playerKey(p));
                    po.setFirstName(p.getFirstName());
                    po.setLastName(p.getLastName());
                    po.setNationality(p.getNationality());
                    po.setBirthDate(p.getBirthDate());
                    po.setGender(p.getGender());
                    if (p.getRank() != null) po.setRank(p.getRank());
                    if (p.getPoints() != null) po.setPoints(p.getPoints());
                    po.setHand(p.getHand());
                    return po;
                })
                .toList();

        if (CollectionUtils.isNotEmpty(toInsert)) {
            this.saveBatch(toInsert);
            log.info("批量插入球员: {}条", toInsert.size());
        }
        if (CollectionUtils.isNotEmpty(toUpdate)) {
            this.updateBatchById(toUpdate);
            log.info("批量更新球员: {}条", toUpdate.size());
        }
    }

    private static String playerKey(TourPlayerPO po) {
        return po.getTour() + ":" + po.getPlayerId();
    }

    public List<TourPlayerPO> listByPlayerIds(List<String> playerIds) {
        if (CollectionUtils.isEmpty(playerIds)) {
            return List.of();
        }
        return this.lambdaQuery()
                .in(TourPlayerPO::getPlayerId, playerIds)
                .list();
    }
}
