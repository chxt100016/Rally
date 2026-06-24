package com.rally.db.user.repository;

import com.rally.db.user.convert.TennisProfileConvertMapper;
import com.rally.db.user.entity.TennisProfilePO;
import com.rally.db.user.service.TennisProfileService;
import com.rally.domain.user.gateway.TennisProfileRepository;
import com.rally.domain.user.model.TennisProfileData;
import com.rally.domain.user.model.VideoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TennisProfileRepositoryImpl implements TennisProfileRepository {

    private final TennisProfileService tourProfileService;
    private static final TennisProfileConvertMapper CONVERTER = TennisProfileConvertMapper.INSTANCE;

    @Override
    public Optional<TennisProfileData> findByUserId(String userId) {
        return tourProfileService.findByUserId(userId).map(CONVERTER::toData);
    }

    @Override
    public TennisProfileData update(TennisProfileData data) {
        TennisProfilePO po = tourProfileService.findByUserId(data.getUserId())
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
        TennisProfilePO po = tourProfileService.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("档案不存在"));
        po.setVideos(CONVERTER.videoListToJson(videos));
        tourProfileService.updateById(po);
    }

    @Override
    public void updateScoreFields(String userId, Integer reputationScore, Integer credibilityScore,
                                  Integer calibrationScore, Boolean isNewbie) {
        tourProfileService.lambdaUpdate()
                .eq(TennisProfilePO::getUserId, userId)
                .set(reputationScore != null, TennisProfilePO::getReputationScore, reputationScore)
                .set(credibilityScore != null, TennisProfilePO::getCredibilityScore, credibilityScore)
                .set(calibrationScore != null, TennisProfilePO::getCalibrationScore, calibrationScore)
                .set(isNewbie != null, TennisProfilePO::getIsNewbie, isNewbie)
                .update();
    }
}
