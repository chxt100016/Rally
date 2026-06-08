package com.rally.db.tennis.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.tennis.entity.TennisTournamentEntryPO;
import com.rally.db.tennis.mapper.TennisTournamentEntryMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
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
                    .eq(TennisTournamentEntryPO::getDrawId, entry.getDrawId())
                    .eq(TennisTournamentEntryPO::getPlayerId, entry.getPlayerId())
                    .one();

            if (existing != null) {
                existing.setSeed(entry.getSeed());
                this.updateById(existing);
            } else {
                this.save(entry);
            }
        }
    }

    public List<TennisTournamentEntryPO> listByDrawIds(List<Long> drawIds) {
        if (CollectionUtils.isEmpty(drawIds)) {
            return List.of();
        }
        return this.lambdaQuery()
                .in(TennisTournamentEntryPO::getDrawId, drawIds)
                .list();
    }
}
