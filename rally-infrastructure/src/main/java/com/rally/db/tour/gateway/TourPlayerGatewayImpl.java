package com.rally.db.tour.gateway;

import com.rally.db.tour.convert.TourConvertMapper;
import com.rally.db.tour.service.TourPlayerService;
import com.rally.domain.tour.gateway.TourPlayerGateway;
import com.rally.domain.tour.model.PlayerData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TourPlayerGatewayImpl implements TourPlayerGateway {

    private final TourPlayerService tourPlayerService;
    private static final TourConvertMapper MAPPER = TourConvertMapper.INSTANCE;

    @Override
    public void saveOrUpdateBatch(List<PlayerData> players) {
        tourPlayerService.saveOrUpdateBatch(MAPPER.toPlayerPOList(players));
    }

    @Override
    public List<PlayerData> listByTourOrderByRank(String tour) {
        return MAPPER.toPlayerDataList(tourPlayerService.lambdaQuery()
                .eq(com.rally.db.tour.entity.TourPlayerPO::getTour, tour)
                .isNotNull(com.rally.db.tour.entity.TourPlayerPO::getRank)
                .orderByAsc(com.rally.db.tour.entity.TourPlayerPO::getRank)
                .list());
    }

    @Override
    public List<PlayerData> listByPlayerIds(List<String> playerIds) {
        return MAPPER.toPlayerDataList(tourPlayerService.listByPlayerIds(playerIds));
    }
}
