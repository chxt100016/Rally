package com.rally.db.recap.gateway;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.db.review.convert.ReviewConvertMapper;
import com.rally.db.review.entity.ReviewPO;
import com.rally.db.review.entity.ScoreRecordPO;
import com.rally.db.review.service.ReviewService;
import com.rally.db.review.service.ScoreRecordService;
import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.recap.gateway.RecapGateway;
import com.rally.domain.recap.model.ReviewData;
import com.rally.domain.recap.model.ReviewSubmitCmd;
import com.rally.domain.recap.model.ScoreRecordData;
import com.rally.domain.recap.model.ScoreSubmitCmd;
import com.rally.domain.user.gateway.UserGateway;
import com.rally.domain.user.model.UserData;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 赛后收集网关实现
 * <p>
 * 职责：
 * 1. 加载聚合根所需数据（评价、比分）
 * 2. 封装评价 diff 判断与落库
 * 3. 封装比分版本校验、diff 判断与落库
 */
@Component
@RequiredArgsConstructor
public class RecapRepository implements RecapGateway {

    private final ReviewService reviewService;
    private final ScoreRecordService scoreRecordService;
    private final UserGateway userGateway;

    private static final ReviewConvertMapper MAPPER = ReviewConvertMapper.INSTANCE;



    // ==================== 评价提交 ====================

    @Override
    public void submitReviewItems(String meetupId, String fromUserId, List<ReviewSubmitCmd.ReviewItem> targetReviews) {
        for (ReviewSubmitCmd.ReviewItem item : targetReviews) {
            // 查询是否已存在相同维度的评价（meetupId + fromUserId + toUserId + reviewType）
            ReviewPO existing = reviewService.lambdaQuery()
                    .eq(ReviewPO::getRallyMeetupId, meetupId)
                    .eq(ReviewPO::getFromUserId, fromUserId)
                    .eq(ReviewPO::getToUserId, item.getToUserId())
                    .eq(ReviewPO::getReviewType, item.getType().name())
                    .one();

            if (existing != null) {
                // 存在则更新评价值，利用唯一索引保证原子性
                reviewService.lambdaUpdate()
                        .eq(ReviewPO::getId, existing.getId())
                        .set(ReviewPO::getReviewValue, item.getValue())
                        .update();
            } else {
                // 不存在则插入，若并发导致重复则由唯一索引 uk_review_dim 兜底
                ReviewPO newReview = MAPPER.toReviewPO(item, IdWorker.getIdStr(), meetupId, fromUserId);
                reviewService.save(newReview);
            }
        }
    }

    // ==================== 比分提交 ====================

    @Override
    public void submitScoreItems(String meetupId, String userId, List<ScoreSubmitCmd.ScoreItem> targetScores, Integer clientVersion, LocalDateTime meetupDate, String venueName) {
        // 查询该 meetup 已有的比分记录（按盘号索引）
        Map<Integer, ScoreRecordPO> existingMap = scoreRecordService.lambdaQuery()
                .eq(ScoreRecordPO::getRallyMeetupId, meetupId)
                .list()
                .stream()
                .collect(Collectors.toMap(ScoreRecordPO::getSetNumber, po -> po));

        // 批量查询所有选手的用户信息（昵称、头像）
        Map<String, UserData> userMap = batchQueryUsers(targetScores);

        List<ScoreRecordPO> toInsert = new ArrayList<>();
        List<ScoreRecordPO> toUpdate = new ArrayList<>();

        for (ScoreSubmitCmd.ScoreItem item : targetScores) {
            ScoreRecordPO existing = existingMap.get(item.getSetNum());
            if (existing != null) {
                // 已有该盘记录，准备更新（乐观锁由 updateById 自动处理）
                existing.setSetFormat(item.getSetFormatType());
                existing.setSideAPlayer1(item.getSideAPlayer1());
                existing.setSideAPlayer2(item.getSideAPlayer2());
                existing.setSideBPlayer1(item.getSideBPlayer1());
                existing.setSideBPlayer2(item.getSideBPlayer2());
                existing.setSideAScore(item.getSideAScore());
                existing.setSideBScore(item.getSideBScore());
                existing.setRecordedBy(userId);
                existing.setMatchType(item.getMatchType());
                existing.setMeetupDate(meetupDate);
                existing.setVenueName(venueName);
                // 从用户服务填充昵称和头像
                fillUserinfo(existing, userMap);
                toUpdate.add(existing);
            } else {
                // 新盘记录，准备插入
                ScoreRecordPO newRecord = MAPPER.toScoreRecordPO(item, IdWorker.getIdStr(), meetupId, userId);
                newRecord.setMatchType(item.getMatchType());
                newRecord.setMeetupDate(meetupDate);
                newRecord.setVenueName(venueName);
                // 从用户服务填充昵称和头像
                fillUserinfo(newRecord, userMap);
                toInsert.add(newRecord);
            }
        }

        // 批量插入新盘
        if (!toInsert.isEmpty()) {
            scoreRecordService.saveBatch(toInsert);
        }
        // 逐条更新已有盘（乐观锁：WHERE version = ?）
        for (ScoreRecordPO record : toUpdate) {
            boolean success = scoreRecordService.updateById(record);
            Assert.isTrue(success, BizErrorCode.SCORE_VERSION_MISMATCH);
        }
    }

    /**
     * 批量查询所有选手的用户信息
     */
    private Map<String, UserData> batchQueryUsers(List<ScoreSubmitCmd.ScoreItem> targetScores) {
        // 收集所有非空的选手 userId（去重）
        List<String> userIds = targetScores.stream()
                .flatMap(item -> Stream.of(item.getSideAPlayer1(), item.getSideAPlayer2(), item.getSideBPlayer1(), item.getSideBPlayer2()))
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        // 批量查询用户数据
        Map<String, UserData> userMap = new HashMap<>();
        for (String uid : userIds) {
            userGateway.findByUserId(uid).ifPresent(data -> userMap.put(uid, data));
        }
        return userMap;
    }

    /**
     * 从用户数据填充 PO 的昵称和头像字段
     */
    private void fillUserinfo(ScoreRecordPO po, Map<String, UserData> userMap) {
        UserData a1 = userMap.get(po.getSideAPlayer1());
        if (a1 != null) {
            po.setSideAPlayer1Nickname(a1.getNickname());
            po.setSideAPlayer1Avatar(a1.getAvatarUrl());
        }
        if (StringUtils.isNotBlank(po.getSideAPlayer2())) {
            UserData a2 = userMap.get(po.getSideAPlayer2());
            if (a2 != null) {
                po.setSideAPlayer2Nickname(a2.getNickname());
                po.setSideAPlayer2Avatar(a2.getAvatarUrl());
            }
        }
        UserData b1 = userMap.get(po.getSideBPlayer1());
        if (b1 != null) {
            po.setSideBPlayer1Nickname(b1.getNickname());
            po.setSideBPlayer1Avatar(b1.getAvatarUrl());
        }
        if (StringUtils.isNotBlank(po.getSideBPlayer2())) {
            UserData b2 = userMap.get(po.getSideBPlayer2());
            if (b2 != null) {
                po.setSideBPlayer2Nickname(b2.getNickname());
                po.setSideBPlayer2Avatar(b2.getAvatarUrl());
            }
        }
    }

    // ==================== 查询 ====================

    @Override
    public List<ReviewData> listReviewsByMeetupAndFrom(String meetupId, String fromUserId) {
        List<ReviewPO> poList = reviewService.lambdaQuery()
                .eq(ReviewPO::getRallyMeetupId, meetupId)
                .eq(ReviewPO::getFromUserId, fromUserId)
                .list();
        return MAPPER.toReviewDataList(poList);
    }

    @Override
    public List<ScoreRecordData> listScoresByMeetup(String meetupId) {
        List<ScoreRecordPO> poList = scoreRecordService.lambdaQuery()
                .eq(ScoreRecordPO::getRallyMeetupId, meetupId)
                .orderByAsc(ScoreRecordPO::getSetNumber)
                .list();
        return MAPPER.toScoreRecordDataList(poList);
    }
}
