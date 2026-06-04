package com.rally.client.geo;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.rally.domain.config.gateway.ConfigGateway;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 城市定位器：经纬度反查 city_code
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CityLocator {

    private final ConfigGateway configGateway;

    /**
     * 开通城市信息
     */
    @Data
    public static class CityInfo {
        private String cityCode;
        private String name;
        private Double lng;
        private Double lat;
    }

    /**
     * 校验城市编码是否已开通
     * @param cityCode 城市编码
     * @return 匹配的城市信息，未开通返回 null
     */
    public String validateCityCode(String cityCode) {
        if (cityCode == null || cityCode.isEmpty()) {
            return null;
        }
        List<String> openedCities = getOpenedCities();
        if (openedCities == null || openedCities.isEmpty()) {
            return null;
        }
        return openedCities.stream()
                .filter(cityCode::equals)
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取开通城市列表
     */
    public List<String> getOpenedCities() {
        String json = configGateway.getString("meetup.city.opened_codes", "[]");
        if (json == null || json.isEmpty() || "[]".equals(json)) {
            return List.of();
        }
        try {
            JSONArray array = JSON.parseArray(json);
            return array.toJavaList(String.class);
        } catch (Exception e) {
            log.error("解析开通城市配置失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 计算两点距离（米）- Haversine 公式
     */
    private double calculateDistance(double lng1, double lat1, double lng2, double lat2) {
        final int R = 6371000; // 地球半径（米）
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
