package com.rally.domain.court.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.domain.court.gateway.CourtRepository;
import com.rally.domain.court.model.CourtData;
import com.rally.domain.utils.GeoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 球场领域服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CourtDomainService {

    private static final double MERGE_DISTANCE_METERS = 200.0;

    private final CourtRepository courtRepository;

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
}
