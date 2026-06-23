package com.rally.db.tour.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.tour.entity.TourMatchPO;
import com.rally.db.tour.mapper.TourMatchMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TourMatchService extends ServiceImpl<TourMatchMapper, TourMatchPO> {

    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateBatch(List<TourMatchPO> matches) {
        if (CollectionUtils.isEmpty(matches)) {
            return;
        }

        List<String> matchIds = matches.stream()
                .map(TourMatchPO::getMatchId)
                .filter(java.util.Objects::nonNull)
                .toList();

        Map<String, TourMatchPO> existMap = this.lambdaQuery()
                .in(TourMatchPO::getMatchId, matchIds)
                .list()
                .stream()
                .collect(Collectors.toMap(
                        m -> m.getMatchId() + "|" + m.getDrawId(),
                        m -> m,
                        (a, b) -> a));

        List<TourMatchPO> toInsert = matches.stream()
                .filter(m -> m.getMatchId() == null || !existMap.containsKey(m.getMatchId() + "|" + m.getDrawId()))
                .toList();

        List<TourMatchPO> toUpdate = matches.stream()
                .filter(m -> m.getMatchId() != null && existMap.containsKey(m.getMatchId() + "|" + m.getDrawId()))
                .peek(m -> {
                    TourMatchPO po = existMap.get(m.getMatchId() + "|" + m.getDrawId());
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

    public List<TourMatchPO> findActiveByTournament(String tournamentId, Integer year) {
        return this.lambdaQuery()
                .eq(TourMatchPO::getTournamentId, tournamentId)
                .eq(TourMatchPO::getYear, year)
                .in(TourMatchPO::getStatus, "live", "scheduled")
                .list();
    }

    public boolean hasActiveMatches() {
        return this.lambdaQuery()
                .in(TourMatchPO::getStatus, "live", "scheduled")
                .exists();
    }

    public List<TourMatchPO> findByTournamentIdsAndDate(List<String> tournamentIds, LocalDate date) {
        if (CollectionUtils.isEmpty(tournamentIds) || date == null) {
            return List.of();
        }
        return this.lambdaQuery()
                .in(TourMatchPO::getTournamentId, tournamentIds)
                .eq(TourMatchPO::getMatchDate, date)
                .list();
    }
}
