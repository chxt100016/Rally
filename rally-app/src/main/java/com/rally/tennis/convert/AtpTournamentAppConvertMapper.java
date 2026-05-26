package com.rally.tennis.convert;

import com.rally.client.atp.model.AtpTournamentsResponse;
import com.rally.db.tennis.entity.TennisTournamentPO;
import com.rally.tennis.model.Tournament;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Mapper
public interface AtpTournamentAppConvertMapper {

    AtpTournamentAppConvertMapper INSTANCE = Mappers.getMapper(AtpTournamentAppConvertMapper.class);

    @Mapping(target = "tournamentId", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "tour", constant = "ATP")
    @Mapping(target = "category", source = "type")
    @Mapping(target = "surface", source = "surface")
    @Mapping(target = "city", source = "location", qualifiedByName = "locationToCity")
    @Mapping(target = "country", source = "location", qualifiedByName = "locationToCountry")
    @Mapping(target = "prizeMoneyText", source = "totalFinancialCommitment")
    @Mapping(target = "prizeMoney", source = "totalFinancialCommitment", qualifiedByName = "parsePrizeMoney")
    @Mapping(target = "status", source = "isPastEvent", qualifiedByName = "parseStatus")
    @Mapping(target = "startDate", source = "formattedDate", qualifiedByName = "parseStartDate")
    @Mapping(target = "endDate", source = "formattedDate", qualifiedByName = "parseEndDate")
    @Mapping(target = "year", ignore = true)
    Tournament toTournament(AtpTournamentsResponse.TournamentItem item);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    TennisTournamentPO toTournamentPO(Tournament tournament);

    List<TennisTournamentPO> toTournamentPOList(List<Tournament> tournaments);

    @Named("locationToCity")
    default String locationToCity(String location) {
        if (location == null) return null;
        int idx = location.lastIndexOf(',');
        return idx > 0 ? location.substring(0, idx).trim() : location.trim();
    }

    @Named("locationToCountry")
    default String locationToCountry(String location) {
        if (location == null) return null;
        int idx = location.lastIndexOf(',');
        return idx > 0 ? location.substring(idx + 1).trim() : null;
    }

    @Named("parsePrizeMoney")
    default Integer parsePrizeMoney(String prizeText) {
        if (prizeText == null) return null;
        try {
            String cleaned = prizeText.replaceAll("[^0-9]", "");
            return cleaned.isEmpty() ? null : Integer.parseInt(cleaned);
        } catch (Exception e) {
            return null;
        }
    }

    @Named("parseStatus")
    default String parseStatus(Boolean isPastEvent) {
        return Boolean.TRUE.equals(isPastEvent) ? "completed" : "active";
    }

    /**
     * 解析 "4 - 11 January, 2026" → 开始日期
     */
    @Named("parseStartDate")
    default LocalDate parseStartDate(String formattedDate) {
        return parseDateFromFormatted(formattedDate, true);
    }

    /**
     * 解析 "4 - 11 January, 2026" → 结束日期
     */
    @Named("parseEndDate")
    default LocalDate parseEndDate(String formattedDate) {
        return parseDateFromFormatted(formattedDate, false);
    }

    // 解析 "4 - 11 January, 2026" 格式，start=true 取开始日，false 取结束日
    default LocalDate parseDateFromFormatted(String formattedDate, boolean start) {
        if (formattedDate == null || formattedDate.isBlank()) return null;
        try {
            // 格式: "d - d Month, yyyy" 或跨月 "31 December, 2025 - 11 January, 2026"
            String s = formattedDate.trim();
            if (s.contains(",")) {
                // 尝试跨月格式: "31 December, 2025 - 11 January, 2026"
                String[] parts = s.split(" - ");
                if (parts.length == 2 && parts[0].contains(",")) {
                    // 两部分都含年份
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d MMMM, yyyy", Locale.ENGLISH);
                    return start ? LocalDate.parse(parts[0].trim(), fmt) : LocalDate.parse(parts[1].trim(), fmt);
                }
                // 同月格式: "4 - 11 January, 2026"
                String[] dashParts = s.split(" - ");
                if (dashParts.length == 2) {
                    String endPart = dashParts[1].trim(); // "11 January, 2026"
                    String[] endTokens = endPart.split(" ", 2); // ["11", "January, 2026"]
                    String monthYear = endTokens[1]; // "January, 2026"
                    String startDay = dashParts[0].trim(); // "4"
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d MMMM, yyyy", Locale.ENGLISH);
                    if (start) {
                        return LocalDate.parse(startDay + " " + monthYear, fmt);
                    } else {
                        return LocalDate.parse(endPart, fmt);
                    }
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
