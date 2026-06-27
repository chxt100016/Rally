package com.rally.job;

import com.rally.domain.meetup.gateway.MeetupRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 约球状态定时任务
 * - 状态兜底（凌晨 2:00）
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "job.meetup.enabled", havingValue = "true")
public class MeetupStatusJob {

    @Resource
    private MeetupRepository meetupRepository;

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
}
