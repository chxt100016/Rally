package com.rally.db.recap.gateway;

import com.rally.db.review.convert.ReviewConvertMapper;
import com.rally.db.review.entity.ReviewPO;
import com.rally.db.review.entity.ScoreRecordPO;
import com.rally.db.review.repository.ReviewRepository;
import com.rally.db.review.repository.ScoreRecordRepository;
import com.rally.domain.meetup.gateway.MeetupGateway;
import com.rally.domain.meetup.gateway.RegistrationGateway;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.recap.enums.ReviewTypeEnum;
import com.rally.domain.recap.gateway.RecapGateway;
import com.rally.domain.recap.model.RecapSubmitCmd;
import com.rally.domain.recap.model.RecapFactory;
import com.rally.domain.recap.model.ScoreConflictException;
import com.rally.domain.recap.model.ReviewData;
import com.rally.domain.recap.model.ScoreRecordData;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 赛后收集网关实现
 * <p>
 * 职责：
 * 1. 加载聚合根所需数据
 * 2. 封装评价 diff 判断与落库
 * 3. 封装比分版本校验、diff 判断与落库
 */
@Component
@RequiredArgsConstructor
public class RecapGatewayImpl implements RecapGateway {

    private final ReviewRepository reviewRepository;
    private final ScoreRecordRepository scoreRecordRepository;
    private final MeetupGateway meetupGateway;
    private final RegistrationGateway registrationGateway;

    private static final ReviewConvertMapper MAPPER = ReviewConvertMapper.INSTANCE;

    // ==================== 数据加载 ====================

    @Override
    public List<ReviewData> listMyReviews(String meetupId, String userId) {
        return MAPPER.toReviewDataList(
                reviewRepository.listByMeetupAndFrom(meetupId, userId));
    }

    @Override
    public List<String> listParticipantUserIds(String meetupId) {
        return meetupGateway.listParticipantUserIds(meetupId);
    }

    @Override
    public MeetupData findMeetupById(String meetupId) {
        return meetupGateway.findByBizId(meetupId);
    }

    @Override
    public List<ScoreRecordData> listScores(String meetupId) {
        return MAPPER.toScoreRecordDataList(scoreRecordRepository.listByMeetupId(meetupId));
    }

    // ==================== 评价提交 ====================

    @Override
    public void submitReviews(String meetupId, String fromUserId,
                              List<ReviewData> myReviews, List<RecapSubmitCmd.ReviewItem> targetReviews) {
        // 1. 构建当前评价 map，键 = (toUser, type)
        Map<String, ReviewData> existingMap = new LinkedHashMap<>();
        if (myReviews != null) {
            for (ReviewData r : myReviews) {
                String key = r.getToUserId() + ":" + r.getReviewType().name();
                existingMap.put(key, r);
            }
        }

        // 2. 构建目标 map
        Map<String, RecapSubmitCmd.ReviewItem> targetMap = new LinkedHashMap<>();
        if (targetReviews != null) {
            for (RecapSubmitCmd.ReviewItem item : targetReviews) {
                String key = item.getToUserId() + ":" + item.getType().name();
                targetMap.put(key, item);
            }
        }

        // 3. 遍历目标：找新增和更新
        for (Map.Entry<String, RecapSubmitCmd.ReviewItem> entry : targetMap.entrySet()) {
            String key = entry.getKey();
            RecapSubmitCmd.ReviewItem target = entry.getValue();
            ReviewData existing = existingMap.get(key);

            if (existing == null) {
                // 新增
                ReviewPO po = new ReviewPO();
                po.setBizId(IdWorker.getIdStr());
                po.setRallyMeetupId(meetupId);
                po.setFromUserId(fromUserId);
                po.setToUserId(target.getToUserId());
                po.setReviewType(target.getType().name());
                po.setReviewValue(target.getValue());
                reviewRepository.save(po);
            } else if (!target.getValue().equals(existing.getReviewValue())) {
                // 更新（value 变化）
                reviewRepository.updateValue(meetupId, fromUserId,
                        target.getToUserId(), target.getType().name(), target.getValue());
            }
        }

        // 4. 遍历当前：找删除（原本有、目标无）
        for (Map.Entry<String, ReviewData> entry : existingMap.entrySet()) {
            if (!targetMap.containsKey(entry.getKey())) {
                ReviewData existing = entry.getValue();
                reviewRepository.deleteByDimension(meetupId, fromUserId,
                        existing.getToUserId(), existing.getReviewType().name());
            }
        }
    }

    // ==================== 比分提交 ====================

    @Override
    public void submitScores(String meetupId, String userId,
                             List<ScoreRecordData> currentScores,
                             List<RecapSubmitCmd.ScoreItem> targetScores,
                             Integer clientVersion) {
        // 1. 版本校验
        int serverVersion = 0;
        if (currentScores != null) {
            for (ScoreRecordData s : currentScores) {
                if (s.getVersion() != null && s.getVersion() > serverVersion) {
                    serverVersion = s.getVersion();
                }
            }
        }

        boolean versionMatch;
        if (clientVersion == null) {
            versionMatch = (serverVersion == 0);
        } else {
            versionMatch = (serverVersion == clientVersion);
        }

        if (!versionMatch) {
            throw new ScoreConflictException(serverVersion);
        }

        // 2. 构建 map
        Map<Integer, ScoreRecordData> existingMap = new LinkedHashMap<>();
        if (currentScores != null) {
            for (ScoreRecordData s : currentScores) {
                existingMap.put(s.getSetNumber(), s);
            }
        }

        Map<Integer, RecapSubmitCmd.ScoreItem> targetMap = new LinkedHashMap<>();
        if (targetScores != null) {
            for (RecapSubmitCmd.ScoreItem item : targetScores) {
                targetMap.put(item.getSetNum(), item);
            }
        }

        // 3. 计算新版本号
        int newVersion = serverVersion + 1;

        // 4. 遍历目标：找新增和更新
        for (Map.Entry<Integer, RecapSubmitCmd.ScoreItem> entry : targetMap.entrySet()) {
            Integer setNum = entry.getKey();
            RecapSubmitCmd.ScoreItem target = entry.getValue();
            ScoreRecordData existing = existingMap.get(setNum);

            if (existing == null) {
                // 新增
                ScoreRecordPO po = buildScorePO(meetupId, userId, target);
                po.setVersion(newVersion);
                scoreRecordRepository.save(po);
            } else if (isScoreChanged(existing, target)) {
                // 更新
                scoreRecordRepository.updateWithoutVersionCheck(
                        existing.getBizId(), buildScorePO(meetupId, userId, target), newVersion);
            }
        }

        // 5. 遍历当前：找删除（原本有、目标无）
        for (Map.Entry<Integer, ScoreRecordData> entry : existingMap.entrySet()) {
            if (!targetMap.containsKey(entry.getKey())) {
                scoreRecordRepository.deleteByBizId(entry.getValue().getBizId());
            }
        }

        // 6. 批量同步版本号
        scoreRecordRepository.updateVersionByMeetupId(meetupId, newVersion);
    }

    // ==================== 内部方法 ====================

    /**
     * 比分是否有变化
     */
    private boolean isScoreChanged(ScoreRecordData existing, RecapSubmitCmd.ScoreItem target) {
        String existingFormat = existing.getSetFormat() != null ? existing.getSetFormat().name() : null;
        if (!Objects.equals(existingFormat, target.getSetFormat())) return true;
        if (!Objects.equals(existing.getSideAPlayer1(), target.getSideAPlayer1())) return true;
        if (!Objects.equals(existing.getSideAPlayer2(), target.getSideAPlayer2())) return true;
        if (!Objects.equals(existing.getSideBPlayer1(), target.getSideBPlayer1())) return true;
        if (!Objects.equals(existing.getSideBPlayer2(), target.getSideBPlayer2())) return true;
        if (!Objects.equals(existing.getSideAScore(), target.getSideAScore())) return true;
        if (!Objects.equals(existing.getSideBScore(), target.getSideBScore())) return true;
        return false;
    }

    /**
     * 构建 ScoreRecordPO
     */
    private ScoreRecordPO buildScorePO(String meetupId, String userId, RecapSubmitCmd.ScoreItem item) {
        ScoreRecordPO po = new ScoreRecordPO();
        po.setBizId(IdWorker.getIdStr());
        po.setRallyMeetupId(meetupId);
        po.setSetNumber(item.getSetNum());
        po.setSetFormat(item.getSetFormat());
        po.setSideAPlayer1(item.getSideAPlayer1());
        po.setSideAPlayer2(item.getSideAPlayer2());
        po.setSideBPlayer1(item.getSideBPlayer1());
        po.setSideBPlayer2(item.getSideBPlayer2());
        po.setSideAScore(item.getSideAScore());
        po.setSideBScore(item.getSideBScore());
        po.setRecordedBy(userId);
        return po;
    }
}
