package com.rally.tennis.convert;

import com.rally.client.qiniu.QiniuClient;
import com.rally.config.property.QiniuConfiguration;
import com.rally.db.tennis.entity.TennisTournamentPO;
import com.rally.domain.tennis.model.TournamentDTO;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 赛事 PO → DTO 转换器
 */
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
    @Mapping(target = "status", ignore = true)       // 由 @AfterMapping 从日期派生
    @Mapping(target = "statusLabel", ignore = true)   // 由 @AfterMapping 根据 status 填充
    @Mapping(target = "groupId", ignore = true)       // 由外部通过 @Context 传入
    @Mapping(target = "backgroundUrl", ignore = true) // 由 @AfterMapping 通过 QiniuClient 生成
    TournamentDTO toDTO(TennisTournamentPO po, @Context String groupId, @Context QiniuClient qiniuClient);

    List<TournamentDTO> toDTOList(List<TennisTournamentPO> poList, @Context String groupId, @Context QiniuClient qiniuClient);

    /**
     * 映射完成后：派生展示状态、填充 statusLabel、生成背景图 URL
     */
    @AfterMapping
    default void fillDerivedFields(TennisTournamentPO po,
                                   @Context String groupId,
                                   @Context QiniuClient qiniuClient,
                                   @MappingTarget TournamentDTO dto) {
        // 从日期派生展示状态
        String displayStatus = deriveStatus(po);
        dto.setStatus(displayStatus);
        dto.setStatusLabel(resolveStatusLabel(displayStatus));

        // groupId 从外部传入
        dto.setGroupId(groupId);

        // 生成背景图签名 URL
        if (po.getBackgroundPath() != null && !po.getBackgroundPath().isBlank()) {
            dto.setBackgroundUrl(QiniuConfiguration.buildSignedUrl(po.getBackgroundPath()));
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

    default String deriveStatus(TennisTournamentPO po) {
        LocalDate today = LocalDate.now();
        if (po.getEndDate() != null && today.isAfter(po.getEndDate())) {
            return "FINISHED";
        }
        if (po.getStartDate() != null && today.isBefore(po.getStartDate())) {
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
