package com.rally.domain.court.convert;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.rally.domain.court.model.CourtDTO;
import com.rally.domain.court.model.CourtData;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 球场领域对象转换器
 */
@Mapper
public interface CourtConvertMapper {

    CourtConvertMapper INSTANCE = Mappers.getMapper(CourtConvertMapper.class);

    @Mapping(target = "courtId", source = "bizId")
    @Mapping(target = "tags", expression = "java(splitToList(data.getTags()))")
    @Mapping(target = "alias", expression = "java(splitToList(data.getAlias()))")
    @Mapping(target = "pinyin", expression = "java(extractPinyin(data.getExtData()))")
    @Mapping(target = "pinyinInitial", expression = "java(extractPinyinInitial(data.getExtData()))")
    CourtDTO toDTO(CourtData data);

    List<CourtDTO> toDTOList(List<CourtData> dataList);

    @Named("splitToList")
    default List<String> splitToList(String str) {
        if (StringUtils.isBlank(str)) {
            return Collections.emptyList();
        }
        return Arrays.asList(str.split(","));
    }

    @Named("extractPinyin")
    default String extractPinyin(String extData) {
        if (StringUtils.isBlank(extData)) {
            return null;
        }
        try {
            JSONObject json = JSON.parseObject(extData);
            return json.getString("pinyin");
        } catch (Exception e) {
            return null;
        }
    }

    @Named("extractPinyinInitial")
    default String extractPinyinInitial(String extData) {
        if (StringUtils.isBlank(extData)) {
            return null;
        }
        try {
            JSONObject json = JSON.parseObject(extData);
            return json.getString("pinyinInitial");
        } catch (Exception e) {
            return null;
        }
    }
}
