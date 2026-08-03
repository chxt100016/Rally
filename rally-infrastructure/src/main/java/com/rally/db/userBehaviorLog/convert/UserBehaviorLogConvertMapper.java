package com.rally.db.userBehaviorLog.convert;

import com.rally.db.userBehaviorLog.entity.UserBehaviorLogPO;
import com.rally.domain.behavior.model.UserBehaviorLogData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface UserBehaviorLogConvertMapper {

    UserBehaviorLogConvertMapper INSTANCE = Mappers.getMapper(UserBehaviorLogConvertMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    UserBehaviorLogPO toPO(UserBehaviorLogData data);
}
