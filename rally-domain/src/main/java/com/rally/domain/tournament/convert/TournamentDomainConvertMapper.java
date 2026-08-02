package com.rally.domain.tournament.convert;

import com.rally.domain.tournament.model.TournamentCreateCmd;
import com.rally.domain.tournament.model.TournamentData;
import com.rally.domain.tournament.model.TournamentDTO;
import com.rally.domain.tournament.model.TournamentEntryData;
import com.rally.domain.tournament.model.TournamentEntryDTO;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
    @Mapping(target = "wechatGroupQrCodeUrl", source = "wechatGroupQrCodeKey")
    @Mapping(target = "matchTypeShow", ignore = true)
    @Mapping(target = "genderLimitShow", ignore = true)
    @Mapping(target = "displayStatus", ignore = true)
    @Mapping(target = "displayStatusShow", ignore = true)
    @Mapping(target = "prizeMoneyList", expression = "java(TournamentDomainConvertMapper.splitPrizeMoney(data.getPrizeMoney()))")
    TournamentDTO toTournamentDTO(TournamentData data);

    @Mapping(target = "entryId", source = "bizId")
    @Mapping(target = "currentRoundShow", expression = "java(data.getCurrentRound() == null ? null : data.getCurrentRound().getLabel())")
    TournamentEntryDTO toTournamentEntryDTO(TournamentEntryData data);

    static List<Long> splitPrizeMoney(String prizeMoney) {
        if (StringUtils.isBlank(prizeMoney)) {
            return Collections.emptyList();
        }
        return Arrays.stream(prizeMoney.split(",")).map(Long::valueOf).collect(Collectors.toList());
    }
}
