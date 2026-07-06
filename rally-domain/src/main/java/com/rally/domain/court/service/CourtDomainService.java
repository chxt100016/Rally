package com.rally.domain.court.service;

import com.rally.domain.court.gateway.CourtRepository;
import com.rally.domain.court.model.CourtData;
import com.rally.domain.meetup.enums.CourtSelectModeEnum;
import com.rally.domain.meetup.gateway.MeetupRepository;
import com.rally.domain.meetup.model.MeetupData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 球场领域服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CourtDomainService {

    private final CourtRepository courtRepository;
    private final MeetupRepository meetupRepository;

    /**
     * 根据 bizId 获取球场
     */
    public CourtData getByBizId(String bizId) {
        return courtRepository.findByBizId(bizId);
    }

    /**
     * 统计前一天结束的球场活动并更新约球次数
     */
    public void statisticsYesterdayMeetupCount() {
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            LocalDateTime startTime = LocalDateTime.of(yesterday, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(yesterday, LocalTime.MAX);
            log.info("开始统计前一天({})结束的球场活动约球次数", yesterday);
            List<MeetupData> finishedMeetups = meetupRepository.listFinishedByTimeRange(startTime, endTime);
            if (finishedMeetups.isEmpty()) {
                log.info("前一天无结束的约球活动");
                return;
            }
            Map<String, Integer> courtIdCountMap = new HashMap<>();
            for (MeetupData meetup : finishedMeetups) {
                CourtSelectModeEnum courtSelectMode = meetup.getCourtSelectMode();
                String courtId = meetup.getCourtId();
                if (courtId != null && (CourtSelectModeEnum.MAP.equals(courtSelectMode) || CourtSelectModeEnum.TEXT.equals(courtSelectMode))) {
                    courtIdCountMap.put(courtId, courtIdCountMap.getOrDefault(courtId, 0) + 1);
                }
            }
            if (courtIdCountMap.isEmpty()) {
                log.info("前一天结束的约球活动中无有效球场ID");
                return;
            }
            courtRepository.batchIncrementMeetupCount(courtIdCountMap);
            log.info("完成统计前一天球场活动约球次数，更新{}个球场", courtIdCountMap.size());
        } catch (Exception e) {
            log.error("统计前一天球场活动约球次数异常", e);
            throw e;
        }
    }
}
