package com.rally.court.activity;

import com.rally.domain.court.gateway.CourtRepository;
import com.rally.domain.meetup.enums.CourtSelectModeEnum;
import com.rally.domain.meetup.gateway.MeetupRepository;
import com.rally.domain.meetup.model.MeetupData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务活动 settle-court-heat：汇总昨日合格约球并累加关联球场活动热度。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettleCourtHeatActivity {

    private final MeetupRepository meetupRepository;
    private final CourtRepository courtRepository;

    public void execute() {
        // A1 以运行环境日期确定昨日完整自然日窗口
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime startTime = LocalDateTime.of(yesterday, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(yesterday, LocalTime.MAX);

        // A2 只保留已结束、通过地图或文本搜索选场且关联了球场的约球
        List<MeetupData> finishedMeetups = meetupRepository.listFinishedByTimeRange(startTime, endTime);
        if (finishedMeetups.isEmpty()) {
            log.info("前一天无结束的约球活动");
            return;
        }

        // A3 同一约球只为关联球场计数一次
        Map<String, Integer> courtIdCountMap = new HashMap<>();
        for (MeetupData meetup : finishedMeetups) {
            CourtSelectModeEnum courtSelectMode = meetup.getCourtSelectMode();
            String courtId = meetup.getCourtId();
            if (courtId != null
                    && (CourtSelectModeEnum.MAP.equals(courtSelectMode)
                    || CourtSelectModeEnum.TEXT.equals(courtSelectMode))) {
                courtIdCountMap.merge(courtId, 1, Integer::sum);
            }
        }
        if (courtIdCountMap.isEmpty()) {
            log.info("前一天结束的约球活动中无有效球场ID");
            return;
        }

        // A4 meetup_count 是聚合外统计投影，沿用现有统计写入端口逐球场原子累加
        courtRepository.batchIncrementMeetupCount(courtIdCountMap);
        log.info("完成统计前一天球场活动约球次数，更新{}个球场", courtIdCountMap.size());
    }
}
