package com.rally.tennis.convert;

import com.rally.client.qiniu.QiniuClient;
import com.rally.config.property.QiniuConfiguration;
import com.rally.domain.tennis.model.TournamentData;
import com.rally.domain.tennis.model.TournamentDTO;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Mapper
public interface TournamentConvertMapper {

    TournamentConvertMapper INSTANCE = Mappers.getMapper(TournamentConvertMapper.class);

    DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Mapping(target = "id", source = "tournamentId")
    @Mapping(target = "type", source = "tour")
    @Mapping(target = "typeLabel", source = "tour", qualifiedByName = "resolveTypeLabel")
    @Mapping(target = "surface", source = "surface", qualifiedByName = "upperSurface")
    @Mapping(target = "surfaceLabel", source = "surface")
    @Mapping(target = "startDate", source = "startDate", qualifiedByName = "formatDate")
    @Mapping(target = "endDate", source = "endDate", qualifiedByName = "formatDate")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "statusLabel", ignore = true)
    @Mapping(target = "groupId", ignore = true)
    @Mapping(target = "backgroundUrl", ignore = true)
    TournamentDTO toDTO(TournamentData data, @Context String groupId, @Context QiniuClient qiniuClient);

    List<TournamentDTO> toDTOList(List<TournamentData> dataList, @Context String groupId, @Context QiniuClient qiniuClient);

    @AfterMapping
    default void fillDerivedFields(TournamentData data, @Context String groupId, @Context QiniuClient qiniuClient, @MappingTarget TournamentDTO dto) {
        String displayStatus = deriveStatus(data);
        dto.setStatus(displayStatus);
        dto.setStatusLabel(resolveStatusLabel(displayStatus));
        dto.setGroupId(groupId);
        if (data.getBackgroundPath() != null && !data.getBackgroundPath().isBlank()) {
            dto.setBackgroundUrl(QiniuConfiguration.buildSignedUrl(data.getBackgroundPath()));
        }
    }

    @Named("upperSurface")
    default String upperSurface(String surface) {
        return surface != null ? surface.toUpperCase() : null;
    }

    @Named("resolveTypeLabel")
    default String resolveTypeLabel(String type) {
        if (type == null) return "";
        return switch (type) {
            case "ATP" -> "ATP";
            case "WTA" -> "WTA";
            default -> type;
        };
    }

    @Named("formatDate")
    default String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FMT) : null;
    }

    default String deriveStatus(TournamentData data) {
        LocalDate today = LocalDate.now();
        if (data.getEndDate() != null && today.isAfter(data.getEndDate())) {
            return "FINISHED";
        }
        if (data.getStartDate() != null && today.isBefore(data.getStartDate())) {
            return "UPCOMING";
        }
        return "ONGOING";
    }

    default String resolveStatusLabel(String status) {
        if (status == null) return "";
        return switch (status) {
            case "ONGOING" -> "进行中";
            case "UPCOMING" -> "即将开始";
            case "FINISHED" -> "已结束";
            default -> status;
        };
    }
}
