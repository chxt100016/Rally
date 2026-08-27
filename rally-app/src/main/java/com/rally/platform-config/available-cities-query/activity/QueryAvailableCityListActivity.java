package com.rally.platformconfig.availablecitiesquery.activity;

import com.rally.domain.system.CityConfig;
import com.rally.system.convert.CityAppConvertMapper;
import com.rally.system.model.CityDTO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 业务活动 query-available-city-list：组装当前已开通城市列表。
 */
@Component
public class QueryAvailableCityListActivity {

    public List<CityDTO> execute() {
        // A1/A2 由 CityConfig 保留原有配置默认值、半角逗号分割与 Set.copyOf 去重语义。
        // A3 未知编码映射出的 null 会由现有 MapStruct 列表转换原样保留。
        return CityAppConvertMapper.INSTANCE.toCityDTOList(CityConfig.listAvailable());
    }
}
