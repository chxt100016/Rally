package com.rally.meetup.activity;

import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.recap.service.ReviewDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 业务活动 complete-peer-review-coverage：按已保存评价覆盖范围推进本人报名状态。
 */
@Component
@RequiredArgsConstructor
public class CompletePeerReviewCoverageActivity {

    private final ReviewDomainService reviewDomainService;

    public void execute(Meetup meetup, String fromUserId) {
        /*
         * A1-A4：现有事务性领域入口在空评价批次下不会新增或覆盖评价项，只会：
         * 1. 短路 REVIEWED/SKIPPED；
         * 2. 计算除本人外的有效参与者；
         * 3. 必要时汇总已评价目标；
         * 4. 覆盖完整时以 JOINED 为条件更新为 REVIEWED 并记录操作时间。
         *
         * toUserId 在空批次下不参与任何评价写入，使用评价人自身编号以避免引入额外契约。
         */
        reviewDomainService.submitReviewItems(meetup, fromUserId, fromUserId, List.of());
    }
}
