package com.rally.db.userFollow.convert;

import com.rally.db.userFollow.entity.UserFollowPO;
import com.rally.domain.user.model.UserFollowData;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface UserFollowConvertMapper {

    UserFollowConvertMapper INSTANCE = Mappers.getMapper(UserFollowConvertMapper.class);

    UserFollowData toData(UserFollowPO po);

    UserFollowPO toPO(UserFollowData data);

    List<UserFollowData> toDataList(List<UserFollowPO> poList);
}
