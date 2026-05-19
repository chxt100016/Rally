package com.rally.job;


import com.rally.tennis.TennisCollectFacade;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "job.tennis.enabled", havingValue = "true", matchIfMissing = false)
public class TennisCollectJob {

    @Resource
    private TennisCollectFacade tennisCollectFacade;

    /** 每天凌晨2点采集当前进行中赛事签表 */
    @Scheduled(cron = "${job.tennis.collect.draws.cron}")
    public void currentDraws() {
        tennisCollectFacade.currentDraws();
    }

    /** 每小时采集比赛详情 */
    @Scheduled(cron = "${job.tennis.collect.matches.cron}")
    public void currentMatch() {
        tennisCollectFacade.oop();
    }

    /** 每分钟采集进行中比赛实时状态 */
    @Scheduled(cron = "${job.tennis.collect.live.cron}")
    public void liveMatch() {
        tennisCollectFacade.liveMatch();
    }
}
