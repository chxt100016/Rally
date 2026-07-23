package com.rally.tournament.convert;

import com.rally.domain.tournament.model.TournamentAdminItemDTO;
import com.rally.domain.tournament.model.TournamentData;
import org.mapstruct.Mapping;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 赛事域 MapStruct 转换器：Data → DTO
 */
@Mapper
public interface TournamentAppConvertMapper {

    TournamentAppConvertMapper INSTANCE = Mappers.getMapper(TournamentAppConvertMapper.class);

    @Mapping(target = "tournamentId", source = "bizId")
    @Mapping(target = "posterUrl", expression = "java(com.rally.config.property.QiniuConfiguration.buildSignedUrl(data.getPosterKey()))")
    TournamentAdminItemDTO toTournamentAdminItemDTO(TournamentData data);

    List<TournamentAdminItemDTO> toTournamentAdminItemDTOList(List<TournamentData> dataList);
}
