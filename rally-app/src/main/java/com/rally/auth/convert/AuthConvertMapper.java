package com.rally.auth.convert;

import com.rally.domain.auth.model.CompleteRegistrationCmd;
import com.rally.domain.user.model.UserData;
import com.rally.domain.user.model.UserVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface AuthConvertMapper {

    AuthConvertMapper INSTANCE = Mappers.getMapper(AuthConvertMapper.class);

    UserVO toVO(UserData data);

    /**
     * CompleteRegistrationCmd -> UserData，只映射注册相关字段
     */
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "bio", ignore = true)
    UserData toUserData(CompleteRegistrationCmd cmd);
}
