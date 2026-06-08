package com.rally.recap.convert;

import com.rally.domain.recap.model.RecapDTO;
import com.rally.recap.model.RecapDetailDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 赛后收集 App 层转换器
 */
@Mapper
public interface RecapAppConvertMapper {

    RecapAppConvertMapper INSTANCE = Mappers.getMapper(RecapAppConvertMapper.class);

    // ==================== Detail VO -> DTO ====================

    @Mapping(target = "myReviews", expression = "java(flattenReviewMap(vo.getMyReviews()))")
    RecapDetailDTO toDetailDTO(RecapDTO vo);

    RecapDetailDTO.ReviewDTO toReviewDTO(RecapDTO.ReviewItem item);

    RecapDetailDTO.ScoreDTO toScoreDTO(RecapDTO.ScoreItem item);

    /**
     * 将 Map<String, List<ReviewItem>> 展平为 List<ReviewDTO>
     */
    default List<RecapDetailDTO.ReviewDTO> flattenReviewMap(Map<String, List<RecapDTO.ReviewItem>> reviewMap) {
        if (reviewMap == null) {
            return new ArrayList<>();
        }
        List<RecapDetailDTO.ReviewDTO> result = new ArrayList<>();
        for (List<RecapDTO.ReviewItem> items : reviewMap.values()) {
            for (RecapDTO.ReviewItem item : items) {
                result.add(toReviewDTO(item));
            }
        }
        return result;
    }
}
