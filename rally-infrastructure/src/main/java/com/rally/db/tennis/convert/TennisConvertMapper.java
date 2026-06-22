package com.rally.db.tennis.convert;

import com.rally.db.tennis.entity.TennisDrawPO;
import com.rally.db.tennis.entity.TennisMatchPO;
import com.rally.db.tennis.entity.TennisPlayerPO;
import com.rally.db.tennis.entity.TennisSetScorePO;
import com.rally.db.tennis.entity.TennisTournamentEntryPO;
import com.rally.db.tennis.entity.TennisTournamentPO;
import com.rally.domain.tennis.model.MatchData;
import com.rally.domain.tennis.model.PlayerData;
import com.rally.domain.tennis.model.SetScoreData;
import com.rally.domain.tennis.model.TennisDrawData;
import com.rally.domain.tennis.model.TournamentData;
import com.rally.domain.tennis.model.TournamentEntryData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface TennisConvertMapper {

    TennisConvertMapper INSTANCE = Mappers.getMapper(TennisConvertMapper.class);

    TournamentData toTournamentData(TennisTournamentPO po);

    List<TournamentData> toTournamentDataList(List<TennisTournamentPO> pos);

    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    TennisTournamentPO toTournamentPO(TournamentData data);

    List<TennisTournamentPO> toTournamentPOList(List<TournamentData> dataList);

    PlayerData toPlayerData(TennisPlayerPO po);

    List<PlayerData> toPlayerDataList(List<TennisPlayerPO> pos);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    TennisPlayerPO toPlayerPO(PlayerData data);

    List<TennisPlayerPO> toPlayerPOList(List<PlayerData> dataList);

    @Mapping(target = "tennisMatchId", source = "id")
    MatchData toMatchData(TennisMatchPO po);

    List<MatchData> toMatchDataList(List<TennisMatchPO> pos);

    @Mapping(target = "id", source = "tennisMatchId")
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "description", ignore = true)
    TennisMatchPO toMatchPO(MatchData data);

    List<TennisMatchPO> toMatchPOList(List<MatchData> dataList);

    SetScoreData toSetScoreData(TennisSetScorePO po);

    @Mapping(target = "id", ignore = true)
    TennisSetScorePO toSetScorePO(SetScoreData data);

    List<TennisSetScorePO> toSetScorePOList(List<SetScoreData> dataList);

    TennisDrawData toDrawData(TennisDrawPO po);

    List<TennisDrawData> toDrawDataList(List<TennisDrawPO> pos);

    TournamentEntryData toEntryData(TennisTournamentEntryPO po);

    List<TournamentEntryData> toEntryDataList(List<TennisTournamentEntryPO> pos);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    TennisTournamentEntryPO toEntryPO(TournamentEntryData data);

    List<TennisTournamentEntryPO> toEntryPOList(List<TournamentEntryData> dataList);
}
