package com.rally.db.user.convert;

import com.rally.db.user.entity.UserPO;
import com.rally.domain.user.enums.GenderEnum;
import com.rally.domain.user.model.UserData;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

@Mapper
public interface UserConvertMapper {

    UserConvertMapper INSTANCE = Mappers.getMapper(UserConvertMapper.class);

    @Named("toData")
    @Mapping(target = "gender", expression = "java(stringToGender(po.getGender()))")
    UserData toData(UserPO po);

    @Named("toPO")
    @Mapping(target = "gender", expression = "java(genderToString(data.getGender()))")
    UserPO toPO(UserData data);

    /**
     * 更新 PO，只更新源对象中非空的字段
     * 忽略 userId 和 id 等不应被更新的字段
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "gender", expression = "java(genderToString(data.getGender()))")
    void updatePO(@MappingTarget UserPO po, UserData data);

    @Named("genderToString")
    default String genderToString(GenderEnum gender) {
        if (gender == null) {
            return GenderEnum.UNDISCLOSED.name().toLowerCase();
        }
        return gender.name().toLowerCase();
    }

    @Named("stringToGender")
    default GenderEnum stringToGender(String gender) {
        if (gender == null) {
            return GenderEnum.UNDISCLOSED;
        }
        try {
            return GenderEnum.valueOf(gender.toUpperCase());
        } catch (IllegalArgumentException e) {
            return GenderEnum.UNDISCLOSED;
        }
    }
}
