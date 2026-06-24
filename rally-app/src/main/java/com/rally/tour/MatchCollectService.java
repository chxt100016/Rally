package com.rally.tour;

import com.rally.client.tourtv.model.MatchesResponse;
import com.rally.domain.tour.repository.TourMatchCollectRepository;
import com.rally.domain.tour.model.MatchData;
import com.rally.domain.tour.model.SetScoreData;
import com.rally.tour.convert.MatchAppConvertMapper;
import com.rally.tour.model.Match;
import com.rally.tour.model.SetScore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MatchCollectService {

    @Resource
    private TourMatchCollectRepository tourMatchCollectRepository;

    public int collect(List<MatchesResponse.MatchInfo> matches) {
        if (CollectionUtils.isEmpty(matches)) return 0;
        List<Match> data = matches.stream().map(MatchAppConvertMapper.INSTANCE::toMatch).toList();
        this.saveMatches(data);
        return data.size();
    }

    public void saveMatches(List<Match> matches) {
        if (CollectionUtils.isEmpty(matches)) return;
        List<MatchData> matchDataList = MatchAppConvertMapper.INSTANCE.toMatchDataList(matches);
        List<MatchData> savedMatches = tourMatchCollectRepository.saveOrUpdateBatch(matchDataList);

        Map<String, Long> matchKeyToId = savedMatches.stream()
                .filter(m -> m.getTourMatchId() != null && m.getMatchId() != null)
                .collect(Collectors.toMap(m -> m.getMatchId() + "|" + m.getDrawId(), MatchData::getTourMatchId, (a, b) -> a));

        saveSetScores(matches, matchKeyToId);
    }

    private void saveSetScores(List<Match> matches, Map<String, Long> matchKeyToId) {
        List<SetScoreData> allSetScores = new ArrayList<>();
        for (Match match : matches) {
            if (CollectionUtils.isEmpty(match.getSets()) || match.getMatchId() == null) continue;
            Long tourMatchId = matchKeyToId.get(match.getMatchId() + "|" + match.getDrawId());
            if (tourMatchId == null) continue;
            for (SetScore setScore : match.getSets()) {
                SetScoreData data = new SetScoreData();
                data.setTourMatchId(tourMatchId);
                data.setSetNumber(setScore.getSetNumber());
                data.setP1Games(setScore.getP1Games());
                data.setP2Games(setScore.getP2Games());
                data.setP1Tiebreak(setScore.getP1Tiebreak());
                data.setP2Tiebreak(setScore.getP2Tiebreak());
                allSetScores.add(data);
            }
        }
        tourMatchCollectRepository.saveSetScores(allSetScores);
    }
}
