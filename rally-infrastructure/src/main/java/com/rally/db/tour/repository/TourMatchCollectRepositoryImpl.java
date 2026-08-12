package com.rally.db.tour.repository;

import com.rally.db.tour.convert.TourConvertMapper;
import com.rally.db.tour.entity.TourMatchPO;
import com.rally.db.tour.service.TourMatchService;
import com.rally.domain.tour.repository.TourMatchCollectRepository;
import com.rally.domain.tour.model.MatchData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TourMatchCollectRepositoryImpl implements TourMatchCollectRepository {

    private final TourMatchService tourMatchService;
    private static final TourConvertMapper MAPPER = TourConvertMapper.INSTANCE;

    @Override
    public List<MatchData> saveOrUpdateBatch(List<MatchData> matches) {
        List<TourMatchPO> matchPOs = MAPPER.toMatchPOList(matches);
        return MAPPER.toMatchDataList(tourMatchService.saveOrUpdateBatch(matchPOs));
    }
}
