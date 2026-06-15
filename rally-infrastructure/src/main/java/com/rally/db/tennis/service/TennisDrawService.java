package com.rally.db.tennis.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.tennis.entity.TennisDrawPO;
import com.rally.db.tennis.mapper.TennisDrawMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class TennisDrawService extends ServiceImpl<TennisDrawMapper, TennisDrawPO> {

    public Long findId(String tournamentId, Integer year, String drawType) {
        TennisDrawPO po = this.lambdaQuery()
                .eq(TennisDrawPO::getTournamentId, tournamentId)
                .eq(TennisDrawPO::getYear, year)
                .eq(TennisDrawPO::getDrawType, drawType)
                .one();
        return po != null ? po.getId() : null;
    }

    public List<TennisDrawPO> listByTournamentIds(List<String> tournamentIds) {
        if (CollectionUtils.isEmpty(tournamentIds)) {
            return List.of();
        }
        return this.lambdaQuery()
                .in(TennisDrawPO::getTournamentId, tournamentIds)
                .list();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long saveOrUpdate(String tournamentId, Integer year, String drawType, Integer size, Integer totalRounds) {
        TennisDrawPO existing = this.lambdaQuery()
                .eq(TennisDrawPO::getTournamentId, tournamentId)
                .eq(TennisDrawPO::getYear, year)
                .eq(TennisDrawPO::getDrawType, drawType)
                .one();

        if (existing != null) {
            existing.setSize(size);
            existing.setTotalRounds(totalRounds);
            this.updateById(existing);
            return existing.getId();
        }

        TennisDrawPO draw = new TennisDrawPO();
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
