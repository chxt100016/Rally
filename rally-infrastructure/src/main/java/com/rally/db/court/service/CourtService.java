package com.rally.db.court.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.court.entity.CourtPO;
import com.rally.db.court.mapper.CourtMapper;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CourtService extends ServiceImpl<CourtMapper, CourtPO> {

    /**
     * 批量增加球场约球次数
     * @param courtIdCountMap 球场 bizId -> 增加次数
     */
    public void batchIncrementMeetupCount(Map<String, Integer> courtIdCountMap) {
        if (courtIdCountMap == null || courtIdCountMap.isEmpty()) {
            return;
        }
        courtIdCountMap.forEach((courtId, count) -> {
            if (count != null && count > 0) {
                baseMapper.incrementMeetupCount(courtId, count);
            }
        });
    }
}
