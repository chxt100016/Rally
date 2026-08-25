package com.rally.court.activity;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.court.model.CourtLocation;
import com.rally.domain.system.enums.LocationScopeEnum;
import com.rally.domain.system.model.LocationQuery;
import com.rally.domain.system.model.LocationResult;
import com.rally.domain.system.service.LocationCatalogService;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 业务活动 court.court-update.activity.resolve-court-location：
 * 运营填了城市或区域编码时校验它在名录中存在，取回中文名称。两个编码都没填时不做任何查询。
 */
@Component
@RequiredArgsConstructor
public class UpdateResolveCourtLocationActivity {

    private final LocationCatalogService locationCatalogService;

    public CourtLocation execute(String cityCode, String districtCode) {
        String cityName = null;
        // A1 填了城市编码时按它查名录，未命中则报 COURT_CITY_NOT_FOUND
        if (StringUtils.isNotBlank(cityCode)) {
            LocationResult city = locationCatalogService.query(new LocationQuery(cityCode, LocationScopeEnum.CITY));
            Assert.isTrue(city.isHit(), BizErrorCode.COURT_CITY_NOT_FOUND);
            cityName = city.getName();
        }
        String districtName = null;
        // A2 填了区域编码时按它查名录，未命中则报 COURT_DISTRICT_NOT_FOUND；不校验区域是否属于该城市
        if (StringUtils.isNotBlank(districtCode)) {
            LocationResult district = locationCatalogService.query(new LocationQuery(districtCode, LocationScopeEnum.DISTRICT));
            Assert.isTrue(district.isHit(), BizErrorCode.COURT_DISTRICT_NOT_FOUND);
            districtName = district.getName();
        }
        // A3 组成归属信息返回，没填的项留空；两个编码都没填时四项全空，编排方据此判定不改归属
        if (cityName == null && districtName == null) {
            return null;
        }
        return new CourtLocation(StringUtils.trimToNull(cityCode), cityName, StringUtils.trimToNull(districtCode), districtName);
    }
}
