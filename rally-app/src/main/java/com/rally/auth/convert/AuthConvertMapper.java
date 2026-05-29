package com.rally.auth.convert;

import com.rally.domain.user.model.UserData;
import com.rally.domain.user.model.UserVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface AuthConvertMapper {

    AuthConvertMapper INSTANCE = Mappers.getMapper(AuthConvertMapper.class);

    UserVO toVO(UserData data);
}
