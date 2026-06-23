package com.rally.db.tour.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.tour.entity.TourDrawPO;
import com.rally.db.tour.mapper.TourDrawMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class TourDrawService extends ServiceImpl<TourDrawMapper, TourDrawPO> {

    public Long findId(String tournamentId, Integer year, String drawType) {
        TourDrawPO po = this.lambdaQuery()
                .eq(TourDrawPO::getTournamentId, tournamentId)
                .eq(TourDrawPO::getYear, year)
                .eq(TourDrawPO::getDrawType, drawType)
                .one();
        return po != null ? po.getId() : null;
    }

    public List<TourDrawPO> listByTournamentIds(List<String> tournamentIds) {
        if (CollectionUtils.isEmpty(tournamentIds)) {
            return List.of();
        }
        return this.lambdaQuery()
                .in(TourDrawPO::getTournamentId, tournamentIds)
                .list();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long saveOrUpdate(String tournamentId, Integer year, String drawType, Integer size, Integer totalRounds) {
        TourDrawPO existing = this.lambdaQuery()
                .eq(TourDrawPO::getTournamentId, tournamentId)
                .eq(TourDrawPO::getYear, year)
                .eq(TourDrawPO::getDrawType, drawType)
                .one();

        if (existing != null) {
            existing.setSize(size);
            existing.setTotalRounds(totalRounds);
            this.updateById(existing);
            return existing.getId();
        }

        TourDrawPO draw = new TourDrawPO();
        draw.setTournamentId(tournamentId);
        draw.setYear(year);
        draw.setDrawType(drawType);
        draw.setSize(size);
        draw.setTotalRounds(totalRounds);
        this.save(draw);
        log.info("创建签表: tournamentId={}, year={}, drawType={}, size={}", tournamentId, year, drawType, size);
        return draw.getId();
    }
}
