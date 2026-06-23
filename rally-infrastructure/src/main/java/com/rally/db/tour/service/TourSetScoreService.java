package com.rally.db.tour.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.tour.entity.TourSetScorePO;
import com.rally.db.tour.mapper.TourSetScoreMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TourSetScoreService extends ServiceImpl<TourSetScoreMapper, TourSetScorePO> {

    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateBatch(List<TourSetScorePO> scores) {
        if (CollectionUtils.isEmpty(scores)) {
            return;
        }

        List<Long> tourMatchIds = scores.stream()
                .map(TourSetScorePO::getTourMatchId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        // 按 tourMatchId 分组，tourMatchId 是 tour_match.id 全局唯一，不存在跨赛事冲突
        Map<Long, List<TourSetScorePO>> existMap = this.lambdaQuery()
                .in(TourSetScorePO::getTourMatchId, tourMatchIds)
                .list()
                .stream()
                .collect(Collectors.groupingBy(TourSetScorePO::getTourMatchId));

        List<TourSetScorePO> toInsert = scores.stream()
                .filter(s -> {
                    List<TourSetScorePO> existing = existMap.get(s.getTourMatchId());
                    if (existing == null || existing.isEmpty()) return true;
                    return existing.stream().noneMatch(e -> e.getSetNumber().equals(s.getSetNumber()));
                })
                .toList();

        List<TourSetScorePO> toUpdate = scores.stream()
                .filter(s -> {
                    List<TourSetScorePO> existing = existMap.get(s.getTourMatchId());
                    if (existing == null) return false;
                    return existing.stream().anyMatch(e -> e.getSetNumber().equals(s.getSetNumber()));
                })
                .map(s -> {
                    List<TourSetScorePO> existing = existMap.get(s.getTourMatchId());
                    return existing.stream()
                            .filter(e -> e.getSetNumber().equals(s.getSetNumber()))
                            .findFirst()
                            .map(e -> {
                                e.setP1Games(s.getP1Games());
                                e.setP2Games(s.getP2Games());
                                e.setP1Tiebreak(s.getP1Tiebreak());
                                e.setP2Tiebreak(s.getP2Tiebreak());
                                return e;
                            })
                            .orElse(s);
                })
                .toList();

        if (CollectionUtils.isNotEmpty(toInsert)) {
            this.saveBatch(toInsert);
            log.info("批量插入盘分: {}条", toInsert.size());
        }
        if (CollectionUtils.isNotEmpty(toUpdate)) {
            this.updateBatchById(toUpdate);
            log.info("批量更新盘分: {}条", toUpdate.size());
        }
    }
}
