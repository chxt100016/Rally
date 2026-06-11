package com.rally.domain.system;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.system.gateway.SysConfigLoader;
import com.rally.domain.system.model.Location;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 城市定位器：静态方法提供城市数据与配置查询
 */
@Component
public class CityConfig {

    /** 城市数据缓存 */
    public static Map<String, Location> cities = Map.of();
    /** 省份数据缓存 */
    public static Map<String, Location> districts = Map.of();

    public CityConfig(SysConfigLoader loader) {
        cities = loader.city();
        districts = loader.district();
    }


    /**
     * 断言城市编码已开通，未开通则抛出异常
     * @param cityCode 城市编码
     */
    public static void assertCityOpened(String cityCode) {
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
    }

    /**
     * 获取开通城市编码列表
     */
    public static List<String> getOpenedCities() {
        String str = SystemConfig.getString("meetup.city.opened_codes", "[]");
        try {
            return Arrays.asList(str.split(","));
        } catch (Exception e) {
            return List.of();
        }
    }

    public static List<Location> allCity() {
        return new ArrayList<>(cities.values());
    }


    /**
     * 获取已开通城市数据
     */
    public static List<Location> listAvailable() {
        Set<String> opened = Set.copyOf(getOpenedCities());
        return opened.stream().map(city -> cities.get(city)).collect(Collectors.toList());
    }

    public static String getCityName(String code) {
        return cities.get(code).getName();
    }

}
