package com.rally.db.tour.gateway;

import com.rally.db.tour.convert.TourConvertMapper;
import com.rally.db.tour.service.TourDrawService;
import com.rally.domain.tour.gateway.TourDrawGateway;
import com.rally.domain.tour.model.TourDrawData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TourDrawGatewayImpl implements TourDrawGateway {

    private final TourDrawService tourDrawService;

    @Override
    public Long saveOrUpdate(String tournamentId, Integer year, String drawType, Integer size, Integer totalRounds) {
        return tourDrawService.saveOrUpdate(tournamentId, year, drawType, size, totalRounds);
    }

    @Override
    public List<TourDrawData> listByTournamentIds(List<String> tournamentIds) {
        return TourConvertMapper.INSTANCE.toDrawDataList(tourDrawService.listByTournamentIds(tournamentIds));
    }
}
