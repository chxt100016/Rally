package com.rally.tournament.convert;

import com.rally.domain.tournament.model.TournamentEntryData;
import com.rally.domain.tournament.model.TournamentEntryDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 报名域 MapStruct 转换器：Data → DTO
 */
@Mapper
public interface TournamentEntryAppConvertMapper {

    TournamentEntryAppConvertMapper INSTANCE = Mappers.getMapper(TournamentEntryAppConvertMapper.class);

    @Mapping(target = "entryId", source = "bizId")
    TournamentEntryDTO toTournamentEntryDTO(TournamentEntryData data);
}
