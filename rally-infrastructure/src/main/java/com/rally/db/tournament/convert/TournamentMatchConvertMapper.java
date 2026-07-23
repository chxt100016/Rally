package com.rally.db.tournament.convert;

import com.rally.db.tournament.entity.TournamentMatchPO;
import com.rally.domain.meetup.enums.CourtSelectModeEnum;
import com.rally.domain.tournament.enums.TournamentMatchStatusEnum;
import com.rally.domain.tournament.enums.TournamentRoundEnum;
import com.rally.domain.tournament.model.TournamentMatchData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

/**
 * 比赛域 MapStruct 转换器：PO ↔ Data
 */
@Mapper
public interface TournamentMatchConvertMapper {

    TournamentMatchConvertMapper INSTANCE = Mappers.getMapper(TournamentMatchConvertMapper.class);

    @Mapping(target = "round", source = "round", qualifiedByName = "strToRound")
    @Mapping(target = "status", source = "status", qualifiedByName = "strToMatchStatus")
    @Mapping(target = "courtSelectMode", source = "courtSelectMode", qualifiedByName = "strToCourtSelectMode")
    TournamentMatchData toTournamentMatchData(TournamentMatchPO po);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "round", source = "round", qualifiedByName = "roundToStr")
    @Mapping(target = "status", source = "status", qualifiedByName = "matchStatusToStr")
    @Mapping(target = "courtSelectMode", source = "courtSelectMode", qualifiedByName = "courtSelectModeToStr")
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    TournamentMatchPO toTournamentMatchPO(TournamentMatchData data);

    @Named("strToRound")
    static TournamentRoundEnum strToRound(String value) {
        return value == null ? null : TournamentRoundEnum.valueOf(value);
    }

    @Named("roundToStr")
    static String roundToStr(TournamentRoundEnum value) {
        return value == null ? null : value.name();
    }

    @Named("strToMatchStatus")
    static TournamentMatchStatusEnum strToMatchStatus(String value) {
        return value == null ? null : TournamentMatchStatusEnum.valueOf(value);
    }

    @Named("matchStatusToStr")
    static String matchStatusToStr(TournamentMatchStatusEnum value) {
        return value == null ? null : value.name();
    }

    @Named("strToCourtSelectMode")
    static CourtSelectModeEnum strToCourtSelectMode(String value) {
        return value == null ? null : CourtSelectModeEnum.valueOf(value);
    }

    @Named("courtSelectModeToStr")
    static String courtSelectModeToStr(CourtSelectModeEnum value) {
        return value == null ? null : value.name();
    }
}
