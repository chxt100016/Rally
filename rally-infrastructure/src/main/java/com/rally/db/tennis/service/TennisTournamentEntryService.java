package com.rally.db.tennis.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.tennis.entity.TennisTournamentEntryPO;
import com.rally.db.tennis.mapper.TennisTournamentEntryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class TennisTournamentEntryService extends ServiceImpl<TennisTournamentEntryMapper, TennisTournamentEntryPO> {

    @Transactional(rollbackFor = Exception.class)
    public void saveEntries(List<TennisTournamentEntryPO> entries) {
        for (TennisTournamentEntryPO entry : entries) {
            TennisTournamentEntryPO existing = this.lambdaQuery()
                    .eq(TennisTournamentEntryPO::getTournamentId, entry.getTournamentId())
                    .eq(TennisTournamentEntryPO::getYear, entry.getYear())
                    .eq(TennisTournamentEntryPO::getPlayerId, entry.getPlayerId())
                    .eq(TennisTournamentEntryPO::getDrawType, entry.getDrawType())
                    .one();

            if (existing != null) {
                existing.setDrawId(entry.getDrawId());
                existing.setSeed(entry.getSeed());
                this.updateById(existing);
            } else {
                this.save(entry);
            }
        }
    }
}
