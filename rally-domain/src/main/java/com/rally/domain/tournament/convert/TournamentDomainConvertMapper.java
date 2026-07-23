package com.rally.domain.tournament.convert;

import com.rally.domain.tournament.model.TournamentCreateCmd;
import com.rally.domain.tournament.model.TournamentData;
import com.rally.domain.tournament.model.TournamentDTO;
import com.rally.domain.tournament.model.TournamentEntryData;
import com.rally.domain.tournament.model.TournamentEntryDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 赛事域 MapStruct 转换器：Cmd/Data 互转
 */
@Mapper
public interface TournamentDomainConvertMapper {

    TournamentDomainConvertMapper INSTANCE = Mappers.getMapper(TournamentDomainConvertMapper.class);

    TournamentData toTournamentData(TournamentCreateCmd cmd);

    /**
     * 编辑草稿：覆盖除 bizId/status/currentFilledSlots 外的全部配置字段
     */
    void updateTournamentData(@org.mapstruct.MappingTarget TournamentData data, TournamentCreateCmd cmd);

    @Mapping(target = "tournamentId", source = "bizId")
    @Mapping(target = "posterUrl", source = "posterKey")
    @Mapping(target = "displayStatus", ignore = true)
    @Mapping(target = "displayStatusShow", ignore = true)
    TournamentDTO toTournamentDTO(TournamentData data);

    @Mapping(target = "entryId", source = "bizId")
    TournamentEntryDTO toTournamentEntryDTO(TournamentEntryData data);
}
