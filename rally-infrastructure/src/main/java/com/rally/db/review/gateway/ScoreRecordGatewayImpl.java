package com.rally.db.review.gateway;

import com.rally.db.review.convert.ReviewConvertMapper;
import com.rally.db.review.entity.ScoreRecordPO;
import com.rally.db.review.repository.ScoreRecordRepository;
import com.rally.domain.recap.gateway.ScoreRecordGateway;
import com.rally.domain.recap.model.ScoreRecordData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 比分记录网关实现（含乐观锁）
 */
@Component
@RequiredArgsConstructor
public class ScoreRecordGatewayImpl implements ScoreRecordGateway {

    private final ScoreRecordRepository scoreRecordRepository;
    private static final ReviewConvertMapper MAPPER = ReviewConvertMapper.INSTANCE;

    @Override
    public void insert(ScoreRecordData data) {
        ScoreRecordPO po = MAPPER.toScoreRecordPO(data);
        po.setVersion(0);
        scoreRecordRepository.save(po);
    }

    @Override
    public int updateWithLock(ScoreRecordData data) {
        ScoreRecordPO po = MAPPER.toScoreRecordPO(data);
        return scoreRecordRepository.updateWithLock(po);
    }

    @Override
    public ScoreRecordData findByBizId(String bizId) {
        ScoreRecordPO po = scoreRecordRepository.findByBizId(bizId);
        return MAPPER.toScoreRecordData(po);
    }

    @Override
    public List<ScoreRecordData> listByMeetupId(String rallyMeetupId) {
        return MAPPER.toScoreRecordDataList(scoreRecordRepository.listByMeetupId(rallyMeetupId));
    }

    @Override
    public int deleteByBizIdWithLock(String bizId, Integer version) {
        return scoreRecordRepository.deleteByBizIdWithLock(bizId, version);
    }
}
