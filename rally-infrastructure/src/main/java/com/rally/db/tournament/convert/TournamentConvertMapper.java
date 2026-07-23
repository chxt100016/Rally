package com.rally.db.tournament.convert;

import com.rally.db.tournament.entity.TournamentPO;
import com.rally.domain.tournament.enums.TournamentGenderLimitEnum;
import com.rally.domain.tournament.enums.TournamentStatusEnum;
import com.rally.domain.tournament.model.TournamentData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 赛事域 MapStruct 转换器：PO ↔ Data
 */
@Mapper
public interface TournamentConvertMapper {

    TournamentConvertMapper INSTANCE = Mappers.getMapper(TournamentConvertMapper.class);

    @Mapping(target = "genderLimit", source = "genderLimit", qualifiedByName = "strToGenderLimit")
    @Mapping(target = "status", source = "status", qualifiedByName = "strToStatus")
    TournamentData toTournamentData(TournamentPO po);

    List<TournamentData> toTournamentDataList(List<TournamentPO> poList);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "genderLimit", source = "genderLimit", qualifiedByName = "genderLimitToStr")
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToStr")
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    TournamentPO toTournamentPO(TournamentData data);

    @Named("strToGenderLimit")
    static TournamentGenderLimitEnum strToGenderLimit(String value) {
        return value == null ? null : TournamentGenderLimitEnum.valueOf(value);
    }

    @Named("genderLimitToStr")
    static String genderLimitToStr(TournamentGenderLimitEnum value) {
        return value == null ? null : value.name();
    }

    @Named("strToStatus")
    static TournamentStatusEnum strToStatus(String value) {
        return value == null ? null : TournamentStatusEnum.valueOf(value);
    }

    @Named("statusToStr")
    static String statusToStr(TournamentStatusEnum value) {
        return value == null ? null : value.name();
    }
}
