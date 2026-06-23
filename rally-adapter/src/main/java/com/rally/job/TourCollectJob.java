package com.rally.job;


import com.rally.tour.TourCollectFacade;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(name = "job.tour.enabled", havingValue = "true", matchIfMissing = false)
public class TourCollectJob {

    @Resource
    private TourCollectFacade tourCollectFacade;

    @Resource
    private Environment environment;

    /** 每天凌晨2点采集当前进行中赛事签表 */
    @Scheduled(cron = "${job.tour.collect.draws.cron}")
    public void currentDraws() {
        tourCollectFacade.currentDraws();
    }

    /** 每小时采集比赛详情 */
    @Scheduled(cron = "${job.tour.collect.matches.cron}")
    public void currentMatch() {
        tourCollectFacade.oop();
    }

    /** 每分钟采集进行中比赛实时状态 */
    @Scheduled(cron = "${job.tour.collect.live.cron}")
    public void liveMatch() {
        tourCollectFacade.liveMatch();
    }

    /** 每天凌晨4点采集排名，仅 wechat 环境执行 */
    @Scheduled(cron = "${job.tour.collect.rank.cron}")
    public void rank() {
        if (!List.of(environment.getActiveProfiles()).contains("wechat")) return;
        tourCollectFacade.rank();
    }
}
