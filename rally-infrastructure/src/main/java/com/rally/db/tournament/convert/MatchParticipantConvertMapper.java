package com.rally.db.tournament.convert;

import com.rally.db.tournament.entity.MatchParticipantPO;
import com.rally.domain.tournament.enums.ConfirmStatusEnum;
import com.rally.domain.tournament.model.MatchParticipantData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

/**
 * 比赛参与者域 MapStruct 转换器：PO ↔ Data
 */
@Mapper
public interface MatchParticipantConvertMapper {

    MatchParticipantConvertMapper INSTANCE = Mappers.getMapper(MatchParticipantConvertMapper.class);

    @Mapping(target = "confirmStatus", source = "confirmStatus", qualifiedByName = "strToConfirmStatus")
    @Mapping(target = "resultConfirmStatus", source = "resultConfirmStatus", qualifiedByName = "strToConfirmStatus")
    MatchParticipantData toMatchParticipantData(MatchParticipantPO po);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "confirmStatus", source = "confirmStatus", qualifiedByName = "confirmStatusToStr")
    @Mapping(target = "resultConfirmStatus", source = "resultConfirmStatus", qualifiedByName = "confirmStatusToStr")
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    MatchParticipantPO toMatchParticipantPO(MatchParticipantData data);

    @Named("strToConfirmStatus")
    static ConfirmStatusEnum strToConfirmStatus(String value) {
        return value == null ? null : ConfirmStatusEnum.valueOf(value);
    }

    @Named("confirmStatusToStr")
    static String confirmStatusToStr(ConfirmStatusEnum value) {
        return value == null ? null : value.name();
    }
}
