package com.rally.db.review.convert;

import com.rally.db.review.entity.ReviewPO;
import com.rally.db.review.entity.ScoreRecordPO;
import com.rally.domain.recap.enums.ReviewTypeEnum;
import com.rally.domain.recap.model.ReviewData;
import com.rally.domain.recap.model.ReviewSubmitCmd;
import com.rally.domain.recap.model.ScoreAddCmd;
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

    ScoreRecordData toScoreRecordData(ScoreRecordPO po);

    List<ScoreRecordData> toScoreRecordDataList(List<ScoreRecordPO> poList);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    ScoreRecordPO toScoreRecordPO(ScoreRecordData data);

    // ==================== Cmd → PO 转换 ====================

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "bizId", source = "bizId")
    @Mapping(target = "rallyMeetupId", source = "meetupId")
    @Mapping(target = "fromUserId", source = "fromUserId")
    @Mapping(target = "toUserId", source = "toUserId")
    @Mapping(target = "reviewType", source = "item.type", qualifiedByName = "reviewTypeToStr")
    @Mapping(target = "reviewValue", source = "item.value")
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    ReviewPO toReviewPO(ReviewSubmitCmd.ReviewItem item, String bizId, String meetupId, String fromUserId, String toUserId);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "bizId", source = "bizId")
    @Mapping(target = "rallyMeetupId", source = "meetupId")
    @Mapping(target = "setNumber", source = "cmd.setNum")
    @Mapping(target = "setFormat", source = "cmd.setFormatType")
    @Mapping(target = "recordedBy", source = "userId")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    ScoreRecordPO toScoreRecordPO(ScoreAddCmd cmd, String bizId, String meetupId, String userId);

    // ==================== 枚举转换 ====================

    @Named("strToReviewType")
    static ReviewTypeEnum strToReviewType(String value) {
        return value == null ? null : ReviewTypeEnum.valueOf(value);
    }

    @Named("reviewTypeToStr")
    static String reviewTypeToStr(ReviewTypeEnum value) {
        return value == null ? null : value.name();
    }
}
