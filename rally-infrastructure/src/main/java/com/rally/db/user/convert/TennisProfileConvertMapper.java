package com.rally.db.user.convert;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.rally.db.user.entity.TennisProfilePO;
import com.rally.domain.user.model.TennisProfileData;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface TennisProfileConvertMapper {

    TennisProfileConvertMapper INSTANCE = Mappers.getMapper(TennisProfileConvertMapper.class);

    @Named("toData")
    @Mapping(target = "videoUrls", expression = "java(jsonToStringList(po.getVideoUrls()))")
    TennisProfileData toData(TennisProfilePO po);

    @Named("toPO")
    @Mapping(target = "videoUrls", expression = "java(stringListToJson(data.getVideoUrls()))")
    TennisProfilePO toPO(TennisProfileData data);

    /**
     * 更新 PO，只更新源对象中非空的字段
     * 忽略 bizId、userId、id 等不应被更新的字段
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "bizId", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "videoUrls", expression = "java(stringListToJson(data.getVideoUrls()))")
    void updatePO(@MappingTarget TennisProfilePO po, TennisProfileData data);

    @Named("stringListToJson")
    default String stringListToJson(List<String> list) {
        if (list == null) {
            return null;
        }
        return JSON.toJSONString(list);
    }

    @Named("jsonToStringList")
    default List<String> jsonToStringList(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        return JSON.parseObject(json, new TypeReference<List<String>>() {});
    }

}
