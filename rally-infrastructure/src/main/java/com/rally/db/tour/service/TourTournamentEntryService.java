package com.rally.db.tour.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.tour.entity.TourTournamentEntryPO;
import com.rally.db.tour.mapper.TourTournamentEntryMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class TourTournamentEntryService extends ServiceImpl<TourTournamentEntryMapper, TourTournamentEntryPO> {

    @Transactional(rollbackFor = Exception.class)
    public void saveEntries(List<TourTournamentEntryPO> entries) {
        for (TourTournamentEntryPO entry : entries) {
            TourTournamentEntryPO existing = this.lambdaQuery()
                    .eq(TourTournamentEntryPO::getDrawId, entry.getDrawId())
                    .eq(TourTournamentEntryPO::getPlayerId, entry.getPlayerId())
                    .one();

            if (existing != null) {
                existing.setSeed(entry.getSeed());
                this.updateById(existing);
            } else {
                this.save(entry);
            }
        }
    }

    public List<TourTournamentEntryPO> listByDrawIds(List<Long> drawIds) {
        if (CollectionUtils.isEmpty(drawIds)) {
            return List.of();
        }
        return this.lambdaQuery()
                .in(TourTournamentEntryPO::getDrawId, drawIds)
                .list();
    }
}
