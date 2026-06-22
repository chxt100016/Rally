package com.rally.db.tennis.gateway;

import com.rally.db.tennis.convert.TennisConvertMapper;
import com.rally.db.tennis.service.TennisDrawService;
import com.rally.domain.tennis.gateway.TennisDrawGateway;
import com.rally.domain.tennis.model.TennisDrawData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TennisDrawGatewayImpl implements TennisDrawGateway {

    private final TennisDrawService tennisDrawService;

    @Override
    public Long saveOrUpdate(String tournamentId, Integer year, String drawType, Integer size, Integer totalRounds) {
        return tennisDrawService.saveOrUpdate(tournamentId, year, drawType, size, totalRounds);
    }

    @Override
    public List<TennisDrawData> listByTournamentIds(List<String> tournamentIds) {
        return TennisConvertMapper.INSTANCE.toDrawDataList(tennisDrawService.listByTournamentIds(tournamentIds));
    }
}
