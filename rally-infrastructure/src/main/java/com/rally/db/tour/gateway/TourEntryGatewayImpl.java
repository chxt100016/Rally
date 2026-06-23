package com.rally.db.tour.gateway;

import com.rally.db.tour.convert.TourConvertMapper;
import com.rally.db.tour.entity.TourTournamentEntryPO;
import com.rally.db.tour.service.TourTournamentEntryService;
import com.rally.domain.tour.gateway.TourEntryGateway;
import com.rally.domain.tour.model.TournamentEntryData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TourEntryGatewayImpl implements TourEntryGateway {

    private final TourTournamentEntryService tourTournamentEntryService;
    private static final TourConvertMapper MAPPER = TourConvertMapper.INSTANCE;

    @Override
    public void saveEntries(List<TournamentEntryData> entries) {
        tourTournamentEntryService.saveEntries(MAPPER.toEntryPOList(entries));
    }

    @Override
    public Map<String, Short> listSeedMapByDrawIds(List<Long> drawIds) {
        return tourTournamentEntryService.listByDrawIds(drawIds)
                .stream()
                .filter(e -> e.getSeed() != null && e.getSeed() > 0)
                .collect(Collectors.toMap(e -> e.getDrawId() + ":" + e.getPlayerId(), TourTournamentEntryPO::getSeed, (a, b) -> a));
    }

    @Override
    public List<TournamentEntryData> listByDrawIds(List<Long> drawIds) {
        return MAPPER.toEntryDataList(tourTournamentEntryService.listByDrawIds(drawIds));
    }
}
