package com.rally.tennis.convert;

import com.rally.client.tennistv.model.MatchesResponse;
import com.rally.domain.tennis.model.TournamentData;
import com.rally.tennis.model.Tournament;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface TournamentAppConvertMapper {

    TournamentAppConvertMapper INSTANCE = Mappers.getMapper(TournamentAppConvertMapper.class);

    @Mapping(target = "tournamentId", source = "id")
    @Mapping(target = "category", source = "type")
    @Mapping(target = "city", source = "info.city")
    @Mapping(target = "country", source = "location")
    @Mapping(target = "tour", source = "gender")
    @Mapping(target = "prizeMoneyText", expression = "java(info.getInfo() != null ? info.getInfo().getPrize() : null)")
    @Mapping(target = "prizeMoney", expression = "java(parsePrizeMoney(info.getInfo()))")
    @Mapping(target = "status", constant = "active")
    @Mapping(target = "startDate", expression = "java(parseDate(info.getStart()))")
    @Mapping(target = "endDate", expression = "java(parseDate(info.getEnd()))")
    Tournament toTournament(MatchesResponse.TournamentInfo info);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "imagePath", ignore = true)
    @Mapping(target = "backgroundPath", ignore = true)
    TournamentData toTournamentData(Tournament tournament);

    List<TournamentData> toTournamentDataList(List<Tournament> tournaments);

    default java.time.LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            return java.time.LocalDate.parse(dateStr);
        } catch (Exception e) {
            return null;
        }
    }

    default Integer parsePrizeMoney(MatchesResponse.TournamentDetailInfo info) {
        if (info == null || info.getPrize() == null) return null;
        try {
            String cleaned = info.getPrize().replaceAll("[^0-9]", "");
            return cleaned.isEmpty() ? null : Integer.parseInt(cleaned);
        } catch (Exception e) {
            return null;
        }
    }
}
