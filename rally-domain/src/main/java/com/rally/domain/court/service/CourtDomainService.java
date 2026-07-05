package com.rally.domain.court.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.domain.court.gateway.CourtRepository;
import com.rally.domain.court.model.CourtData;
import com.rally.domain.meetup.enums.CourtSelectModeEnum;
import com.rally.domain.meetup.gateway.MeetupRepository;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.utils.GeoUtils;
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

    private static final double MERGE_DISTANCE_METERS = 200.0;

    private final CourtRepository courtRepository;
    private final MeetupRepository meetupRepository;

    /**
     * 查找或创建球场（200m 范围内合并为同一场地）
     * @param name 球场名称
     * @param address 球场地址
     * @param lng 经度
     * @param lat 纬度
     * @param cityCode 城市编码
     * @param districtCode 区县编码（选填）
     * @param total 场地数量（选填）
     * @return 球场数据（已存在或新建）
     */
    public CourtData findOrCreate(String name, String address, Double lng, Double lat,
                                   String cityCode, String districtCode, Integer total) {
        // 1. 查询同城市所有球场
        List<CourtData> existingCourts = courtRepository.findByCityCode(cityCode);

        // 2. 200m 内视为同一场地，返回已有球场
        for (CourtData court : existingCourts) {
            double distance = GeoUtils.distance(lat, lng, court.getLat(), court.getLng()) * 1000;
            if (distance <= MERGE_DISTANCE_METERS) {
                log.info("球场合并：输入位置({}, {}) 与已有球场[{}]距离{}m，合并", lng, lat, court.getName(), String.format("%.1f", distance));
                return court;
            }
        }

        // 3. 无近邻，创建新球场
        CourtData newCourt = new CourtData();
        newCourt.setBizId(IdWorker.getIdStr());
        newCourt.setName(name);
        newCourt.setAddress(address);
        newCourt.setLng(lng);
        newCourt.setLat(lat);
        newCourt.setCityCode(cityCode);
        newCourt.setDistrictCode(districtCode);
        newCourt.setTotal(total);
        courtRepository.save(newCourt);
        log.info("新建球场：{}({},{})", name, lng, lat);
        return newCourt;
    }

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
