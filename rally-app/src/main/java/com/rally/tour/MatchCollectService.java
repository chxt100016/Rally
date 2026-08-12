package com.rally.tour;

import com.rally.domain.tour.repository.TourMatchCollectRepository;
import com.rally.domain.tour.model.MatchData;
import com.rally.tour.convert.MatchAppConvertMapper;
import com.rally.tour.model.Match;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class MatchCollectService {

    @Resource
    private TourMatchCollectRepository tourMatchCollectRepository;

    public void saveMatches(List<Match> matches) {
        if (CollectionUtils.isEmpty(matches)) return;
        List<MatchData> matchDataList = MatchAppConvertMapper.INSTANCE.toMatchDataList(matches);
        tourMatchCollectRepository.saveOrUpdateBatch(matchDataList);
    }
}
