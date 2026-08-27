package com.rally.meetup.activity;

import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.recap.model.ScoreAddCmd;
import com.rally.domain.recap.service.ScoreDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 add-score-record：在约球可复盘窗口内新增一盘比分。
 */
@Component
@RequiredArgsConstructor
public class AddScoreRecordActivity {

    private final MeetupDomainService meetupDomainService;

    private final ScoreDomainService scoreDomainService;

    public void execute(String recordedBy, ScoreAddCmd cmd) {
        // A1：发布者或活跃报名（包括 PENDING）可进入，并校验实际状态与截止时间。
        Meetup meetup = meetupDomainService.get(cmd.getMeetupId());
        meetup.assertReviewAvailable(recordedBy);

        /*
         * A2-A5：领域入口仅预检 meetupId + setNum，主分相等时拒绝；
         * 盘号、阵容、球员资格、快照命中和网球计分都不加强校验。
         * 它同时生成业务编号、补充可取得的球员快照并保存记录。
         */
        scoreDomainService.addScoreItem(meetup, recordedBy, cmd);
    }
}
