package com.rally.home.convert;

import com.rally.domain.tour.model.TournamentData;
import com.rally.home.model.TournamentDisplayDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface HomeAppConvertMapper {

    HomeAppConvertMapper INSTANCE = Mappers.getMapper(HomeAppConvertMapper.class);

    @Mapping(target = "tournamentName", source = "name")
    @Mapping(target = "tour", ignore = true)
    @Mapping(target = "courtName", ignore = true)
    @Mapping(target = "matchDate", ignore = true)
    @Mapping(target = "imagePath", ignore = true)
    @Mapping(target = "matches", ignore = true)
    TournamentDisplayDTO toTournamentDisplayDTO(TournamentData data);
}
