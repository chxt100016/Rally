package com.rally.meetup.activity;

import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.recap.model.ScoreDeleteCmd;
import com.rally.domain.recap.service.ScoreDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 delete-score-record：在约球可复盘窗口内物理删除指定比分。
 */
@Component
@RequiredArgsConstructor
public class DeleteScoreRecordActivity {

    private final MeetupDomainService meetupDomainService;

    private final ScoreDomainService scoreDomainService;

    public void execute(String userId, ScoreDeleteCmd cmd) {
        // A1：发布者或活跃报名（包括 PENDING）可进入，并校验实际状态与截止时间。
        Meetup meetup = meetupDomainService.get(cmd.getMeetupId());
        meetup.assertReviewAvailable(userId);

        // A2-A3：不预读或校验记录人/版本，按 meetupId + bizId 直接物理删除，零行也成功。
        scoreDomainService.deleteScoreItem(meetup, cmd.getBizId());
    }
}
