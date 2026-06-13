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
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public void submitScoreItems(String meetupId, String userId, List<ScoreSubmitCmd.ScoreItem> targetScores, Integer clientVersion) {
        // 查询该 meetup 已有的比分记录（按盘号索引）
        Map<Integer, ScoreRecordPO> existingMap = scoreRecordService.lambdaQuery()
                .eq(ScoreRecordPO::getRallyMeetupId, meetupId)
                .list()
                .stream()
                .collect(Collectors.toMap(ScoreRecordPO::getSetNumber, po -> po));

        List<ScoreRecordPO> toInsert = new ArrayList<>();
        List<ScoreRecordPO> toUpdate = new ArrayList<>();

        for (ScoreSubmitCmd.ScoreItem item : targetScores) {
            ScoreRecordPO existing = existingMap.get(item.getSetNum());
            if (existing != null) {
                // 已有该盘记录，准备更新（乐观锁由 updateById 自动处理）
                existing.setSetFormat(item.getSetFormat());
                existing.setSideAPlayer1(item.getSideAPlayer1());
                existing.setSideAPlayer1Nickname(item.getSideAPlayer1Nickname());
                existing.setSideAPlayer1Avatar(item.getSideAPlayer1Avatar());
                existing.setSideAPlayer2(item.getSideAPlayer2());
                existing.setSideAPlayer2Nickname(item.getSideAPlayer2Nickname());
                existing.setSideAPlayer2Avatar(item.getSideAPlayer2Avatar());
                existing.setSideBPlayer1(item.getSideBPlayer1());
                existing.setSideBPlayer1Nickname(item.getSideBPlayer1Nickname());
                existing.setSideBPlayer1Avatar(item.getSideBPlayer1Avatar());
                existing.setSideBPlayer2(item.getSideBPlayer2());
                existing.setSideBPlayer2Nickname(item.getSideBPlayer2Nickname());
                existing.setSideBPlayer2Avatar(item.getSideBPlayer2Avatar());
                existing.setSideAScore(item.getSideAScore());
                existing.setSideBScore(item.getSideBScore());
                existing.setRecordedBy(userId);
                toUpdate.add(existing);
            } else {
                // 新盘记录，准备插入
                ScoreRecordPO newRecord = MAPPER.toScoreRecordPO(item, IdWorker.getIdStr(), meetupId, userId);
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
