package com.rally.system.convert;


import com.rally.domain.system.model.Location;
import com.rally.system.model.CityDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 城市 App 层转换器
 */
@Mapper
public interface CityAppConvertMapper {

    CityAppConvertMapper INSTANCE = Mappers.getMapper(CityAppConvertMapper.class);

    CityDTO toCityDTO(Location city);

    List<CityDTO> toCityDTOList(List<Location> cities);
}
