package com.rally.db.score.gateway;

import com.rally.db.score.repository.ScoreStatusRepository;
import com.rally.domain.score.gateway.ScoreStatusGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 批量评分幂等状态网关实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScoreStatusGatewayImpl implements ScoreStatusGateway {

    private final ScoreStatusRepository scoreStatusRepository;

    @Override
    public List<String> findPendingMeetupIds() {
        return scoreStatusRepository.findPendingMeetupIds();
    }

    @Override
    public void bumpVersion(String meetupId) {
        scoreStatusRepository.bumpVersion(meetupId);
    }

    @Override
    public void markProcessed(String meetupId) {
        scoreStatusRepository.markProcessed(meetupId);
    }
}
