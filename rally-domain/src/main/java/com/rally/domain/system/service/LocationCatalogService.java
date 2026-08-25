package com.rally.domain.system.service;

import com.rally.domain.system.CityConfig;
import com.rally.domain.system.enums.LocationScopeEnum;
import com.rally.domain.system.model.Location;
import com.rally.domain.system.model.LocationQuery;
import com.rally.domain.system.model.LocationResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 行政区划名录领域服务：按编码查名称，按城市编码推导下属区县。
 * 只给出预检结论，查不到即为未命中，不抛业务异常。
 */
@Service
public class LocationCatalogService {

    /** 直辖市：区县编码按前两位归属 */
    private static final Set<String> MUNICIPALITIES = Set.of("110000", "120000", "310000", "500000");
    private static final int CODE_LENGTH = 6;
    private static final int MUNICIPALITY_PREFIX_LENGTH = 2;
    private static final int CITY_PREFIX_LENGTH = 4;

    /** 城市编码 -> 下属区县，首次使用时按名录预建 */
    private volatile Map<String, List<Location>> districtsByCity;

    public LocationResult query(LocationQuery query) {
        if (query == null || query.getScope() == null || !isValidCode(query.getCode())) {
            return LocationResult.miss();
        }
        // R1 城市与区县分属两份名录，按 scope 决定查哪一份，两份互不回退
        return switch (query.getScope()) {
            case CITY -> queryCity(query.getCode());
            case DISTRICT -> queryDistrict(query.getCode());
            case DISTRICTS_OF_CITY -> queryDistrictsOfCity(query.getCode());
        };
    }

    private LocationResult queryCity(String code) {
        Location city = CityConfig.cities.get(code);
        // R2 编码在对应名录中找不到时结论为未命中
        return city == null ? LocationResult.miss() : LocationResult.of(city.getName());
    }

    private LocationResult queryDistrict(String code) {
        Location district = CityConfig.districts.get(code);
        // R2 编码在对应名录中找不到时结论为未命中
        return district == null ? LocationResult.miss() : LocationResult.of(district.getName());
    }

    private LocationResult queryDistrictsOfCity(String cityCode) {
        Location city = CityConfig.cities.get(cityCode);
        if (city == null) {
            // R2 城市编码本身未命中时不再推导
            return LocationResult.miss();
        }
        // R5 一个区县都推不出的城市给空列表，命中结论仍为真
        return LocationResult.of(city.getName(), districtsOf(cityCode));
    }

    /** R3 R4 R6 按编码前缀归属推导下属区县 */
    private List<Location> districtsOf(String cityCode) {
        Map<String, List<Location>> index = districtsByCity;
        if (index == null) {
            synchronized (this) {
                if (districtsByCity == null) {
                    districtsByCity = buildIndex();
                }
                index = districtsByCity;
            }
        }
        return index.getOrDefault(prefixOf(cityCode), List.of());
    }

    private Map<String, List<Location>> buildIndex() {
        Map<String, List<Location>> index = new HashMap<>();
        // R6 同一编码重复出现时只取第一条
        Set<String> seen = new LinkedHashSet<>();
        List<Location> all = new ArrayList<>(CityConfig.districts.values());
        for (Location district : all) {
            String code = district.getCode();
            if (!isValidCode(code) || !seen.add(code)) {
                continue;
            }
            // R3 直辖市按前两位、R4 其余城市按前四位，两种前缀都建索引
            index.computeIfAbsent(code.substring(0, MUNICIPALITY_PREFIX_LENGTH), k -> new ArrayList<>()).add(district);
            index.computeIfAbsent(code.substring(0, CITY_PREFIX_LENGTH), k -> new ArrayList<>()).add(district);
        }
        // R6 下属区县按区县编码升序给出
        for (List<Location> list : index.values()) {
            list.sort(Comparator.comparing(Location::getCode));
        }
        return index;
    }

    private static String prefixOf(String cityCode) {
        int length = MUNICIPALITIES.contains(cityCode) ? MUNICIPALITY_PREFIX_LENGTH : CITY_PREFIX_LENGTH;
        return cityCode.substring(0, length);
    }

    /** R2 编码为空白或长度不足六位时视为未命中 */
    private static boolean isValidCode(String code) {
        return StringUtils.isNotBlank(code) && code.trim().length() == CODE_LENGTH;
    }
}
