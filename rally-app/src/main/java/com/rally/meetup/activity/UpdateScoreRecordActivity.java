package com.rally.meetup.activity;

import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.recap.model.ScoreUpdateCmd;
import com.rally.domain.recap.service.ScoreDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 update-score-record：在约球可复盘窗口内修正一盘比分。
 */
@Component
@RequiredArgsConstructor
public class UpdateScoreRecordActivity {

    private final MeetupDomainService meetupDomainService;

    private final ScoreDomainService scoreDomainService;

    public void execute(String recordedBy, ScoreUpdateCmd cmd) {
        // A1：发布者或活跃报名（包括 PENDING）可进入，并校验实际状态与截止时间。
        Meetup meetup = meetupDomainService.get(cmd.getMeetupId());
        meetup.assertReviewAvailable(recordedBy);

        /*
         * A2-A4：领域入口按 meetupId + bizId 读取并在内存比较版本，
         * 随后重建快照并按非空字段更新；更新 SQL 不带版本、不检查影响行数。
         */
        scoreDomainService.updateScoreItem(meetup, recordedBy, cmd);
    }
}
