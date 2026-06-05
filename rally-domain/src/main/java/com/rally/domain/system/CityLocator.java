package com.rally.domain.system;

import java.util.Arrays;
import java.util.List;

/**
 * 城市定位器：静态方法提供城市配置查询
 */
public class CityLocator {


    /**
     * 校验城市编码是否已开通
     * @param cityCode 城市编码
     * @return 匹配的城市编码，未开通返回 null
     */
    public static String validateCityCode(String cityCode) {
        if (cityCode == null || cityCode.isEmpty()) {
            return null;
        }
        List<String> openedCities = getOpenedCities();
        if (openedCities.isEmpty()) {
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
    public static List<String> getOpenedCities() {
        String str = SystemConfig.getString("meetup.city.opened_codes", "[]");
        try {
            return Arrays.asList(str.split(","));
        } catch (Exception e) {
            return List.of();
        }
    }
}
