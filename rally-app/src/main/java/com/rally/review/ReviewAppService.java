package com.rally.review;

import com.rally.cache.UserContext;
import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.config.gateway.ConfigGateway;
import com.rally.domain.meetup.gateway.MeetupGateway;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.review.enums.AttendanceEnum;
import com.rally.domain.review.enums.NtrpVoteEnum;
import com.rally.domain.review.enums.ReviewTypeEnum;
import com.rally.domain.review.gateway.ReviewGateway;
import com.rally.domain.review.gateway.ScoreRecordGateway;
import com.rally.domain.review.model.*;
import com.rally.domain.score.gateway.ScoreManager;
import com.rally.domain.user.gateway.UserGateway;
import com.rally.domain.user.gateway.UserProfileGateway;
import com.rally.domain.user.model.UserData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 评价编排服务：提交评价、可评价人列表、收到评价聚合
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewAppService {

    private final ReviewGateway reviewGateway;
    private final MeetupGateway meetupGateway;
    private final ConfigGateway configGateway;
    private final ScoreManager scoreManager;
    private final UserGateway userGateway;
    private final UserProfileGateway userProfileGateway;

    /**
     * 提交评价（本人对某人，一次覆盖全部维度）
     * 同事务：评价落库 + 触发评分重算
     */
    @Transactional
    public void submitReview(ReviewCmd cmd) {
        String operatorId = UserContext.get();
        String rallyMeetupId = cmd.getRallyMeetupId();
        String toUserId = cmd.getToUserId();

        // 1. 自评禁止
        if (operatorId.equals(toUserId)) {
            throw new BusinessException(BizErrorCode.REVIEW_SELF_FORBIDDEN);
        }

        // 2. 约球是否存在
        MeetupData meetup = meetupGateway.findByBizId(rallyMeetupId);
        if (meetup == null) {
            throw new BusinessException(BizErrorCode.MEETUP_NOT_FOUND);
        }

        // 3. 约球是否已结束（懒判定：end_time < NOW()）
        if (!meetupGateway.isFinished(rallyMeetupId)) {
            throw new BusinessException(BizErrorCode.MEETUP_NOT_FINISHED);
        }

        // 4. 截止时间校验
        int deadlineDays = configGateway.getInt("review.deadline_days", 30);
        LocalDateTime deadlineAt = meetup.getEndTime().plusDays(deadlineDays);
        if (LocalDateTime.now().isAfter(deadlineAt)) {
            throw new BusinessException(BizErrorCode.REVIEW_DEADLINE_PASSED);
        }

        // 5. 参与者校验（from 和 to 都必须是参与者）
        if (!meetupGateway.isParticipant(rallyMeetupId, operatorId)) {
            throw new BusinessException(BizErrorCode.REVIEW_NOT_PARTICIPANT);
        }
        if (!meetupGateway.isParticipant(rallyMeetupId, toUserId)) {
            throw new BusinessException(BizErrorCode.REVIEW_NOT_PARTICIPANT);
        }

        // 6. 提交评价：ntrp_vote（必选，一行）
        List<String> ntrpValues = List.of(
                (cmd.getNtrpVote() != null ? cmd.getNtrpVote() : NtrpVoteEnum.SAME).name().toLowerCase());
        reviewGateway.upsert(rallyMeetupId, operatorId, toUserId,
                ReviewTypeEnum.NTRP_VOTE.name().toLowerCase(), ntrpValues);

        // 7. 提交评价：attendance（必选，一行）
        List<String> attendanceValues = List.of(
                (cmd.getAttendance() != null ? cmd.getAttendance() : AttendanceEnum.ON_TIME).name().toLowerCase());
        reviewGateway.upsert(rallyMeetupId, operatorId, toUserId,
                ReviewTypeEnum.ATTENDANCE.name().toLowerCase(), attendanceValues);

        // 8. 提交评价：tag（选填，多行）
        List<String> tags = cmd.getTags();
        if (tags != null && !tags.isEmpty()) {
            // 手动标签长度校验
            int maxLength = configGateway.getInt("review.tag.max_length", 8);
            int maxCustom = configGateway.getInt("review.tag.max_custom_per_review", 3);
            if (tags.size() > maxCustom) {
                throw new BusinessException(BizErrorCode.PARAM_ERROR, "手动标签数量不超过" + maxCustom + "个");
            }
            for (String tag : tags) {
                if (tag != null && tag.length() > maxLength) {
                    throw new BusinessException(BizErrorCode.PARAM_ERROR, "标签长度不超过" + maxLength + "个字符");
                }
            }
            reviewGateway.upsert(rallyMeetupId, operatorId, toUserId,
                    ReviewTypeEnum.TAG.name().toLowerCase(), tags);
        } else {
            // 没有标签也要清空旧行
            reviewGateway.upsert(rallyMeetupId, operatorId, toUserId,
                    ReviewTypeEnum.TAG.name().toLowerCase(), List.of());
        }

        // 9. 触发评分重算（同事务）
        try {
            scoreManager.recalc(rallyMeetupId);
        } catch (Exception e) {
            log.error("评价触发评分重算失败，meetupId={}", rallyMeetupId, e);
            throw new BusinessException(BizErrorCode.OPERATION_FAILED, "评分计算异常，请重试");
        }
    }

    /**
     * 获取某场我可评价的人列表与已评状态
     */
    public ReviewableListVO getReviewableList(String rallyMeetupId) {
        String operatorId = UserContext.get();

        // 1. 约球是否存在
        MeetupData meetup = meetupGateway.findByBizId(rallyMeetupId);
        if (meetup == null) {
            throw new BusinessException(BizErrorCode.MEETUP_NOT_FOUND);
        }

        // 2. 判断截止状态
        int deadlineDays = configGateway.getInt("review.deadline_days", 30);
        LocalDateTime deadlineAt = meetup.getEndTime().plusDays(deadlineDays);
        boolean deadlinePassed = LocalDateTime.now().isAfter(deadlineAt);

        // 3. 获取参与者列表（发布者 + 已批准报名者），排除自己
        List<String> participantIds = meetupGateway.listParticipantUserIds(rallyMeetupId);
        List<String> targetIds = participantIds.stream()
                .filter(uid -> !uid.equals(operatorId))
                .toList();

        // 4. 系统默认标签池
        String tagPoolJson = configGateway.getString("review.tag.default_pool",
                "[\"正手稳\",\"反手稳\",\"发球好\",\"底线稳\",\"网前好\",\"移动快\",\"友善\"]");
        List<String> defaultTagPool;
        try {
            defaultTagPool = com.alibaba.fastjson2.JSON.parseArray(tagPoolJson, String.class);
        } catch (Exception e) {
            defaultTagPool = List.of("正手稳", "反手稳", "发球好", "底线稳", "网前好", "移动快", "友善");
        }

        int suggestCount = configGateway.getInt("review.tag.random_pick_count", 3);

        // 5. 构建每个可评价人的信息
        List<ReviewablePersonVO> people = new ArrayList<>();
        for (String toUserId : targetIds) {
            ReviewablePersonVO person = buildReviewablePerson(
                    rallyMeetupId, operatorId, toUserId, defaultTagPool, suggestCount);
            people.add(person);
        }

        // 6. 组装返回
        ReviewableListVO vo = new ReviewableListVO();
        vo.setDeadlinePassed(deadlinePassed);
        vo.setDeadlineAt(deadlineAt);
        vo.setPeople(people);
        return vo;
    }

    /**
     * 获取某人收到的评价（球员主页三区分组）
     */
    public ReviewVO getReceivedReviews(String toUserId) {
        ReviewVO vo = new ReviewVO();
        vo.setToUserId(toUserId);

        // NTRP 投票统计
        Map<String, Long> ntrpVoteMap = new LinkedHashMap<>();
        ntrpVoteMap.put("higher", 0L);
        ntrpVoteMap.put("same", 0L);
        ntrpVoteMap.put("lower", 0L);
        List<Object[]> ntrpCounts = reviewGateway.countByToUserGroupByValue(toUserId,
                ReviewTypeEnum.NTRP_VOTE.name().toLowerCase());
        for (Object[] row : ntrpCounts) {
            ntrpVoteMap.put((String) row[0], (Long) row[1]);
        }
        vo.setNtrpVote(ntrpVoteMap);

        // 出勤统计
        Map<String, Long> attendanceMap = new LinkedHashMap<>();
        attendanceMap.put("on_time", 0L);
        attendanceMap.put("late", 0L);
        attendanceMap.put("no_show", 0L);
        List<Object[]> attendanceCounts = reviewGateway.countByToUserGroupByValue(toUserId,
                ReviewTypeEnum.ATTENDANCE.name().toLowerCase());
        for (Object[] row : attendanceCounts) {
            attendanceMap.put((String) row[0], (Long) row[1]);
        }
        vo.setAttendance(attendanceMap);

        // 标签统计（带次数，按 count 降序）
        List<Object[]> tagCounts = reviewGateway.countByToUserGroupByValue(toUserId,
                ReviewTypeEnum.TAG.name().toLowerCase());
        List<ReviewVO.TagCount> tagList = tagCounts.stream()
                .map(row -> new ReviewVO.TagCount((String) row[0], (Long) row[1]))
                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                .toList();
        vo.setTags(tagList);

        return vo;
    }

    // ==================== 内部方法 ====================

    /**
     * 构建单个可评价人信息
     */
    private ReviewablePersonVO buildReviewablePerson(String rallyMeetupId, String fromUserId,
                                                      String toUserId, List<String> defaultTagPool,
                                                      int suggestCount) {
        ReviewablePersonVO person = new ReviewablePersonVO();
        person.setToUserId(toUserId);

        // 查询用户信息
        UserData userData = userGateway.findByUserId(toUserId).orElse(null);
        if (userData != null) {
            person.setNickname(userData.getNickname());
            person.setAvatarUrl(userData.getAvatarUrl());
        }

        // 查询我已提交的评价
        List<ReviewData> existingReviews = reviewGateway.listByMeetupAndFrom(rallyMeetupId, fromUserId);
        Map<String, List<ReviewData>> grouped = existingReviews.stream()
                .collect(Collectors.groupingBy(r -> r.getReviewType().name().toLowerCase()));

        // 是否已评（任一维度存在即 true）
        person.setReviewed(!existingReviews.isEmpty());

        // 回填 ntrpVote
        List<ReviewData> ntrpReviews = grouped.getOrDefault(ReviewTypeEnum.NTRP_VOTE.name().toLowerCase(), List.of());
        if (!ntrpReviews.isEmpty()) {
            person.setNtrpVote(NtrpVoteEnum.valueOf(ntrpReviews.get(0).getReviewValue().toUpperCase()));
        }

        // 回填 attendance
        List<ReviewData> attendanceReviews = grouped.getOrDefault(ReviewTypeEnum.ATTENDANCE.name().toLowerCase(), List.of());
        if (!attendanceReviews.isEmpty()) {
            person.setAttendance(AttendanceEnum.valueOf(attendanceReviews.get(0).getReviewValue().toUpperCase()));
        }

        // 回填 tags
        List<ReviewData> tagReviews = grouped.getOrDefault(ReviewTypeEnum.TAG.name().toLowerCase(), List.of());
        person.setTags(tagReviews.stream().map(ReviewData::getReviewValue).toList());

        // 标签推荐池 = 系统默认全集 ∪ 被评价人历史标签随机挑3
        person.setTagSuggestions(buildTagSuggestions(toUserId, defaultTagPool, suggestCount));

        return person;
    }

    /**
     * 构建标签推荐池：系统默认全集 ∪ 被评价人历史标签随机挑3
     */
    private List<String> buildTagSuggestions(String toUserId, List<String> defaultTagPool, int suggestCount) {
        // 获取被评价人历史标签（去重）
        List<String> historicalTags = reviewGateway.listDistinctValuesByToUserAndType(
                toUserId, ReviewTypeEnum.TAG.name().toLowerCase());

        // 剔除已在系统默认中的
        Set<String> defaultSet = new HashSet<>(defaultTagPool);
        List<String> candidates = historicalTags.stream()
                .filter(t -> !defaultSet.contains(t))
                .toList();

        // 随机洗牌取前 suggestCount 个
        List<String> shuffled = new ArrayList<>(candidates);
        Collections.shuffle(shuffled);
        List<String> randomPicked = shuffled.stream().limit(suggestCount).toList();

        // 合并：系统默认 + 随机挑的
        List<String> suggestions = new ArrayList<>(defaultTagPool);
        suggestions.addAll(randomPicked);
        return suggestions;
    }
}
