package com.rally.court.convert;

import com.rally.domain.court.model.CourtCreateApiCmd;
import com.rally.domain.court.model.CourtCreateCmd;
import com.rally.domain.court.model.CourtProfile;
import com.rally.domain.court.model.CourtUpdateApiCmd;
import com.rally.domain.court.model.CourtUpdateCmd;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 球场后台运营接口出入参转换
 */
@Mapper
public interface CourtAdminAppConvertMapper {

    CourtAdminAppConvertMapper INSTANCE = Mappers.getMapper(CourtAdminAppConvertMapper.class);

    @Mapping(target = "location", ignore = true)
    @Mapping(target = "profile", expression = "java(toProfile(cmd.getRating(), cmd.getCost(), cmd.getOpentime(), cmd.getTel()))")
    CourtCreateCmd toCreateCmd(CourtCreateApiCmd cmd);

    @Mapping(target = "location", ignore = true)
    @Mapping(target = "profile", expression = "java(toProfile(cmd.getRating(), cmd.getCost(), cmd.getOpentime(), cmd.getTel()))")
    CourtUpdateCmd toUpdateCmd(CourtUpdateApiCmd cmd);

    default CourtProfile toProfile(String rating, String cost, String opentime, String tel) {
        return new CourtProfile(rating, cost, opentime, tel);
    }
}
