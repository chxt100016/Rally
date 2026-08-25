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
 * 业务活动 court.court-create.activity.resolve-court-location：
 * 校验城市与区域编码在名录中存在，取回对应的中文名称。
 */
@Component
@RequiredArgsConstructor
public class CreateResolveCourtLocationActivity {

    private final LocationCatalogService locationCatalogService;

    public CourtLocation execute(String cityCode, String districtCode) {
        // A1 按城市编码查名录，未命中则报 COURT_CITY_NOT_FOUND
        LocationResult city = locationCatalogService.query(new LocationQuery(cityCode, LocationScopeEnum.CITY));
        Assert.isTrue(city.isHit(), BizErrorCode.COURT_CITY_NOT_FOUND);

        String districtName = null;
        // A2 填了区域编码时按区域编码查名录，未命中则报 COURT_DISTRICT_NOT_FOUND；
        // 没填时跳过，区域编码与区域名称都按空处理；不校验区域是否属于该城市
        if (StringUtils.isNotBlank(districtCode)) {
            LocationResult district = locationCatalogService.query(new LocationQuery(districtCode, LocationScopeEnum.DISTRICT));
            Assert.isTrue(district.isHit(), BizErrorCode.COURT_DISTRICT_NOT_FOUND);
            districtName = district.getName();
        }
        // A3 把城市编码、城市名称、区域编码、区域名称组成归属信息返回
        return new CourtLocation(cityCode, city.getName(), StringUtils.trimToNull(districtCode), districtName);
    }
}
