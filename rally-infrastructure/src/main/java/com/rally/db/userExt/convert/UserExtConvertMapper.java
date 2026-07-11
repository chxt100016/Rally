package com.rally.db.userExt.convert;

import com.rally.db.userExt.entity.UserExtPO;
import com.rally.domain.user.model.UserExtData;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface UserExtConvertMapper {
    UserExtConvertMapper INSTANCE = Mappers.getMapper(UserExtConvertMapper.class);

    UserExtData toData(UserExtPO po);
    UserExtPO toPO(UserExtData data);
    List<UserExtData> toDataList(List<UserExtPO> poList);
}
