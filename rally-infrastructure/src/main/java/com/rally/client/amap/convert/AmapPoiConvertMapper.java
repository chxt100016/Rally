package com.rally.client.amap.convert;

import com.rally.client.amap.model.AmapPoiResponse;
import com.rally.domain.court.model.CourtPoi;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 高德场所记录转领域场所记录
 */
@Mapper
public interface AmapPoiConvertMapper {

    AmapPoiConvertMapper INSTANCE = Mappers.getMapper(AmapPoiConvertMapper.class);

    @Mapping(target = "poiType", source = "type")
    @Mapping(target = "rectag", source = "business.rectag")
    @Mapping(target = "keytag", source = "business.keytag")
    @Mapping(target = "rating", source = "business.rating")
    @Mapping(target = "cost", source = "business.cost")
    @Mapping(target = "opentime", source = "business.opentime_week")
    @Mapping(target = "tel", source = "business.tel")
    CourtPoi toCourtPoi(AmapPoiResponse.AmapPoi poi);

    List<CourtPoi> toCourtPoiList(List<AmapPoiResponse.AmapPoi> pois);
}
