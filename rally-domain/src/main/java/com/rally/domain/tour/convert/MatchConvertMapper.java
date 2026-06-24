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
    @Mapping(target = "tiebreak1", expression = "java(bothZero(data.getP1Tiebreak(), data.getP2Tiebreak()) ? null : data.getP1Tiebreak())")
    @Mapping(target = "tiebreak2", expression = "java(bothZero(data.getP1Tiebreak(), data.getP2Tiebreak()) ? null : data.getP2Tiebreak())")
    SetScoreVO toSetScoreVO(SetScoreData data);

    default boolean bothZero(Integer t1, Integer t2) {
        return Integer.valueOf(0).equals(t1) && Integer.valueOf(0).equals(t2);
    }
}
