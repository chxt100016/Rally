package com.rally.domain.tour.convert;

import com.rally.domain.tour.model.SetScoreData;
import com.rally.domain.tour.model.SetScoreVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 盘分 Data → VO 转换器
 */
@Mapper
public interface MatchConvertMapper {

    MatchConvertMapper INSTANCE = Mappers.getMapper(MatchConvertMapper.class);

    @Mapping(target = "number", source = "setNumber")
    @Mapping(target = "player1", source = "p1Games")
    @Mapping(target = "player2", source = "p2Games")
    @Mapping(target = "tiebreak1", source = "p1Tiebreak")
    @Mapping(target = "tiebreak2", source = "p2Tiebreak")
    SetScoreVO toSetScoreVO(SetScoreData data);
}
