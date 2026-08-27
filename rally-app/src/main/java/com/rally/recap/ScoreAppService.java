package com.rally.recap;

import com.rally.domain.meetup.enums.MatchTypeEnum;
import com.rally.domain.meetup.model.PageDTO;
import com.rally.domain.recap.model.ScoreAddCmd;
import com.rally.domain.recap.model.ScoreDeleteCmd;
import com.rally.domain.recap.model.ScoreItemDTO;
import com.rally.domain.recap.model.ScoreListQueryCmd;
import com.rally.domain.recap.model.ScoreStatsDTO;
import com.rally.domain.recap.model.ScoreUpdateCmd;
import com.rally.meetup.activity.AddScoreRecordActivity;
import com.rally.meetup.activity.DeleteScoreRecordActivity;
import com.rally.meetup.activity.UpdateScoreRecordActivity;
import com.rally.personalprofile.myperformancestats.activity.CalculateMyPerformanceStatsActivity;
import com.rally.personalprofile.myscores.activity.QueryMyScorePageActivity;
import com.rally.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScoreAppService {

    private final AddScoreRecordActivity addScoreRecordActivity;
    private final UpdateScoreRecordActivity updateScoreRecordActivity;
    private final DeleteScoreRecordActivity deleteScoreRecordActivity;
    private final CalculateMyPerformanceStatsActivity calculateMyPerformanceStatsActivity;
    private final QueryMyScorePageActivity queryMyScorePageActivity;

    public void addScore(ScoreAddCmd cmd) {
        addScoreRecordActivity.execute(UserContext.get(), cmd);
    }

    public void updateScore(ScoreUpdateCmd cmd) {
        updateScoreRecordActivity.execute(UserContext.get(), cmd);
    }

    public void deleteScore(ScoreDeleteCmd cmd) {
        deleteScoreRecordActivity.execute(UserContext.get(), cmd);
    }

    public ScoreStatsDTO queryMyScoreStats(MatchTypeEnum matchType) {
        return calculateMyPerformanceStatsActivity.execute(UserContext.get(), matchType);
    }

    public PageDTO<ScoreItemDTO> queryMyScores(ScoreListQueryCmd cmd) {
        return queryMyScorePageActivity.execute(UserContext.get(), cmd);
    }
}
