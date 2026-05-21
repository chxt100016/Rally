package com.rally.tennis;

import com.rally.client.tennistv.model.MatchesResponse;
import com.rally.db.tennis.entity.TennisMatchPO;
import com.rally.db.tennis.entity.TennisSetScorePO;
import com.rally.db.tennis.repository.TennisMatchRepository;
import com.rally.db.tennis.repository.TennisSetScoreRepository;
import com.rally.tennis.convert.MatchAppConvertMapper;
import com.rally.tennis.model.Match;
import com.rally.tennis.model.SetScore;
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
    private TennisMatchRepository tennisMatchRepository;

    @Resource
    private TennisSetScoreRepository tennisSetScoreRepository;


    public int collect(List<MatchesResponse.MatchInfo> matches) {
        if (CollectionUtils.isEmpty(matches)) {
            return 0;
        }
        List<Match> data = matches.stream()
                .map(MatchAppConvertMapper.INSTANCE::toMatch)
                .toList();
        this.saveMatches(data);
        return data.size();
    }


    public void saveMatches(List<Match> matches) {
        if (CollectionUtils.isEmpty(matches)) {
            return;
        }
        List<TennisMatchPO> matchPOs = MatchAppConvertMapper.INSTANCE.toMatchPOList(matches);
        // saveOrUpdateBatch 内部会通过 peek 将数据库自增 id 回填到 matchPOs
        tennisMatchRepository.saveOrUpdateBatch(matchPOs);

        // 构建 matchId|drawId → tennis_match.id 映射，供盘分写入时关联
        Map<String, Long> matchKeyToId = matchPOs.stream()
                .filter(m -> m.getId() != null && m.getMatchId() != null)
                .collect(Collectors.toMap(uniqueKey -> uniqueKey.getMatchId() + "|" + uniqueKey.getDrawId(),
                        TennisMatchPO::getId, (a, b) -> a));

        saveSetScores(matches, matchKeyToId);
    }




    private void saveSetScores(List<Match> matches, Map<String, Long> matchKeyToId) {
        List<TennisSetScorePO> allSetScores = new ArrayList<>();
        for (Match match : matches) {
            if (CollectionUtils.isEmpty(match.getSets()) || match.getMatchId() == null) {
                continue;
            }
            Long tennisMatchId = matchKeyToId.get(match.getMatchId() + "|" + match.getDrawId());
            if (tennisMatchId == null) {
                continue;
            }
            for (SetScore setScore : match.getSets()) {
                TennisSetScorePO po = new TennisSetScorePO();
                po.setTennisMatchId(tennisMatchId);
                po.setSetNumber(setScore.getSetNumber());
                po.setP1Games(setScore.getP1Games());
                po.setP2Games(setScore.getP2Games());
                po.setP1Tiebreak(setScore.getP1Tiebreak());
                po.setP2Tiebreak(setScore.getP2Tiebreak());
                allSetScores.add(po);
            }
        }
        tennisSetScoreRepository.saveOrUpdateBatch(allSetScores);
    }

}
