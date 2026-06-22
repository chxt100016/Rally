package com.rally.db.tennis.gateway;

import com.rally.db.tennis.convert.TennisConvertMapper;
import com.rally.db.tennis.service.TennisPlayerService;
import com.rally.domain.tennis.gateway.TennisPlayerGateway;
import com.rally.domain.tennis.model.PlayerData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TennisPlayerGatewayImpl implements TennisPlayerGateway {

    private final TennisPlayerService tennisPlayerService;
    private static final TennisConvertMapper MAPPER = TennisConvertMapper.INSTANCE;

    @Override
    public void saveOrUpdateBatch(List<PlayerData> players) {
        tennisPlayerService.saveOrUpdateBatch(MAPPER.toPlayerPOList(players));
    }

    @Override
    public List<PlayerData> listByTourOrderByRank(String tour) {
        return MAPPER.toPlayerDataList(tennisPlayerService.lambdaQuery()
                .eq(com.rally.db.tennis.entity.TennisPlayerPO::getTour, tour)
                .isNotNull(com.rally.db.tennis.entity.TennisPlayerPO::getRank)
                .orderByAsc(com.rally.db.tennis.entity.TennisPlayerPO::getRank)
                .list());
    }

    @Override
    public List<PlayerData> listByPlayerIds(List<String> playerIds) {
        return MAPPER.toPlayerDataList(tennisPlayerService.listByPlayerIds(playerIds));
    }
}
