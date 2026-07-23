package com.rally.db.tournament.convert;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.rally.db.tournament.entity.TournamentEntryPO;
import com.rally.domain.tournament.enums.CourtAbilityEnum;
import com.rally.domain.tournament.enums.TournamentEntryStageEnum;
import com.rally.domain.tournament.enums.TournamentEntryStatusEnum;
import com.rally.domain.tournament.enums.TournamentRoundEnum;
import com.rally.domain.tournament.model.TournamentEntryData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 报名域 MapStruct 转换器：PO ↔ Data
 */
@Mapper
public interface TournamentEntryConvertMapper {

    TournamentEntryConvertMapper INSTANCE = Mappers.getMapper(TournamentEntryConvertMapper.class);

    @Mapping(target = "preferredDistricts", source = "preferredDistricts", qualifiedByName = "jsonToStrList")
    @Mapping(target = "courtAbility", source = "courtAbility", qualifiedByName = "strToCourtAbility")
    @Mapping(target = "availableTimes", source = "availableTimes", qualifiedByName = "jsonToStrList")
    @Mapping(target = "stage", source = "stage", qualifiedByName = "strToStage")
    @Mapping(target = "status", source = "status", qualifiedByName = "strToStatus")
    @Mapping(target = "currentRound", source = "currentRound", qualifiedByName = "strToRound")
    TournamentEntryData toTournamentEntryData(TournamentEntryPO po);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "preferredDistricts", source = "preferredDistricts", qualifiedByName = "strListToJson")
    @Mapping(target = "courtAbility", source = "courtAbility", qualifiedByName = "courtAbilityToStr")
    @Mapping(target = "availableTimes", source = "availableTimes", qualifiedByName = "strListToJson")
    @Mapping(target = "stage", source = "stage", qualifiedByName = "stageToStr")
    @Mapping(target = "status", source = "status", qualifiedByName = "statusToStr")
    @Mapping(target = "currentRound", source = "currentRound", qualifiedByName = "roundToStr")
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    TournamentEntryPO toTournamentEntryPO(TournamentEntryData data);

    @Named("strToCourtAbility")
    static CourtAbilityEnum strToCourtAbility(String value) {
        return value == null ? null : CourtAbilityEnum.valueOf(value);
    }

    @Named("courtAbilityToStr")
    static String courtAbilityToStr(CourtAbilityEnum value) {
        return value == null ? null : value.name();
    }

    @Named("strToStage")
    static TournamentEntryStageEnum strToStage(String value) {
        return value == null ? null : TournamentEntryStageEnum.valueOf(value);
    }

    @Named("stageToStr")
    static String stageToStr(TournamentEntryStageEnum value) {
        return value == null ? null : value.name();
    }

    @Named("strToStatus")
    static TournamentEntryStatusEnum strToStatus(String value) {
        return value == null ? null : TournamentEntryStatusEnum.valueOf(value);
    }

    @Named("statusToStr")
    static String statusToStr(TournamentEntryStatusEnum value) {
        return value == null ? null : value.name();
    }

    @Named("strToRound")
    static TournamentRoundEnum strToRound(String value) {
        return value == null ? null : TournamentRoundEnum.valueOf(value);
    }

    @Named("roundToStr")
    static String roundToStr(TournamentRoundEnum value) {
        return value == null ? null : value.name();
    }

    @Named("jsonToStrList")
    static List<String> jsonToStrList(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        return JSON.parseObject(json, new TypeReference<List<String>>() {});
    }

    @Named("strListToJson")
    static String strListToJson(List<String> list) {
        if (list == null) {
            return null;
        }
        return JSON.toJSONString(list);
    }
}
