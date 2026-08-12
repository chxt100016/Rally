package com.rally.db.tour.convert;

import com.alibaba.fastjson2.JSON;
import com.rally.db.tour.entity.TourDrawPO;
import com.rally.db.tour.entity.TourMatchPO;
import com.rally.db.tour.entity.TourPlayerPO;
import com.rally.db.tour.entity.TourTournamentEntryPO;
import com.rally.db.tour.entity.TourTournamentPO;
import com.rally.domain.tour.model.MatchData;
import com.rally.domain.tour.model.PlayerData;
import com.rally.domain.tour.model.SetScore;
import com.rally.domain.tour.model.TourDrawData;
import com.rally.domain.tour.model.TournamentData;
import com.rally.domain.tour.model.TournamentEntryData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface TourConvertMapper {

    TourConvertMapper INSTANCE = Mappers.getMapper(TourConvertMapper.class);

    TournamentData toTournamentData(TourTournamentPO po);

    List<TournamentData> toTournamentDataList(List<TourTournamentPO> pos);

    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    TourTournamentPO toTournamentPO(TournamentData data);

    List<TourTournamentPO> toTournamentPOList(List<TournamentData> dataList);

    PlayerData toPlayerData(TourPlayerPO po);

    List<PlayerData> toPlayerDataList(List<TourPlayerPO> pos);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    TourPlayerPO toPlayerPO(PlayerData data);

    List<TourPlayerPO> toPlayerPOList(List<PlayerData> dataList);

    @Mapping(target = "tourMatchId", source = "id")
    @Mapping(target = "drawType", ignore = true)
    @Mapping(target = "sets", source = "setsJson")
    MatchData toMatchData(TourMatchPO po);

    List<MatchData> toMatchDataList(List<TourMatchPO> pos);

    @Mapping(target = "id", source = "tourMatchId")
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "setsJson", source = "sets")
    TourMatchPO toMatchPO(MatchData data);

    List<TourMatchPO> toMatchPOList(List<MatchData> dataList);

    default String setsToJson(List<SetScore> sets) {
        return sets == null || sets.isEmpty() ? null : JSON.toJSONString(sets);
    }

    default List<SetScore> jsonToSets(String setsJson) {
        return setsJson == null || setsJson.isBlank()
                ? List.of()
                : JSON.parseArray(setsJson, SetScore.class);
    }

    TourDrawData toDrawData(TourDrawPO po);

    List<TourDrawData> toDrawDataList(List<TourDrawPO> pos);

    TournamentEntryData toEntryData(TourTournamentEntryPO po);

    List<TournamentEntryData> toEntryDataList(List<TourTournamentEntryPO> pos);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    TourTournamentEntryPO toEntryPO(TournamentEntryData data);

    List<TourTournamentEntryPO> toEntryPOList(List<TournamentEntryData> dataList);
}
