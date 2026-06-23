package com.rally.db.user.convert;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.rally.db.user.entity.TourProfilePO;
import com.rally.domain.user.model.TourProfileData;
import com.rally.domain.user.model.VideoVO;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface TourProfileConvertMapper {

    TourProfileConvertMapper INSTANCE = Mappers.getMapper(TourProfileConvertMapper.class);

    @Named("toData")
    @Mapping(target = "videos", expression = "java(jsonToVideoList(po.getVideos()))")
    TourProfileData toData(TourProfilePO po);

    @Named("toPO")
    @Mapping(target = "videos", expression = "java(videoListToJson(data.getVideos()))")
    TourProfilePO toPO(TourProfileData data);

    /**
     * 更新 PO，只更新源对象中非空的字段
     * 忽略 bizId、userId、id 等不应被更新的字段
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "bizId", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "videos", expression = "java(videoListToJson(data.getVideos()))")
    void updatePO(@MappingTarget TourProfilePO po, TourProfileData data);

    @Named("videoListToJson")
    default String videoListToJson(List<VideoVO> list) {
        if (list == null) {
            return null;
        }
        return JSON.toJSONString(list);
    }

    @Named("jsonToVideoList")
    default List<VideoVO> jsonToVideoList(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        return JSON.parseObject(json, new TypeReference<List<VideoVO>>() {});
    }

}
