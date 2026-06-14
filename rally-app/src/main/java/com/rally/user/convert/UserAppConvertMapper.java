package com.rally.user.convert;

import com.rally.domain.user.model.UploadVideoCmd;
import com.rally.domain.user.model.VideoVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface UserAppConvertMapper {

    UserAppConvertMapper INSTANCE = Mappers.getMapper(UserAppConvertMapper.class);

    VideoVO toVideoVO(UploadVideoCmd cmd);
}
