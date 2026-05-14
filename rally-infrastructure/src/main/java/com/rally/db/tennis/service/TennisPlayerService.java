package com.rally.db.tennis.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.tennis.entity.TennisPlayerPO;
import com.rally.db.tennis.mapper.TennisPlayerMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TennisPlayerService extends ServiceImpl<TennisPlayerMapper, TennisPlayerPO> {

    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateBatch(List<TennisPlayerPO> players) {
        if (CollectionUtils.isEmpty(players)) {
            return;
        }

        // ATP 和 WTA 的 playerId 来自不同系统可能重复，去重和查询都按 (playerId, tour) 组合键
        players = new java.util.ArrayList<>(players.stream()
                .filter(p -> p.getPlayerId() != null && p.getTour() != null)
                .collect(Collectors.toMap(
                        TennisPlayerService::playerKey,
                        p -> p,
                        (a, b) -> b
                ))
                .values());

        // 按 tour 分组，每组用 in(playerId) 查询，避免笛卡尔积
        Map<String, List<TennisPlayerPO>> playersByTour = players.stream()
                .collect(Collectors.groupingBy(TennisPlayerPO::getTour));

        Map<String, TennisPlayerPO> existMap = new java.util.HashMap<>();
        for (Map.Entry<String, List<TennisPlayerPO>> entry : playersByTour.entrySet()) {
            String tour = entry.getKey();
            List<String> ids = entry.getValue().stream()
                    .map(TennisPlayerPO::getPlayerId)
                    .toList();
            this.lambdaQuery()
                    .eq(TennisPlayerPO::getTour, tour)
                    .in(TennisPlayerPO::getPlayerId, ids)
                    .list()
                    .forEach(po -> existMap.put(playerKey(po), po));
        }

        List<TennisPlayerPO> toInsert = players.stream()
                .filter(p -> !existMap.containsKey(playerKey(p)))
                .toList();

        List<TennisPlayerPO> toUpdate = players.stream()
                .filter(p -> existMap.containsKey(playerKey(p)))
                .map(p -> {
                    TennisPlayerPO po = existMap.get(playerKey(p));
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

    private static String playerKey(TennisPlayerPO po) {
        return po.getTour() + ":" + po.getPlayerId();
    }
}
