package com.rally.db.user.repository;

import com.rally.db.user.convert.TourProfileConvertMapper;
import com.rally.db.user.entity.TourProfilePO;
import com.rally.db.user.service.TourProfileService;
import com.rally.domain.user.gateway.TourProfileRepository;
import com.rally.domain.user.model.TourProfileData;
import com.rally.domain.user.model.VideoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TourProfileRepositoryImpl implements TourProfileRepository {

    private final TourProfileService tourProfileService;
    private static final TourProfileConvertMapper CONVERTER = TourProfileConvertMapper.INSTANCE;

    @Override
    public Optional<TourProfileData> findByUserId(String userId) {
        return tourProfileService.findByUserId(userId).map(CONVERTER::toData);
    }

    @Override
    public TourProfileData update(TourProfileData data) {
        TourProfilePO po = tourProfileService.findByUserId(data.getUserId())
                .orElseThrow(() -> new RuntimeException("档案不存在"));

        if (data.getVideos() != null) {
            po.setVideos(CONVERTER.videoListToJson(data.getVideos()));
        }
        if (data.getNtrpScore() != null) {
            po.setNtrpScore(data.getNtrpScore());
        }
        if (data.getNtrpUpdatedAt() != null) {
            po.setNtrpUpdatedAt(data.getNtrpUpdatedAt());
        }
        if (data.getStatus() != null) {
            po.setStatus(data.getStatus().toString());
        }
        if (data.getIsUnderReview() != null) {
            po.setIsUnderReview(data.getIsUnderReview());
        }
        tourProfileService.updateById(po);
        return CONVERTER.toData(po);
    }

    @Override
    public void updateVideos(String userId, List<VideoVO> videos) {
        TourProfilePO po = tourProfileService.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("档案不存在"));
        po.setVideos(CONVERTER.videoListToJson(videos));
        tourProfileService.updateById(po);
    }

    @Override
    public void updateScoreFields(String userId, Integer reputationScore, Integer credibilityScore,
                                  Integer calibrationScore, Boolean isNewbie) {
        tourProfileService.lambdaUpdate()
                .eq(TourProfilePO::getUserId, userId)
                .set(reputationScore != null, TourProfilePO::getReputationScore, reputationScore)
                .set(credibilityScore != null, TourProfilePO::getCredibilityScore, credibilityScore)
                .set(calibrationScore != null, TourProfilePO::getCalibrationScore, calibrationScore)
                .set(isNewbie != null, TourProfilePO::getIsNewbie, isNewbie)
                .update();
    }
}
