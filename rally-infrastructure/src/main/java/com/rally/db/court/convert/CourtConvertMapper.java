package com.rally.db.court.convert;

import com.rally.db.court.entity.CourtPO;
import com.rally.domain.court.model.CourtData;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 球场 PO <-> Data 转换器
 */
@Mapper
public interface CourtConvertMapper {

    CourtConvertMapper INSTANCE = Mappers.getMapper(CourtConvertMapper.class);

    CourtData toCourtData(CourtPO po);

    CourtPO toCourtPO(CourtData data);

    List<CourtData> toCourtDataList(List<CourtPO> poList);
}
