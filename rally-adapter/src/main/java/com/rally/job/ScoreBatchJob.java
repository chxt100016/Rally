package com.rally.job;

import com.rally.domain.score.gateway.ScoreManager;
import com.rally.domain.score.gateway.ScoreStatusGateway;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 评分批量计算定时任务
 * 凌晨 2:10 执行（在约球状态兜底之后），扫描待处理的 finished 约球进行评分重算
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "job.score.enabled", havingValue = "true")
public class ScoreBatchJob {

    @Resource
    private ScoreManager scoreManager;

    @Resource
    private ScoreStatusGateway scoreStatusGateway;

    /**
     * 评分批量计算（凌晨 2:10）
     * 扫描待处理的 finished 约球，逐场调用 ScoreManager.recalc 进行评分重算
     */
    @Scheduled(cron = "${job.score.cron:0 10 2 * * ?}")
    public void run() {
        log.info("开始执行评分批量计算任务");
        try {
            // 1. 找待处理 meetup
            List<String> pendingMeetupIds = scoreStatusGateway.findPendingMeetupIds();
            log.info("待处理约球数量: {}", pendingMeetupIds.size());

            if (pendingMeetupIds.isEmpty()) {
                log.info("无待处理约球，跳过");
                return;
            }

            // 2. 逐场调用 recalc
            int successCount = 0;
            int failCount = 0;
            for (String meetupId : pendingMeetupIds) {
                try {
                    scoreManager.recalc(meetupId);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    log.error("单场评分计算失败，meetupId={}", meetupId, e);
                }
            }

            log.info("评分批量计算任务完成，成功={}, 失败={}", successCount, failCount);
        } catch (Exception e) {
            log.error("评分批量计算任务异常", e);
        }
    }
}
