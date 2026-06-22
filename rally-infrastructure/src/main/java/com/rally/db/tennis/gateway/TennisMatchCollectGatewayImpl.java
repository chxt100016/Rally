package com.rally.db.tennis.gateway;

import com.rally.db.tennis.convert.TennisConvertMapper;
import com.rally.db.tennis.entity.TennisMatchPO;
import com.rally.db.tennis.service.TennisMatchService;
import com.rally.db.tennis.service.TennisSetScoreService;
import com.rally.domain.tennis.gateway.TennisMatchCollectGateway;
import com.rally.domain.tennis.model.MatchData;
import com.rally.domain.tennis.model.SetScoreData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TennisMatchCollectGatewayImpl implements TennisMatchCollectGateway {

    private final TennisMatchService tennisMatchService;
    private final TennisSetScoreService tennisSetScoreService;
    private static final TennisConvertMapper MAPPER = TennisConvertMapper.INSTANCE;

    @Override
    public List<MatchData> saveOrUpdateBatch(List<MatchData> matches) {
        List<TennisMatchPO> matchPOs = MAPPER.toMatchPOList(matches);
        tennisMatchService.saveOrUpdateBatch(matchPOs);
        return MAPPER.toMatchDataList(matchPOs);
    }

    @Override
    public void saveSetScores(List<SetScoreData> setScores) {
        tennisSetScoreService.saveOrUpdateBatch(MAPPER.toSetScorePOList(setScores));
    }
}
