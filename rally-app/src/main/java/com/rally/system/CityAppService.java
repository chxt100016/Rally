package com.rally.system;

import com.rally.platformconfig.availablecitiesquery.activity.QueryAvailableCityListActivity;
import com.rally.platformconfig.citycatalogquery.activity.QueryCityCatalogActivity;
import com.rally.system.model.CityDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 城市查询应用服务
 */
@Service
@RequiredArgsConstructor
public class CityAppService {

    private final QueryCityCatalogActivity queryCityCatalogActivity;
    private final QueryAvailableCityListActivity queryAvailableCityListActivity;

    public List<CityDTO> listAll() {
        return queryCityCatalogActivity.execute();
    }

    public List<CityDTO> listAvailable() {
        return queryAvailableCityListActivity.execute();
    }
}
