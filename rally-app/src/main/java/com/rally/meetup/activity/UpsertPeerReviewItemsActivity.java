package com.rally.meetup.activity;

import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.recap.model.ReviewSubmitCmd;
import com.rally.domain.recap.service.ReviewDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 upsert-peer-review-items：校验并保存当前用户对单一目标的同场评价项。
 */
@Component
@RequiredArgsConstructor
public class UpsertPeerReviewItemsActivity {

    private final MeetupDomainService meetupDomainService;

    private final ReviewDomainService reviewDomainService;
    private final CompletePeerReviewCoverageActivity completePeerReviewCoverageActivity;

    public void execute(String fromUserId, ReviewSubmitCmd cmd) {
        // A1：沿用现有类型分档校验；TAG 只拒绝 null。
        // reviews 为 null 或包含 null 项时保留旧入口的系统异常语义，空列表可成功继续。
        cmd.getReviews().forEach(item ->
                ReviewSubmitCmd.assertValidReviewValue(item.getType(), item.getValue()));

        // A2：PENDING 与其他活跃报名一样可通过资格校验，再核实阶段与截止时间。
        Meetup meetup = meetupDomainService.get(cmd.getMeetupId());
        meetup.assertReviewAvailable(fromUserId);

        // A3-A5：仓储按列表顺序逐项查找并 upsert，批内重复维度由后项覆盖前项。
        // 目标用户仅作为唯一维度之一，当前不校验账户、同场资格或是否本人。
        reviewDomainService.submitReviewItems(
                meetup,
                fromUserId,
                cmd.getToUserId(),
                cmd.getReviews());
        completePeerReviewCoverageActivity.execute(meetup, fromUserId);
    }
}
