package com.rally.db.translation.convert;

import com.rally.db.translation.entity.TranslationPO;
import com.rally.domain.translation.model.TranslationData;
import com.rally.domain.translation.model.TranslationEntityTypeEnum;
import com.rally.domain.translation.model.TranslationKey;
import com.rally.domain.translation.model.TranslationLanguageEnum;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface TranslationConvertMapper {

    TranslationConvertMapper INSTANCE = Mappers.getMapper(TranslationConvertMapper.class);

    // PO → Domain
    @Mapping(target = "entityType", source = "entityType", qualifiedByName = "strToEntityType")
    @Mapping(target = "language", source = "language", qualifiedByName = "strToLanguage")
    TranslationData toDomain(TranslationPO po);

    List<TranslationData> toDomainList(List<TranslationPO> poList);

    // Domain → PO（含 id，用于保存/更新）
    @Mapping(target = "entityType", source = "entityType", qualifiedByName = "entityTypeToStr")
    @Mapping(target = "language", source = "language", qualifiedByName = "languageToStr")
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    TranslationPO toPO(TranslationData data);

    // Domain → PO（不含 id，仅用于批量查询条件构造）
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "entityType", source = "entityType", qualifiedByName = "entityTypeToStr")
    @Mapping(target = "language", source = "language", qualifiedByName = "languageToStr")
    @Mapping(target = "translatedText", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    TranslationPO toQueryPO(TranslationData data);

    TranslationPO toQueryPO(TranslationKey key);
    List<TranslationPO> toQueryPO(List<TranslationKey> key);

    @Named("strToEntityType")
    static TranslationEntityTypeEnum strToEntityType(String value) {
        return value == null ? null : TranslationEntityTypeEnum.valueOf(value);
    }

    @Named("strToLanguage")
    static TranslationLanguageEnum strToLanguage(String value) {
        return value == null ? null : TranslationLanguageEnum.valueOf(value);
    }

    @Named("entityTypeToStr")
    static String entityTypeToStr(TranslationEntityTypeEnum entityType) {
        return entityType == null ? null : entityType.name();
    }

    @Named("languageToStr")
    static String languageToStr(TranslationLanguageEnum language) {
        return language == null ? null : language.name();
    }
}
