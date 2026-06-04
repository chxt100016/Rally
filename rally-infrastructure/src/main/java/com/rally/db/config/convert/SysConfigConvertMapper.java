package com.rally.db.config.convert;

import com.rally.domain.system.enums.ValueType;
import com.rally.domain.system.model.ConfigData;
import com.rally.db.config.entity.SysConfigPO;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SysConfigConvertMapper {

    SysConfigConvertMapper INSTANCE = Mappers.getMapper(SysConfigConvertMapper.class);

    ConfigData toData(SysConfigPO po);

    SysConfigPO toPO(ConfigData data);

    @Named("stringToValueType")
    default ValueType stringToValueType(String valueType) {
        if (valueType == null) {
            return ValueType.STRING;
        }
        try {
            return ValueType.valueOf(valueType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ValueType.STRING;
        }
    }

    @Named("valueTypeToString")
    default String valueTypeToString(ValueType valueType) {
        if (valueType == null) {
            return "string";
        }
        return valueType.name().toLowerCase();
    }
}
