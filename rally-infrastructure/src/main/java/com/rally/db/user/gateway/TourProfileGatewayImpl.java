package com.rally.db.user.gateway;

import com.rally.db.user.convert.TourProfileConvertMapper;
import com.rally.db.user.entity.TourProfilePO;
import com.rally.db.user.repository.TourProfileRepository;
import com.rally.domain.user.gateway.TourProfileGateway;
import com.rally.domain.user.model.TourProfileData;
import com.rally.domain.user.model.VideoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TourProfileGatewayImpl implements TourProfileGateway {

    private final TourProfileRepository repository;
    private static final TourProfileConvertMapper CONVERTER = TourProfileConvertMapper.INSTANCE;



    @Override
    public Optional<TourProfileData> findByUserId(String userId) {
        return repository.findByUserId(userId).map(CONVERTER::toData);
    }

    @Override
    public TourProfileData update(TourProfileData data) {
        TourProfilePO po = repository.findByUserId(data.getUserId())
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
        repository.updateById(po);
        return CONVERTER.toData(po);
    }

    @Override
    public void updateVideos(String userId, List<VideoVO> videos) {
        TourProfilePO po = repository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("档案不存在"));
        po.setVideos(CONVERTER.videoListToJson(videos));
        repository.updateById(po);
    }

    @Override
    public void updateScoreFields(String userId, Integer reputationScore, Integer credibilityScore,
                                  Integer calibrationScore, Boolean isNewbie) {
        repository.updateScoreFields(userId, reputationScore, credibilityScore, calibrationScore, isNewbie);
    }
}
