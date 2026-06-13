package com.rally.db.user.convert;

import com.rally.db.user.entity.UserPO;
import com.rally.domain.user.enums.GenderEnum;
import com.rally.domain.user.model.EditProfileCmd;
import com.rally.domain.user.model.UserData;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

@Mapper
public interface UserConvertMapper {

    UserConvertMapper INSTANCE = Mappers.getMapper(UserConvertMapper.class);

    @Named("toData")
    UserData toData(UserPO po);

    @Named("toPO")
    UserPO toPO(UserData data);

    /**
     * 更新 UserData，只更新源对象中非空的字段
     * 忽略 userId 不应被更新
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "userId", ignore = true)
    void updateData(@MappingTarget UserData data, EditProfileCmd cmd);

    /**
     * 更新 PO，只更新源对象中非空的字段
     * 忽略 userId 和 id 等不应被更新的字段
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "id", ignore = true)
    void updatePO(@MappingTarget UserPO po, UserData data);

}
