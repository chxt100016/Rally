package com.rally.db.tennis.gateway;

import com.rally.db.tennis.convert.TennisConvertMapper;
import com.rally.db.tennis.entity.TennisTournamentEntryPO;
import com.rally.db.tennis.service.TennisTournamentEntryService;
import com.rally.domain.tennis.gateway.TennisEntryGateway;
import com.rally.domain.tennis.model.TournamentEntryData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TennisEntryGatewayImpl implements TennisEntryGateway {

    private final TennisTournamentEntryService tennisTournamentEntryService;
    private static final TennisConvertMapper MAPPER = TennisConvertMapper.INSTANCE;

    @Override
    public void saveEntries(List<TournamentEntryData> entries) {
        tennisTournamentEntryService.saveEntries(MAPPER.toEntryPOList(entries));
    }

    @Override
    public Map<String, Short> listSeedMapByDrawIds(List<Long> drawIds) {
        return tennisTournamentEntryService.listByDrawIds(drawIds)
                .stream()
                .filter(e -> e.getSeed() != null && e.getSeed() > 0)
                .collect(Collectors.toMap(e -> e.getDrawId() + ":" + e.getPlayerId(), TennisTournamentEntryPO::getSeed, (a, b) -> a));
    }

    @Override
    public List<TournamentEntryData> listByDrawIds(List<Long> drawIds) {
        return MAPPER.toEntryDataList(tennisTournamentEntryService.listByDrawIds(drawIds));
    }
}
