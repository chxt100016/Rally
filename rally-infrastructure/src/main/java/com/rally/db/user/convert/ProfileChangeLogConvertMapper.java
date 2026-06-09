package com.rally.db.user.convert;

import com.rally.domain.user.enums.ChangeLogTypeEnum;
import com.rally.domain.user.enums.ChangeReasonEnum;
import com.rally.domain.log.model.ProfileChangeLogData;
import com.rally.db.user.entity.ProfileChangeLogPO;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ProfileChangeLogConvertMapper {

    ProfileChangeLogConvertMapper INSTANCE = Mappers.getMapper(ProfileChangeLogConvertMapper.class);

    @Named("toData")
    ProfileChangeLogData toData(ProfileChangeLogPO po);

    @Named("toPO")
    ProfileChangeLogPO toPO(ProfileChangeLogData data);

    @Named("stringToChangeLogType")
    default ChangeLogTypeEnum stringToChangeLogType(String type) {
        if (type == null) {
            return null;
        }
        try {
            return ChangeLogTypeEnum.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Named("changeLogTypeToString")
    default String changeLogTypeToString(ChangeLogTypeEnum type) {
        if (type == null) {
            return null;
        }
        return type.name().toLowerCase();
    }

    @Named("stringToChangeReason")
    default ChangeReasonEnum stringToChangeReason(String reason) {
        if (reason == null) {
            return null;
        }
        try {
            return ChangeReasonEnum.valueOf(reason.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Named("changeReasonToString")
    default String changeReasonToString(ChangeReasonEnum reason) {
        if (reason == null) {
            return null;
        }
        return reason.name().toLowerCase();
    }
}
