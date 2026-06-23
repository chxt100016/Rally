package com.rally.tour.convert;

import com.rally.client.tourtv.model.AtpDrawsResponse;
import com.rally.client.tourtv.model.MatchesResponse;
import com.rally.client.tourtv.model.AtpOopResponse;
import com.rally.domain.tour.model.PlayerData;
import com.rally.tour.model.Player;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface PlayerAppConvertMapper {

    PlayerAppConvertMapper INSTANCE = Mappers.getMapper(PlayerAppConvertMapper.class);

    @Mapping(target = "firstName", source = "playerFirstNameFull")
    @Mapping(target = "lastName", source = "playerLastName")
    @Mapping(target = "nationality", source = "playerCountryCode")
    @Mapping(target = "rank", ignore = true)
    @Mapping(target = "points", ignore = true)
    @Mapping(target = "birthDate", ignore = true)
    @Mapping(target = "gender", ignore = true)
    @Mapping(target = "hand", ignore = true)
    Player toPlayer(MatchesResponse.PlayerTeam team);

    @Mapping(target = "nationality", source = "nationality")
    @Mapping(target = "points", ignore = true)
    @Mapping(target = "birthDate", ignore = true)
    @Mapping(target = "gender", ignore = true)
    @Mapping(target = "hand", ignore = true)
    @Mapping(target = "playerId", expression = "java(upCasePlayerId(info))")
    Player toPlayerFromDraw(AtpDrawsResponse.PlayerInfo info);

    default String upCasePlayerId(AtpDrawsResponse.PlayerInfo info) {
        return info.getPlayerId() == null ? null : info.getPlayerId().toUpperCase();
    }

    @Mapping(target = "nationality", source = "playerCountryCode")
    @Mapping(target = "points", ignore = true)
    @Mapping(target = "birthDate", ignore = true)
    @Mapping(target = "gender", ignore = true)
    @Mapping(target = "hand", ignore = true)
    Player toPlayerFromOop(AtpOopResponse.PlayerTeam team);

    PlayerData toPlayerData(Player player);

    List<PlayerData> toPlayerDataList(List<Player> players);
}
