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
    @Mapping(target = "rating", expression = "java(extractRating(data.getExtData()))")
    @Mapping(target = "cost", expression = "java(extractCost(data.getExtData()))")
    @Mapping(target = "opentime", expression = "java(extractOpentime(data.getExtData()))")
    @Mapping(target = "tel", expression = "java(extractTel(data.getExtData()))")
    @Mapping(target = "typeShow", expression = "java(convertTypeShow(data.getType()))")
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

    @Named("extractRating")
    default String extractRating(String extData) {
        if (StringUtils.isBlank(extData)) {
            return null;
        }
        try {
            JSONObject json = JSON.parseObject(extData);
            return json.getString("rating");
        } catch (Exception e) {
            return null;
        }
    }

    @Named("extractCost")
    default String extractCost(String extData) {
        if (StringUtils.isBlank(extData)) {
            return null;
        }
        try {
            JSONObject json = JSON.parseObject(extData);
            return json.getString("cost");
        } catch (Exception e) {
            return null;
        }
    }

    @Named("extractOpentime")
    default String extractOpentime(String extData) {
        if (StringUtils.isBlank(extData)) {
            return null;
        }
        try {
            JSONObject json = JSON.parseObject(extData);
            return json.getString("opentime");
        } catch (Exception e) {
            return null;
        }
    }

    @Named("extractTel")
    default String extractTel(String extData) {
        if (StringUtils.isBlank(extData)) {
            return null;
        }
        try {
            JSONObject json = JSON.parseObject(extData);
            return json.getString("tel");
        } catch (Exception e) {
            return null;
        }
    }

    @Named("convertTypeShow")
    default String convertTypeShow(com.rally.domain.court.enums.CourtEnvironmentEnum type) {
        if (type == null) {
            return null;
        }
        return type.getLabel();
    }
}
