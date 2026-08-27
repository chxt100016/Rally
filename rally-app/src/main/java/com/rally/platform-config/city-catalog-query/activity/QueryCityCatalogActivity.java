package com.rally.platformconfig.citycatalogquery.activity;

import com.rally.domain.system.CityConfig;
import com.rally.system.convert.CityAppConvertMapper;
import com.rally.system.model.CityDTO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 业务活动 query-city-catalog：组装随当前版本发布的完整城市名录。
 */
@Component
public class QueryCityCatalogActivity {

    public List<CityDTO> execute() {
        // A1 复制 city.json 启动缓存的 Map values，保留普通映射的无序语义。
        // A2 沿用现有转换器原样映射 code/name/initials/pinyin，空名录返回空列表。
        return CityAppConvertMapper.INSTANCE.toCityDTOList(CityConfig.allCity());
    }
}
