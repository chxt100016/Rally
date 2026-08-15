package com.rally.job;


import com.rally.tour.TourCollectFacade;
import com.rally.tour.parser.CollectType;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(name = "job.tour.enabled", havingValue = "true", matchIfMissing = false)
public class TourCollectJob {

    @Resource
    private TourCollectFacade tourCollectFacade;

    @Resource
    private Environment environment;

    /**
     * 以 LIVE 的五分钟频率统一触发比赛采集；其他阶段按枚举间隔使用绝对分钟取余。
     * LIVE 优先，避免整点的低频采集延迟实时比分。
     */
    @Scheduled(cron = "${job.tour.collect.live.cron}")
    public void matches() {
        collectMatchesAt(Instant.now());
    }

    void collectMatchesAt(Instant instant) {
        long epochMinute = instant.getEpochSecond() / 60;
        for (CollectType.Phase phase : List.of(
                CollectType.Phase.LIVE,
                CollectType.Phase.OOP,
                CollectType.Phase.DRAW)) {
            if (!phase.shouldRun(epochMinute)) continue;
            try {
                tourCollectFacade.matches(phase);
            } catch (RuntimeException e) {
                log.error("比赛采集阶段执行失败: phase={}", phase, e);
            }
        }
    }

    /** 每天凌晨4点采集排名，仅 wechat 环境执行 */
    @Scheduled(cron = "${job.tour.collect.rank.cron}")
    public void rank() {
        if (!List.of(environment.getActiveProfiles()).contains("wechat")) return;
        tourCollectFacade.rank();
    }
}
