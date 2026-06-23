package com.rally.db.tour.gateway;

import com.rally.db.tour.convert.TourConvertMapper;
import com.rally.db.tour.entity.TourMatchPO;
import com.rally.db.tour.service.TourMatchService;
import com.rally.db.tour.service.TourSetScoreService;
import com.rally.domain.tour.gateway.TourMatchCollectGateway;
import com.rally.domain.tour.model.MatchData;
import com.rally.domain.tour.model.SetScoreData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TourMatchCollectGatewayImpl implements TourMatchCollectGateway {

    private final TourMatchService tourMatchService;
    private final TourSetScoreService tourSetScoreService;
    private static final TourConvertMapper MAPPER = TourConvertMapper.INSTANCE;

    @Override
    public List<MatchData> saveOrUpdateBatch(List<MatchData> matches) {
        List<TourMatchPO> matchPOs = MAPPER.toMatchPOList(matches);
        tourMatchService.saveOrUpdateBatch(matchPOs);
        return MAPPER.toMatchDataList(matchPOs);
    }

    @Override
    public void saveSetScores(List<SetScoreData> setScores) {
        tourSetScoreService.saveOrUpdateBatch(MAPPER.toSetScorePOList(setScores));
    }
}
