package com.rally.db.score.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rally.db.score.entity.ScoreStatusPO;
import com.rally.db.score.mapper.ScoreStatusMapper;
import com.rally.db.score.service.ScoreStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 批量评分幂等状态表 Repository 门面
 */
@Repository
@RequiredArgsConstructor
public class ScoreStatusRepository {

    private final ScoreStatusMapper scoreStatusMapper;
    private final ScoreStatusService scoreStatusService;

    /**
     * 根据 meetupId 查询
     */
    public ScoreStatusPO findByMeetupId(String meetupId) {
        LambdaQueryWrapper<ScoreStatusPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScoreStatusPO::getMeetupId, meetupId);
        return scoreStatusMapper.selectOne(wrapper);
    }

    /**
     * 查询待处理的 meetup_id 列表
     * 条件：processed_at IS NULL OR processed_version < score_version
     */
    public List<String> findPendingMeetupIds() {
        // TODO: 需要关联 rally_meetup 表查询 status=finished 的记录
        // 暂时查询所有未处理或版本落后的记录
        LambdaQueryWrapper<ScoreStatusPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
                .isNull(ScoreStatusPO::getProcessedAt)
                .or()
                .apply("processed_version < score_version")
        );
        List<ScoreStatusPO> list = scoreStatusMapper.selectList(wrapper);
        return list.stream().map(ScoreStatusPO::getMeetupId).toList();
    }

    /**
     * 增加 score_version（无行则 INSERT，有行则 +1）
     */
    public void bumpVersion(String meetupId) {
        ScoreStatusPO po = findByMeetupId(meetupId);
        if (po == null) {
            // 无行则 INSERT
            po = new ScoreStatusPO();
            po.setMeetupId(meetupId);
            po.setScoreVersion(1);
            po.setProcessedVersion(-1);
            scoreStatusService.save(po);
        } else {
            // 有行则 +1
            po.setScoreVersion(po.getScoreVersion() + 1);
            scoreStatusService.updateById(po);
        }
    }

    /**
     * 标记处理完成
     */
    public void markProcessed(String meetupId) {
        ScoreStatusPO po = findByMeetupId(meetupId);
        if (po != null) {
            po.setProcessedVersion(po.getScoreVersion());
            po.setProcessedAt(LocalDateTime.now());
            scoreStatusService.updateById(po);
        }
    }
}
