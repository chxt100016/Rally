package com.rally.job;

import com.rally.domain.meetup.gateway.MeetupRepository;
import com.rally.domain.meetup.gateway.NearbyRepository;
import com.rally.domain.system.CityConfig;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 约球状态定时任务
 * - 状态兜底（凌晨 2:00）
 * - GEO 一致性（每 6 小时）
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "job.meetup.enabled", havingValue = "true")
public class MeetupStatusJob {

    @Resource
    private MeetupRepository meetupRepository;

    @Resource
    private NearbyRepository nearbyRepository;

    /**
     * 约球状态兜底（凌晨 2:00）
     * 将已过 end_time 但状态仍为 OPEN/FULL 的约球批量更新为 FINISHED
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
     * Redis GEO 一致性校验（每 6 小时）
     * 校验 GEO 集合与 MySQL 活跃约球的一致性
     */
    @Scheduled(cron = "${job.meetup.geo.cron:0 0 */6 * * ?}")
    public void syncGeoData() {
        log.info("开始执行 GEO 一致性校验任务");
        try {
            List<String> cities = CityConfig.getOpenedCities();
            if (cities == null || cities.isEmpty()) {
                log.info("无开通城市，跳过 GEO 校验");
                return;
            }

            for (String city : cities) {
                syncCityGeoData(city);
            }

            log.info("GEO 一致性校验任务完成");
        } catch (Exception e) {
            log.error("GEO 一致性校验任务异常", e);
        }
    }

    /**
     * 同步单个城市的 GEO 数据
     */
    private void syncCityGeoData(String cityCode) {
        // 1. 获取 MySQL 中的活跃约球 ID
        List<String> mysqlIds = meetupRepository.listActiveIds(cityCode);

        // 2. 获取 Redis GEO 中的成员
        Set<String> geoIds = nearbyRepository.members(cityCode);

        // 3. 缺漏补齐：MySQL 有、GEO 无
        for (String meetupId : mysqlIds) {
            if (!geoIds.contains(meetupId)) {
                // 需要重新添加到 GEO
                // TODO: 需要从 MySQL 获取经纬度
                log.warn("GEO 缺漏: cityCode={}, meetupId={}", cityCode, meetupId);
            }
        }

        // 4. 多余清理：GEO 有、MySQL 无
        for (String meetupId : geoIds) {
            if (!mysqlIds.contains(meetupId)) {
                nearbyRepository.remove(cityCode, meetupId);
                log.info("GEO 清理: cityCode={}, meetupId={}", cityCode, meetupId);
            }
        }

        log.debug("城市 {} GEO 校验完成: MySQL={}, Redis={}, 缺漏={}, 多余={}",
                cityCode, mysqlIds.size(), geoIds.size(),
                mysqlIds.stream().filter(id -> !geoIds.contains(id)).count(),
                geoIds.stream().filter(id -> !mysqlIds.contains(id)).count());
    }
}
