package com.rally.db.tour.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.tour.entity.TourTournamentPO;
import com.rally.db.tour.mapper.TourTournamentMapper;
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
public class TourTournamentService extends ServiceImpl<TourTournamentMapper, TourTournamentPO> {

    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateBatch(List<TourTournamentPO> tournaments) {
        if (CollectionUtils.isEmpty(tournaments)) {
            return;
        }

        List<String> tournamentIds = tournaments.stream()
                .map(TourTournamentPO::getTournamentId)
                .filter(java.util.Objects::nonNull)
                .toList();

        Map<String, TourTournamentPO> existMap = this.lambdaQuery()
                .in(TourTournamentPO::getTournamentId, tournamentIds)
                .list()
                .stream()
                .collect(Collectors.toMap(
                        t -> t.getTournamentId() + "_" + t.getYear(),
                        t -> t, (a, b) -> a));

        List<TourTournamentPO> toInsert = tournaments.stream()
                .filter(t -> t.getTournamentId() == null
                        || !existMap.containsKey(t.getTournamentId() + "_" + t.getYear()))
                .toList();

        List<TourTournamentPO> toUpdate = tournaments.stream()
                .filter(t -> t.getTournamentId() != null
                        && existMap.containsKey(t.getTournamentId() + "_" + t.getYear()))
                .map(t -> {
                    TourTournamentPO po = existMap.get(t.getTournamentId() + "_" + t.getYear());
                    po.setName(t.getName());
                    po.setTour(t.getTour());
                    po.setCategory(t.getCategory());
                    po.setSurface(t.getSurface());
                    po.setCity(t.getCity());
                    po.setCountry(t.getCountry());
                    po.setPrizeMoney(t.getPrizeMoney());
                    po.setPrizeMoneyText(t.getPrizeMoneyText());
                    po.setStatus(t.getStatus());
                    po.setStartDate(t.getStartDate());
                    po.setEndDate(t.getEndDate());
                    return po;
                })
                .toList();

        if (CollectionUtils.isNotEmpty(toInsert)) {
            this.saveBatch(toInsert);
            log.info("批量插入赛事: {}条", toInsert.size());
        }
        if (CollectionUtils.isNotEmpty(toUpdate)) {
            this.updateBatchById(toUpdate);
            log.info("批量更新赛事: {}条", toUpdate.size());
        }
    }

    public List<TourTournamentPO> findActive() {
        return this.lambdaQuery()
                .eq(TourTournamentPO::getStatus, "active")
                .list();
    }

    public List<TourTournamentPO> findCurrentTournaments(LocalDate date) {
        return this.lambdaQuery()
                .le(TourTournamentPO::getStartDate, date)
                .ge(TourTournamentPO::getEndDate, date)
                .list();
    }

    /**
     * 按条件查询赛事，日期区间取赛事时间与查询窗口的交集（startDate <= dateTo && endDate >= dateFrom）
     */
    public boolean existsByTournamentId(String tournamentId) {
        return lambdaQuery().eq(TourTournamentPO::getTournamentId, tournamentId).exists();
    }

    public List<TourTournamentPO> listByCondition(String dbStatus, String tour,
                                                     LocalDate dateFrom, LocalDate dateTo) {
        var wrapper = this.lambdaQuery();
        if (dbStatus != null) {
            wrapper.eq(TourTournamentPO::getStatus, dbStatus);
        }
        if (tour != null) {
            wrapper.eq(TourTournamentPO::getTour, tour);
        }
        if (dateFrom != null) {
            wrapper.ge(TourTournamentPO::getEndDate, dateFrom);
        }
        if (dateTo != null) {
            wrapper.le(TourTournamentPO::getStartDate, dateTo);
        }
        wrapper.orderByAsc(TourTournamentPO::getStartDate);
        return wrapper.list();
    }

    public void updateImagePaths(String tournamentId, String imagePath, String backgroundPath) {
        this.lambdaUpdate()
                .eq(TourTournamentPO::getTournamentId, tournamentId)
                .set(TourTournamentPO::getImagePath, imagePath)
                .set(TourTournamentPO::getBackgroundPath, backgroundPath)
                .update();
    }

    /** 按 tournamentId 列表批量查询 */
    public List<TourTournamentPO> listByTournamentIds(List<String> tournamentIds) {
        if (CollectionUtils.isEmpty(tournamentIds)) {
            return List.of();
        }
        return this.lambdaQuery()
                .in(TourTournamentPO::getTournamentId, tournamentIds)
                .list();
    }

    public TourTournamentPO findByTournamentId(String tournamentId) {
        return this.lambdaQuery()
                .eq(TourTournamentPO::getTournamentId, tournamentId)
                .last("LIMIT 1")
                .one();
    }

    /** 查询指定日期窗口内 background_path 为空的赛事 */
    public List<TourTournamentPO> listPendingBackground(LocalDate dateFrom, LocalDate dateTo) {
        return this.lambdaQuery()
                .le(TourTournamentPO::getStartDate, dateTo)
                .ge(TourTournamentPO::getEndDate, dateFrom)
                .and(w -> w.isNull(TourTournamentPO::getBackgroundPath)
                        .or().eq(TourTournamentPO::getBackgroundPath, ""))
                .orderByAsc(TourTournamentPO::getStartDate)
                .list();
    }
}
