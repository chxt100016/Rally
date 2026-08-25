package com.rally.court.activity;

import com.rally.court.model.CollectScope;
import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.court.gateway.CourtMapClient;
import com.rally.domain.system.enums.LocationScopeEnum;
import com.rally.domain.system.model.Location;
import com.rally.domain.system.model.LocationQuery;
import com.rally.domain.system.model.LocationResult;
import com.rally.domain.system.service.LocationCatalogService;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 业务活动 resolve-collect-scope：校验城市与地图凭据，解出本次要逐个检索的区县范围。
 */
@Component
@RequiredArgsConstructor
public class ResolveCollectScopeActivity {

    private final LocationCatalogService locationCatalogService;
    private final CourtMapClient courtMapClient;

    public CollectScope execute(String cityCode) {
        // A1 按城市编码查名录取城市名称，未命中则报 COURT_CITY_NOT_FOUND
        LocationResult city = locationCatalogService.query(new LocationQuery(cityCode, LocationScopeEnum.DISTRICTS_OF_CITY));
        Assert.isTrue(city.isHit(), BizErrorCode.COURT_CITY_NOT_FOUND);
        // A2 检查地图服务凭据已配置，未配置则报 COURT_MAP_NOT_CONFIGURED，一次请求都不发起
        Assert.isTrue(courtMapClient.configured(), BizErrorCode.COURT_MAP_NOT_CONFIGURED);
        // A3 按编码归属规则推导该城市下的全部区县，规则由名录承担
        List<Location> districts = city.getDistricts();

        CollectScope scope = new CollectScope();
        scope.setCityCode(cityCode);
        scope.setCityName(city.getName());
        // A4 区县为空时把城市编码本身作为唯一检索范围
        if (districts == null || districts.isEmpty()) {
            scope.setRegionCodes(List.of(cityCode));
            scope.setDistrictCount(0);
        } else {
            scope.setRegionCodes(districts.stream().map(Location::getCode).toList());
            scope.setDistrictCount(districts.size());
        }
        return scope;
    }
}
