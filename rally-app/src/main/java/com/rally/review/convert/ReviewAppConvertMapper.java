package com.rally.review.convert;

import com.rally.domain.review.model.ReviewData;
import com.rally.domain.review.model.ReviewVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 评价域 App 层 MapStruct 转换器
 */
@Mapper
public interface ReviewAppConvertMapper {

    ReviewAppConvertMapper INSTANCE = Mappers.getMapper(ReviewAppConvertMapper.class);

    /**
     * ReviewData → ReviewVO（基础字段映射，复杂聚合在 service 中手动构建）
     */
    ReviewVO toReviewVO(ReviewData data);
}
