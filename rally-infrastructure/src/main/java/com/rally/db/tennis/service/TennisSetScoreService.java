package com.rally.db.tennis.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.tennis.entity.TennisSetScorePO;
import com.rally.db.tennis.mapper.TennisSetScoreMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TennisSetScoreService extends ServiceImpl<TennisSetScoreMapper, TennisSetScorePO> {

    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateBatch(List<TennisSetScorePO> scores) {
        if (CollectionUtils.isEmpty(scores)) {
            return;
        }

        List<Long> tennisMatchIds = scores.stream()
                .map(TennisSetScorePO::getTennisMatchId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        // 按 tennisMatchId 分组，tennisMatchId 是 tennis_match.id 全局唯一，不存在跨赛事冲突
        Map<Long, List<TennisSetScorePO>> existMap = this.lambdaQuery()
                .in(TennisSetScorePO::getTennisMatchId, tennisMatchIds)
                .list()
                .stream()
                .collect(Collectors.groupingBy(TennisSetScorePO::getTennisMatchId));

        List<TennisSetScorePO> toInsert = scores.stream()
                .filter(s -> {
                    List<TennisSetScorePO> existing = existMap.get(s.getTennisMatchId());
                    if (existing == null || existing.isEmpty()) return true;
                    return existing.stream().noneMatch(e -> e.getSetNumber().equals(s.getSetNumber()));
                })
                .toList();

        List<TennisSetScorePO> toUpdate = scores.stream()
                .filter(s -> {
                    List<TennisSetScorePO> existing = existMap.get(s.getTennisMatchId());
                    if (existing == null) return false;
                    return existing.stream().anyMatch(e -> e.getSetNumber().equals(s.getSetNumber()));
                })
                .map(s -> {
                    List<TennisSetScorePO> existing = existMap.get(s.getTennisMatchId());
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
