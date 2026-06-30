package com.rally.db.review.repository;

import com.rally.db.review.convert.ReviewConvertMapper;
import com.rally.db.review.entity.ScoreRecordPO;
import com.rally.db.review.service.ScoreRecordService;
import com.rally.domain.recap.gateway.ScoreRepository;
import com.rally.domain.recap.model.ScoreRecordData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ScoreRepositoryImpl implements ScoreRepository {

    private final ScoreRecordService scoreRecordService;

    private static final ReviewConvertMapper MAPPER = ReviewConvertMapper.INSTANCE;

    @Override
    public boolean existsByMeetupAndSet(String meetupId, Integer setNumber) {
        return scoreRecordService.lambdaQuery().eq(ScoreRecordPO::getRallyMeetupId, meetupId).eq(ScoreRecordPO::getSetNumber, setNumber).count() > 0;
    }

    @Override
    public ScoreRecordData findByBizId(String meetupId, String bizId) {
        ScoreRecordPO po = scoreRecordService.lambdaQuery().eq(ScoreRecordPO::getRallyMeetupId, meetupId).eq(ScoreRecordPO::getBizId, bizId).one();
        return po == null ? null : MAPPER.toScoreRecordData(po);
    }

    @Override
    public void addScore(ScoreRecordData data) {
        scoreRecordService.save(MAPPER.toScoreRecordPO(data));
    }

    @Override
    public void updateScore(ScoreRecordData data) {
        scoreRecordService.lambdaUpdate().eq(ScoreRecordPO::getRallyMeetupId, data.getRallyMeetupId()).eq(ScoreRecordPO::getBizId, data.getBizId()).update(MAPPER.toScoreRecordPO(data));
    }

    @Override
    public void deleteScore(String meetupId, String bizId) {
        scoreRecordService.lambdaUpdate().eq(ScoreRecordPO::getRallyMeetupId, meetupId).eq(ScoreRecordPO::getBizId, bizId).remove();
    }

    @Override
    public List<ScoreRecordData> listScoresByMeetup(String meetupId) {
        return MAPPER.toScoreRecordDataList(scoreRecordService.lambdaQuery().eq(ScoreRecordPO::getRallyMeetupId, meetupId).orderByAsc(ScoreRecordPO::getSetNumber).list());
    }

    @Override
    public List<ScoreRecordData> listScoresByUserId(String userId) {
        return MAPPER.toScoreRecordDataList(scoreRecordService.lambdaQuery().and(w -> w.eq(ScoreRecordPO::getSideAPlayer1, userId).or().eq(ScoreRecordPO::getSideAPlayer2, userId).or().eq(ScoreRecordPO::getSideBPlayer1, userId).or().eq(ScoreRecordPO::getSideBPlayer2, userId)).orderByDesc(ScoreRecordPO::getBizId).list());
    }
}
