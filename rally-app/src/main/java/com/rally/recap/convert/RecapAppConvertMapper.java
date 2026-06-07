package com.rally.recap.convert;

import com.rally.domain.recap.model.RecapCmd;
import com.rally.domain.recap.model.RecapDetailVO;
import com.rally.recap.model.RecapDetailDTO;
import com.rally.recap.model.RecapSubmitDTO;
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

    // ==================== Submit DTO -> Cmd ====================

    RecapCmd toCmd(RecapSubmitDTO dto);

    RecapCmd.ScoreItem toScoreItemCmd(RecapSubmitDTO.ScoreItem dto);

    RecapCmd.ReviewItem toReviewItemCmd(RecapSubmitDTO.ReviewItem dto);

    // ==================== Detail VO -> DTO ====================

    @Mapping(target = "myReviews", expression = "java(flattenReviewMap(vo.getMyReviews()))")
    RecapDetailDTO toDetailDTO(RecapDetailVO vo);

    RecapDetailDTO.ParticipantDTO toParticipantDTO(RecapDetailVO.ParticipantItem item);

    RecapDetailDTO.ReviewDTO toReviewDTO(RecapDetailVO.ReviewItem item);

    @Mapping(target = "setNum", source = "setNum")
    RecapDetailDTO.ScoreDTO toScoreDTO(RecapDetailVO.ScoreItem item);

    /**
     * 将 Map<String, List<ReviewItem>> 展平为 List<ReviewDTO>
     */
    default List<RecapDetailDTO.ReviewDTO> flattenReviewMap(Map<String, List<RecapDetailVO.ReviewItem>> reviewMap) {
        if (reviewMap == null) {
            return new ArrayList<>();
        }
        List<RecapDetailDTO.ReviewDTO> result = new ArrayList<>();
        for (List<RecapDetailVO.ReviewItem> items : reviewMap.values()) {
            for (RecapDetailVO.ReviewItem item : items) {
                result.add(toReviewDTO(item));
            }
        }
        return result;
    }
}
