package com.rally.db.review.convert;

import com.rally.db.review.entity.ReviewPO;
import com.rally.db.review.entity.ScoreRecordPO;
import com.rally.domain.recap.enums.ReviewTypeEnum;
import com.rally.domain.recap.enums.SetFormatEnum;
import com.rally.domain.recap.model.ReviewData;
import com.rally.domain.recap.model.ScoreRecordData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 评价域 PO ↔ Data 转换器
 */
@Mapper
public interface ReviewConvertMapper {

    ReviewConvertMapper INSTANCE = Mappers.getMapper(ReviewConvertMapper.class);

    // ==================== ReviewPO ↔ ReviewData ====================

    @Mapping(target = "reviewType", source = "reviewType", qualifiedByName = "strToReviewType")
    ReviewData toReviewData(ReviewPO po);

    List<ReviewData> toReviewDataList(List<ReviewPO> poList);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "reviewType", source = "reviewType", qualifiedByName = "reviewTypeToStr")
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    ReviewPO toReviewPO(ReviewData data);

    // ==================== ScoreRecordPO ↔ ScoreRecordData ====================

    @Mapping(target = "setFormat", source = "setFormat", qualifiedByName = "strToSetFormat")
    ScoreRecordData toScoreRecordData(ScoreRecordPO po);

    List<ScoreRecordData> toScoreRecordDataList(List<ScoreRecordPO> poList);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "setFormat", source = "setFormat", qualifiedByName = "setFormatToStr")
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    ScoreRecordPO toScoreRecordPO(ScoreRecordData data);

    // ==================== 枚举转换 ====================

    @Named("strToReviewType")
    static ReviewTypeEnum strToReviewType(String value) {
        return value == null ? null : ReviewTypeEnum.valueOf(value);
    }

    @Named("reviewTypeToStr")
    static String reviewTypeToStr(ReviewTypeEnum value) {
        return value == null ? null : value.name();
    }

    @Named("strToSetFormat")
    static SetFormatEnum strToSetFormat(String value) {
        return value == null ? null : SetFormatEnum.valueOf(value);
    }

    @Named("setFormatToStr")
    static String setFormatToStr(SetFormatEnum value) {
        return value == null ? null : value.name();
    }
}
