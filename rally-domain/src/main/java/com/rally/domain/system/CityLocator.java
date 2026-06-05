package com.rally.domain.system;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;

import java.util.Arrays;
import java.util.List;

/**
 * 城市定位器：静态方法提供城市配置查询
 */
public class CityLocator {

    /**
     * 断言城市编码已开通，未开通则抛出异常
     * @param cityCode 城市编码
     * @return 匹配的城市编码
     */
    public static String assertCityOpened(String cityCode) {
        if (cityCode == null || cityCode.isEmpty()) {
            throw new BusinessException(BizErrorCode.CITY_NOT_OPENED);
        }
        List<String> openedCities = getOpenedCities();
        if (openedCities.isEmpty()) {
            throw new BusinessException(BizErrorCode.CITY_NOT_OPENED);
        }
        boolean opened = openedCities.stream().anyMatch(cityCode::equals);
        if (!opened) {
            throw new BusinessException(BizErrorCode.CITY_NOT_OPENED);
        }
        return cityCode;
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
