package com.rally.db.tennis.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.tennis.entity.TennisMatchPO;
import com.rally.db.tennis.mapper.TennisMatchMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TennisMatchService extends ServiceImpl<TennisMatchMapper, TennisMatchPO> {

    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateBatch(List<TennisMatchPO> matches) {
        if (CollectionUtils.isEmpty(matches)) {
            return;
        }

        List<String> matchIds = matches.stream()
                .map(TennisMatchPO::getMatchId)
                .filter(java.util.Objects::nonNull)
                .toList();

        Map<String, TennisMatchPO> existMap = this.lambdaQuery()
                .in(TennisMatchPO::getMatchId, matchIds)
                .list()
                .stream()
                .collect(Collectors.toMap(TennisMatchPO::getMatchId, m -> m, (a, b) -> a));

        List<TennisMatchPO> toInsert = matches.stream()
                .filter(m -> m.getMatchId() == null || !existMap.containsKey(m.getMatchId()))
                .toList();

        List<TennisMatchPO> toUpdate = matches.stream()
                .filter(m -> m.getMatchId() != null && existMap.containsKey(m.getMatchId()))
                .peek(m -> {
                    TennisMatchPO po = existMap.get(m.getMatchId());
                    m.setId(po.getId());
                })
                .toList();

        if (CollectionUtils.isNotEmpty(toInsert)) {
            this.saveBatch(toInsert);
            log.info("批量插入比赛: {}条", toInsert.size());
        }
        if (CollectionUtils.isNotEmpty(toUpdate)) {
            this.updateBatchById(toUpdate);
            log.info("批量更新比赛: {}条", toUpdate.size());
        }
    }

    public List<TennisMatchPO> findActiveByTournament(String tournamentId, Integer year) {
        return this.lambdaQuery()
                .eq(TennisMatchPO::getTournamentId, tournamentId)
                .eq(TennisMatchPO::getYear, year)
                .in(TennisMatchPO::getStatus, "live", "scheduled")
                .list();
    }

    public boolean hasActiveMatches() {
        return this.lambdaQuery()
                .in(TennisMatchPO::getStatus, "live", "scheduled")
                .exists();
    }
}
