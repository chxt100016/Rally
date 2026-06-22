package com.rally.db.score.gateway;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rally.db.score.entity.ScoreStatusPO;
import com.rally.db.score.service.ScoreStatusService;
import com.rally.domain.score.gateway.ScoreStatusGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ScoreStatusGatewayImpl implements ScoreStatusGateway {

    private final ScoreStatusService scoreStatusService;

    @Override
    public List<String> findPendingMeetupIds() {
        LambdaQueryWrapper<ScoreStatusPO> wrapper = new LambdaQueryWrapper<ScoreStatusPO>()
                .and(w -> w.isNull(ScoreStatusPO::getProcessedAt).or().apply("processed_version < score_version"));
        return scoreStatusService.list(wrapper).stream().map(ScoreStatusPO::getMeetupId).toList();
    }

    @Override
    public void bumpVersion(String meetupId) {
        ScoreStatusPO po = findByMeetupId(meetupId);
        if (po == null) {
            po = new ScoreStatusPO();
            po.setMeetupId(meetupId);
            po.setScoreVersion(1);
            po.setProcessedVersion(-1);
            scoreStatusService.save(po);
        } else {
            po.setScoreVersion(po.getScoreVersion() + 1);
            scoreStatusService.updateById(po);
        }
    }

    @Override
    public void markProcessed(String meetupId) {
        ScoreStatusPO po = findByMeetupId(meetupId);
        if (po != null) {
            po.setProcessedVersion(po.getScoreVersion());
            po.setProcessedAt(LocalDateTime.now());
            scoreStatusService.updateById(po);
        }
    }

    private ScoreStatusPO findByMeetupId(String meetupId) {
        return scoreStatusService.lambdaQuery().eq(ScoreStatusPO::getMeetupId, meetupId).one();
    }
}
