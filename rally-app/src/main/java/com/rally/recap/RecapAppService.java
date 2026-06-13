package com.rally.recap;

import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.recap.enums.RecapOverallStatus;
import com.rally.domain.recap.model.ReviewSubmitCmd;
import com.rally.domain.recap.model.ScoreSubmitCmd;
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
     * 提交比分

     */
    public void submitScore(ScoreSubmitCmd cmd) {
        String userId = UserContext.get();
        // 1. 获取 Meetup 聚合根（含报名记录）
        Meetup meetup = meetupDomainService.get(cmd.getMeetupId());
        meetup.assertReviewAvailable(userId);

        // 2. 从 Meetup 冗余提取比赛日期和场地名称
        recapDomainService.submitScoreItems(meetup, userId, cmd.getScores(), cmd.getScoreVersion(),
                meetup.getData().getStartTime(), meetup.getData().getCourtName());
    }

    /**
     * 提交评价
     */
    public void submitReview(ReviewSubmitCmd cmd) {
        cmd.getReviews().forEach(item -> ReviewSubmitCmd.assertValidReviewValue(item.getType(), item.getValue()));
        String userId = UserContext.get();

        // 1. 获取 Meetup 聚合根（含报名记录）
        Meetup meetup = meetupDomainService.get(cmd.getMeetupId());
        meetup.assertReviewAvailable(userId);
        // 2. 提交评价
        recapDomainService.submitReviewItems(meetup, userId, cmd.getReviews());


    }
}
