package com.rally.recap;

import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.recap.model.ReviewSubmitCmd;
import com.rally.domain.recap.model.ScoreAddCmd;
import com.rally.domain.recap.model.ScoreDeleteCmd;
import com.rally.domain.recap.model.ScoreUpdateCmd;
import com.rally.domain.recap.model.SkipReviewCmd;
import com.rally.domain.recap.service.RecapDomainService;
import com.rally.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 赛后收集应用服务（薄编排层）
 * <p>
 * 职责：
 * 1. 获取当前用户上下文
 * 2. 委托领域服务执行业务逻辑
 * 3. 捕获比分冲突异常，转换为状态码
 * 4. VO -> DTO 转换
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecapAppService {

    private final RecapDomainService recapDomainService;
    private final MeetupDomainService meetupDomainService;

    /**
     * 新增比分（一次一盘）
     */
    public void addScore(ScoreAddCmd cmd) {
        String userId = UserContext.get();
        Meetup meetup = meetupDomainService.get(cmd.getMeetupId());
        meetup.assertReviewAvailable(userId);
        recapDomainService.addScoreItem(meetup, userId, cmd, meetup.getData().getStartTime(), meetup.getData().getCourtName());
    }

    /**
     * 修改比分（一次一盘，bizId 定位 + version 乐观锁）
     */
    public void updateScore(ScoreUpdateCmd cmd) {
        String userId = UserContext.get();
        Meetup meetup = meetupDomainService.get(cmd.getMeetupId());
        meetup.assertReviewAvailable(userId);
        recapDomainService.updateScoreItem(meetup, userId, cmd, meetup.getData().getStartTime(), meetup.getData().getCourtName());
    }

    /**
     * 删除比分（一次一盘，bizId 定位）
     */
    public void deleteScore(ScoreDeleteCmd cmd) {
        String userId = UserContext.get();
        Meetup meetup = meetupDomainService.get(cmd.getMeetupId());
        meetup.assertReviewAvailable(userId);
        recapDomainService.deleteScoreItem(meetup, cmd.getBizId());
    }

    /**
     * 跳过评价（一键标记无需评价）
     */
    public void skipReview(SkipReviewCmd cmd) {
        String userId = UserContext.get();
        Meetup meetup = meetupDomainService.get(cmd.getMeetupId());
        meetup.assertCanReview();
        recapDomainService.skipReview(meetup, userId);
    }

    /**
     * 提交评价（一次评价一个用户）
     */
    public void submitReview(ReviewSubmitCmd cmd) {
        cmd.getReviews().forEach(item -> ReviewSubmitCmd.assertValidReviewValue(item.getType(), item.getValue()));
        String userId = UserContext.get();
        Meetup meetup = meetupDomainService.get(cmd.getMeetupId());
        meetup.assertReviewAvailable(userId);
        recapDomainService.submitReviewItems(meetup, userId, cmd.getToUserId(), cmd.getReviews());
    }
}
