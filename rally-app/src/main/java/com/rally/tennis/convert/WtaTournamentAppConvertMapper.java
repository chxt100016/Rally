package com.rally.tennis.convert;

import com.rally.client.wta.model.WtaTournamentsResponse;
import com.rally.db.tennis.entity.TennisTournamentPO;
import com.rally.tennis.model.Tournament;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface WtaTournamentAppConvertMapper {

    WtaTournamentAppConvertMapper INSTANCE = Mappers.getMapper(WtaTournamentAppConvertMapper.class);

    @Mapping(target = "tournamentId", source = "tournamentGroup", qualifiedByName = "groupToId")
    @Mapping(target = "year", source = "year")
    @Mapping(target = "name", source = "title")
    @Mapping(target = "tour", constant = "WTA")
    @Mapping(target = "category", source = "tournamentGroup", qualifiedByName = "groupToCategory")
    @Mapping(target = "surface", source = "surface")
    @Mapping(target = "city", source = "city")
    @Mapping(target = "country", source = "country")
    @Mapping(target = "prizeMoney", source = "prizeMoney", qualifiedByName = "longToInt")
    @Mapping(target = "prizeMoneyText", ignore = true)
    @Mapping(target = "status", source = "status", qualifiedByName = "parseStatus")
    @Mapping(target = "startDate", source = "startDate", qualifiedByName = "parseDate")
    @Mapping(target = "endDate", source = "endDate", qualifiedByName = "parseDate")
    Tournament toTournament(WtaTournamentsResponse.TournamentItem item);

    @AfterMapping
    default void fillPrizeMoneyText(WtaTournamentsResponse.TournamentItem item, @MappingTarget Tournament tournament) {
        tournament.setPrizeMoneyText(item.getPrizeMoney() + " " + item.getPrizeMoneyCurrency());
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    TennisTournamentPO toTournamentPO(Tournament tournament);

    List<TennisTournamentPO> toTournamentPOList(List<Tournament> tournaments);

    @Named("groupToId")
    default String groupToId(WtaTournamentsResponse.TournamentGroup group) {
        return group != null ? String.valueOf(group.getId()) : null;
    }

    @Named("groupToCategory")
    default String groupToCategory(WtaTournamentsResponse.TournamentGroup group) {
        if (group == null) return null;
        return group.getLevel().replace("WTA", "").trim();
    }

    @Named("longToInt")
    default int longToInt(long value) {
        return (int) value;
    }

    @Named("parseStatus")
    default String parseStatus(String status) {
        return "past".equals(status) ? "completed" : "active";
    }

    @Named("parseDate")
    default LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            return null;
        }
    }
}
