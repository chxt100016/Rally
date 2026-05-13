package com.rally.tennis.convert;

import com.rally.client.tennistv.model.AtpDrawsResponse;
import com.rally.client.tennistv.model.MatchesResponse;
import com.rally.client.tennistv.model.AtpOopResponse;
import com.rally.db.tennis.entity.TennisPlayerPO;
import com.rally.tennis.model.Player;
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
    @Mapping(target = "birthDate", ignore = true)
    @Mapping(target = "gender", ignore = true)
    @Mapping(target = "hand", ignore = true)
    Player toPlayer(MatchesResponse.PlayerTeam team);

    @Mapping(target = "nationality", source = "nationality")
    @Mapping(target = "birthDate", ignore = true)
    @Mapping(target = "gender", ignore = true)
    @Mapping(target = "hand", ignore = true)
    Player toPlayerFromDraw(AtpDrawsResponse.PlayerInfo info);

    @Mapping(target = "nationality", source = "playerCountryCode")
    @Mapping(target = "birthDate", ignore = true)
    @Mapping(target = "gender", ignore = true)
    @Mapping(target = "hand", ignore = true)
    Player toPlayerFromOop(AtpOopResponse.PlayerTeam team);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    TennisPlayerPO toPlayerPO(Player player);

    List<TennisPlayerPO> toPlayerPOList(List<Player> players);
}
