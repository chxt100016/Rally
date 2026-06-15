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
import com.rally.domain.recap.model.ScoreAddCmd;
import com.rally.domain.recap.model.ScoreRecordData;
import com.rally.domain.recap.model.ScoreUpdateCmd;
import com.rally.domain.user.gateway.UserGateway;
import com.rally.domain.user.model.UserData;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public void submitReviewItems(String meetupId, String fromUserId, String toUserId, List<ReviewSubmitCmd.ReviewItem> targetReviews) {
        for (ReviewSubmitCmd.ReviewItem item : targetReviews) {
            // 查询是否已存在相同维度的评价（meetupId + fromUserId + toUserId + reviewType）
            ReviewPO existing = reviewService.lambdaQuery()
                    .eq(ReviewPO::getRallyMeetupId, meetupId)
                    .eq(ReviewPO::getFromUserId, fromUserId)
                    .eq(ReviewPO::getToUserId, toUserId)
                    .eq(ReviewPO::getReviewType, item.getType().name())
                    .one();

            if (existing != null) {
                // 存在则更新评价值（TAG 为逗号分隔的整串），不删旧
                reviewService.lambdaUpdate()
                        .eq(ReviewPO::getId, existing.getId())
                        .set(ReviewPO::getReviewValue, item.getValue())
                        .update();
            } else {
                // 不存在则插入，若并发导致重复则由唯一索引 uk_review_dim 兜底
                ReviewPO newReview = MAPPER.toReviewPO(item, IdWorker.getIdStr(), meetupId, fromUserId, toUserId);
                reviewService.save(newReview);
            }
        }
    }

    // ==================== 比分增删改 ====================

    @Override
    public void addScore(String meetupId, String userId, ScoreAddCmd cmd, LocalDateTime meetupDate, String venueName) {
        // 同场同盘唯一校验（uk_meetup_set 兜底，这里先行拦截给出友好错误）
        long exists = scoreRecordService.lambdaQuery().eq(ScoreRecordPO::getRallyMeetupId, meetupId).eq(ScoreRecordPO::getSetNumber, cmd.getSetNum()).count();
        Assert.isTrue(exists == 0, BizErrorCode.SCORE_SET_DUPLICATE);

        ScoreRecordPO record = MAPPER.toScoreRecordPO(cmd, IdWorker.getIdStr(), meetupId, userId);
        record.setMeetupDate(meetupDate);
        record.setVenueName(venueName);
        fillUserinfo(record, queryUsers(cmd.getSideAPlayer1(), cmd.getSideAPlayer2(), cmd.getSideBPlayer1(), cmd.getSideBPlayer2()));
        scoreRecordService.save(record);
    }

    @Override
    public void updateScore(String meetupId, String userId, ScoreUpdateCmd cmd, LocalDateTime meetupDate, String venueName) {
        // 按 bizId 定位记录（雪花永不复用，定位到的就是用户当初看到的那条，天然防 ABA）
        ScoreRecordPO existing = scoreRecordService.lambdaQuery().eq(ScoreRecordPO::getBizId, cmd.getBizId()).eq(ScoreRecordPO::getRallyMeetupId, meetupId).one();
        Assert.notNull(existing, BizErrorCode.RECAP_SCORE_NOT_FOUND);
        // 客户端版本须与库内一致，杜绝同一条记录的并发覆盖（lost update）
        Assert.isTrue(existing.getVersion().equals(cmd.getVersion()), BizErrorCode.SCORE_VERSION_MISMATCH);

        existing.setSetNumber(cmd.getSetNum());
        existing.setSetFormat(cmd.getSetFormatType());
        existing.setMatchType(cmd.getMatchType());
        existing.setSideAPlayer1(cmd.getSideAPlayer1());
        existing.setSideAPlayer2(cmd.getSideAPlayer2());
        existing.setSideBPlayer1(cmd.getSideBPlayer1());
        existing.setSideBPlayer2(cmd.getSideBPlayer2());
        existing.setSideAScore(cmd.getSideAScore());
        existing.setSideBScore(cmd.getSideBScore());
        existing.setSideATiebreakScore(cmd.getSideATiebreakScore());
        existing.setSideBTiebreakScore(cmd.getSideBTiebreakScore());
        existing.setRecordedBy(userId);
        existing.setMeetupDate(meetupDate);
        existing.setVenueName(venueName);
        // 选手可能变化，先清空旧冗余昵称头像再重填
        clearUserinfo(existing);
        fillUserinfo(existing, queryUsers(cmd.getSideAPlayer1(), cmd.getSideAPlayer2(), cmd.getSideBPlayer1(), cmd.getSideBPlayer2()));

        // 乐观锁：updateById 自动追加 WHERE version = ? 并自增版本
        boolean success = scoreRecordService.updateById(existing);
        Assert.isTrue(success, BizErrorCode.SCORE_VERSION_MISMATCH);
    }

    @Override
    public void deleteScore(String meetupId, String bizId) {
        // 按 bizId 删除，幂等；bizId 永不复用，删除的必然是用户当初看到的那条
        scoreRecordService.lambdaUpdate().eq(ScoreRecordPO::getRallyMeetupId, meetupId).eq(ScoreRecordPO::getBizId, bizId).remove();
    }

    /**
     * 批量查询选手的用户信息（去重、过滤空）
     */
    private Map<String, UserData> queryUsers(String... playerIds) {
        List<String> userIds = Stream.of(playerIds).filter(StringUtils::isNotBlank).distinct().toList();
        Map<String, UserData> userMap = new HashMap<>();
        for (String uid : userIds) {
            userGateway.findByUserId(uid).ifPresent(data -> userMap.put(uid, data));
        }
        return userMap;
    }

    /**
     * 清空 PO 的冗余昵称与头像字段（修改选手前调用，避免残留旧选手信息）
     */
    private void clearUserinfo(ScoreRecordPO po) {
        po.setSideAPlayer1Nickname(null);
        po.setSideAPlayer1Avatar(null);
        po.setSideAPlayer2Nickname(null);
        po.setSideAPlayer2Avatar(null);
        po.setSideBPlayer1Nickname(null);
        po.setSideBPlayer1Avatar(null);
        po.setSideBPlayer2Nickname(null);
        po.setSideBPlayer2Avatar(null);
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

    @Override
    public List<ScoreRecordData> listScoresByUserId(String userId) {
        List<ScoreRecordPO> poList = scoreRecordService.lambdaQuery()
                .and(wrapper -> wrapper.eq(ScoreRecordPO::getSideAPlayer1, userId).or().eq(ScoreRecordPO::getSideAPlayer2, userId).or().eq(ScoreRecordPO::getSideBPlayer1, userId).or().eq(ScoreRecordPO::getSideBPlayer2, userId))
                .orderByDesc(ScoreRecordPO::getMeetupDate)
                .last("limit 10")
                .list();
        return MAPPER.toScoreRecordDataList(poList);
    }
}
