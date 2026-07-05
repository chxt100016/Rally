package com.rally.job;

import com.rally.domain.court.service.CourtDomainService;
import com.rally.domain.meetup.gateway.MeetupRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 约球状态定时任务
 * - 状态兜底（凌晨 2:00）
 * - 球场约球次数统计（凌晨 3:00）
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "job.meetup.enabled", havingValue = "true")
public class MeetupStatusJob {

    @Resource
    private MeetupRepository meetupRepository;

    @Resource
    private CourtDomainService courtDomainService;

    /**
     * 约球状态兜底（凌晨 2:00）
     * 将已过 end_time 但状态仍为 OPEN 的约球批量更新为 FINISHED
     */
    @Scheduled(cron = "${job.meetup.status.cron:0 0 2 * * ?}")
    public void updateFinishedStatus() {
        log.info("开始执行约球状态兜底任务");
        try {
            int affected = meetupRepository.batchUpdateToFinished();
            log.info("约球状态兜底任务完成，更新 {} 条记录", affected);
        } catch (Exception e) {
            log.error("约球状态兜底任务异常", e);
        }
    }

    /**
     * 球场约球次数统计（凌晨 3:00）
     * 统计前一天结束的球场活动约球次数
     */
    @Scheduled(cron = "${job.meetup.court-statistics.cron:0 0 3 * * ?}")
    public void statisticsCourtMeetupCount() {
        log.info("开始执行球场约球次数统计任务");
        try {
            courtDomainService.statisticsYesterdayMeetupCount();
            log.info("球场约球次数统计任务完成");
        } catch (Exception e) {
            log.error("球场约球次数统计任务异常", e);
        }
    }
}
