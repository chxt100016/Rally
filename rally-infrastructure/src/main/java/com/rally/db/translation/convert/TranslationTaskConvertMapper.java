package com.rally.db.translation.convert;

import com.rally.domain.translation.model.TranslationData;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper
public interface TranslationTaskConvertMapper {

    TranslationTaskConvertMapper INSTANCE = Mappers.getMapper(TranslationTaskConvertMapper.class);

    /** 将 TranslationData 拼接为单行翻译任务：文案:AA;实体:BB;翻译后语言:CC */
    @Named("toTaskLine")
    default String toTaskLine(TranslationData data) {
        return "文案:" + data.getOriginalText()
                + ";实体:" + data.getEntityType().getChineseDesc()
                + ";翻译后语言:" + data.getLanguage().getChineseDesc();
    }
}
