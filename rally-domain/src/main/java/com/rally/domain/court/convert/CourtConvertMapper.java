package com.rally.domain.court.convert;

import com.rally.domain.court.model.CourtDTO;
import com.rally.domain.court.model.CourtData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 球场领域对象转换器
 */
@Mapper
public interface CourtConvertMapper {

    CourtConvertMapper INSTANCE = Mappers.getMapper(CourtConvertMapper.class);

    @Mapping(target = "courtId", source = "bizId")
    CourtDTO toDTO(CourtData data);

    List<CourtDTO> toDTOList(List<CourtData> dataList);
}
