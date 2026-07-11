package com.rally.user.convert;

import com.rally.config.property.QiniuConfiguration;
import com.rally.domain.user.model.UserExtData;
import com.rally.user.model.PaymentCodeDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PaymentCodeAppConvertMapper {
    PaymentCodeAppConvertMapper INSTANCE = Mappers.getMapper(PaymentCodeAppConvertMapper.class);

    @Mapping(target = "key", source = "extValue")
    @Mapping(target = "paymentCodeUrl", source = "extValue", qualifiedByName = "keyToUrl")
    PaymentCodeDTO toDTO(UserExtData data);

    @Named("keyToUrl")
    default String keyToUrl(String key) {
        return QiniuConfiguration.buildSignedUrl(key);
    }
}
