package com.rally.db.user.gateway;

import com.rally.db.user.convert.TennisProfileConvertMapper;
import com.rally.db.user.entity.TennisProfilePO;
import com.rally.db.user.repository.TennisProfileRepository;
import com.rally.domain.user.gateway.TennisProfileGateway;
import com.rally.domain.user.model.TennisProfileData;
import com.rally.domain.user.model.VideoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TennisProfileGatewayImpl implements TennisProfileGateway {

    private final TennisProfileRepository repository;
    private static final TennisProfileConvertMapper CONVERTER = TennisProfileConvertMapper.INSTANCE;



    @Override
    public Optional<TennisProfileData> findByUserId(String userId) {
        return repository.findByUserId(userId).map(CONVERTER::toData);
    }

    @Override
    public TennisProfileData update(TennisProfileData data) {
        TennisProfilePO po = repository.findByUserId(data.getUserId())
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
        TennisProfilePO po = repository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("档案不存在"));
        po.setVideos(CONVERTER.videoListToJson(videos));
        repository.updateById(po);
    }

    @Override
    public void updateScoreFields(String userId, BigDecimal reputationScore, BigDecimal credibilityScore,
                                  BigDecimal calibrationScore, Boolean isNewbie) {
        repository.updateScoreFields(userId, reputationScore, credibilityScore, calibrationScore, isNewbie);
    }
}
