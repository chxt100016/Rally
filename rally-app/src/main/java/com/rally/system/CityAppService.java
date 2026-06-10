package com.rally.system;

import com.rally.domain.system.CityConfig;
import com.rally.system.convert.CityAppConvertMapper;
import com.rally.system.model.CityDTO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 城市查询应用服务
 */
@Service
public class CityAppService {

    public List<CityDTO> listAll() {
        return CityAppConvertMapper.INSTANCE.toCityDTOList(CityConfig.allCity());
    }

    public List<CityDTO> listAvailable() {
        return CityAppConvertMapper.INSTANCE.toCityDTOList(CityConfig.listAvailable());
    }
}
