package com.rally.domain.meetup.peerreview;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 一位评价人在一场约球中的完整评价集合聚合根。
 *
 * <p>仓储必须按 {@code rally_meetup_id/from_user_id} 装载和保存完整集合；
 * {@link #submit(SubmitPeerReviewsCommand, PeerReviewIdGenerator)} 是唯一状态变更入口。</p>
 */
public final class PeerReviewSet {

    public static final String PEER_REVIEW_DIMENSION_CONFLICT =
            "PEER_REVIEW_DIMENSION_CONFLICT";
    public static final String PEER_REVIEW_TARGET_INVALID =
            "PEER_REVIEW_TARGET_INVALID";
    public static final String PEER_REVIEW_VALUE_INVALID =
            "PEER_REVIEW_VALUE_INVALID";
    public static final String PEER_REVIEW_BATCH_DUPLICATE =
            "PEER_REVIEW_BATCH_DUPLICATE";
    public static final String PEER_REVIEW_NOT_ALLOWED =
            "PEER_REVIEW_NOT_ALLOWED";
    public static final String PEER_REVIEW_BATCH_EMPTY =
            "PEER_REVIEW_BATCH_EMPTY";

    private final String meetupId;
    private final String fromUserId;
    private final Map<DimensionKey, PeerReviewItem> itemsByDimension;

    private PeerReviewSet(
            String meetupId,
            String fromUserId,
            Collection<PeerReviewItem> items) {
        requireNotBlank(meetupId, PEER_REVIEW_DIMENSION_CONFLICT, "约球编号不能为空");
        requireNotBlank(fromUserId, PEER_REVIEW_TARGET_INVALID, "评价人编号不能为空");
        this.meetupId = meetupId;
        this.fromUserId = fromUserId;
        this.itemsByDimension = indexItems(items);
        checkInvariants(this.itemsByDimension);
    }

    /** 从仓储按同一约球与评价人载入的完整评价集合恢复聚合。 */
    public static PeerReviewSet restore(
            String meetupId,
            String fromUserId,
            Collection<PeerReviewItem> items) {
        return new PeerReviewSet(meetupId, fromUserId, items);
    }

    /** 建立尚无评价项的集合。 */
    public static PeerReviewSet empty(String meetupId, String fromUserId) {
        return new PeerReviewSet(meetupId, fromUserId, List.of());
    }

    /**
     * C1：整体校验后新增或覆盖一批评价项。
     *
     * <p>方法在生成候选集合并通过 I1-I4 前不会改写当前聚合，因而任一项失败时整批不变。</p>
     */
    public List<PeerReviewItem> submit(
            SubmitPeerReviewsCommand command,
            PeerReviewIdGenerator idGenerator) {
        require(command != null, PEER_REVIEW_VALUE_INVALID, "提交评价命令不能为空");
        require(command.eligibility() != null && command.eligibility().isAllowed(),
                PEER_REVIEW_NOT_ALLOWED,
                "评价人无资格或已超过评价截止时间");

        List<PeerReviewSubmission> submissions = command.submissions();
        Map<DimensionKey, PeerReviewItem> candidate =
                new LinkedHashMap<>(itemsByDimension);
        Set<String> candidateBusinessIds = collectBusinessIds(candidate.values());
        List<PeerReviewItem> changed = new ArrayList<>(submissions.size());
        for (PeerReviewSubmission submission : submissions) {
            requireNotBlank(submission.toUserId(),
                    PEER_REVIEW_TARGET_INVALID,
                    "目标用户编号不能为空");
            require(submission.reviewType() != null,
                    PEER_REVIEW_VALUE_INVALID,
                    "评价维度不能为空");

            DimensionKey key = new DimensionKey(
                    submission.toUserId(), submission.reviewType());
            String normalizedValue = normalizeValue(
                    submission.reviewType(), submission.reviewValue());
            PeerReviewItem current = candidate.get(key);
            PeerReviewItem next;
            if (current == null) {
                String businessId = nextUniqueBusinessId(idGenerator, candidateBusinessIds);
                next = PeerReviewItem.create(
                        businessId,
                        meetupId,
                        fromUserId,
                        submission,
                        normalizedValue);
                candidateBusinessIds.add(businessId);
            } else {
                next = current.withReviewValue(normalizedValue);
            }
            candidate.put(key, next);
            changed.add(next);
        }

        checkInvariants(candidate);
        itemsByDimension.clear();
        itemsByDimension.putAll(candidate);
        return List.copyOf(changed);
    }

    public String meetupId() {
        return meetupId;
    }

    public String fromUserId() {
        return fromUserId;
    }

    public PeerReviewStatus status() {
        return itemsByDimension.isEmpty()
                ? PeerReviewStatus.EMPTY
                : PeerReviewStatus.RECORDED;
    }

    /** 聚合当前完整不可变快照，供仓储批量 upsert。 */
    public List<PeerReviewItem> items() {
        return List.copyOf(itemsByDimension.values());
    }

    /**
     * 返回已有至少一个合法维度的去重目标集合。
     * 历史上已不属于当前有效参与者的目标不会进入结果。
     */
    public Set<String> coveredTargetUserIds(Set<String> validParticipantUserIds) {
        Set<String> validParticipants = copyValidParticipants(validParticipantUserIds);
        Set<String> covered = new LinkedHashSet<>();
        for (PeerReviewItem item : itemsByDimension.values()) {
            if (!Objects.equals(fromUserId, item.toUserId())
                    && validParticipants.contains(item.toUserId())
                    && isLegalValue(item.reviewType(), item.reviewValue())) {
                covered.add(item.toUserId());
            }
        }
        return Set.copyOf(covered);
    }

    /** 除评价人外没有遗漏有效参与者时即为全部覆盖；所需集合为空时返回 true。 */
    public boolean isFullyCovered(Set<String> validParticipantUserIds) {
        Set<String> requiredTargets = copyValidParticipants(validParticipantUserIds);
        requiredTargets.remove(fromUserId);
        return coveredTargetUserIds(validParticipantUserIds).containsAll(requiredTargets);
    }

    /** 将数据库 {@code uk_review_dim} 冲突转换为 I1 的稳定领域错误。 */
    public static PeerReviewDomainException dimensionConflict(Throwable cause) {
        return new PeerReviewDomainException(
                PEER_REVIEW_DIMENSION_CONFLICT,
                "同一约球、评价人、目标用户和评价维度最多只能存在一项",
                cause);
    }

    /** I1-I3：恢复和 C1 完成后校验集合内可独立判断的全部不变量。 */
    private void checkInvariants(Map<DimensionKey, PeerReviewItem> candidate) {
        Set<String> businessIds = new HashSet<>();
        for (Map.Entry<DimensionKey, PeerReviewItem> entry : candidate.entrySet()) {
            PeerReviewItem item = entry.getValue();
            require(item != null,
                    PEER_REVIEW_DIMENSION_CONFLICT,
                    "评价项不能为空");
            requireNotBlank(item.businessId(),
                    PEER_REVIEW_DIMENSION_CONFLICT,
                    "评价项业务编号不能为空");
            require(businessIds.add(item.businessId()),
                    PEER_REVIEW_DIMENSION_CONFLICT,
                    "评价项业务编号必须唯一");
            require(Objects.equals(meetupId, item.meetupId())
                            && Objects.equals(fromUserId, item.fromUserId()),
                    PEER_REVIEW_DIMENSION_CONFLICT,
                    "评价项不属于当前完整评价集合");
            require(Objects.equals(entry.getKey(),
                            new DimensionKey(item.toUserId(), item.reviewType())),
                    PEER_REVIEW_DIMENSION_CONFLICT,
                    "评价维度索引与评价项不一致");
            normalizeValue(item.reviewType(), item.reviewValue());
        }
    }

    private Map<DimensionKey, PeerReviewItem> indexItems(Collection<PeerReviewItem> items) {
        Map<DimensionKey, PeerReviewItem> indexed = new LinkedHashMap<>();
        if (items == null) {
            return indexed;
        }
        for (PeerReviewItem item : items) {
            require(item != null,
                    PEER_REVIEW_DIMENSION_CONFLICT,
                    "评价项不能为空");
            requireNotBlank(item.toUserId(),
                    PEER_REVIEW_TARGET_INVALID,
                    "目标用户编号不能为空");
            require(item.reviewType() != null,
                    PEER_REVIEW_VALUE_INVALID,
                    "评价维度不能为空");
            DimensionKey key = new DimensionKey(item.toUserId(), item.reviewType());
            require(indexed.putIfAbsent(key, item) == null,
                    PEER_REVIEW_DIMENSION_CONFLICT,
                    "同一目标与评价维度存在重复评价项");
        }
        return indexed;
    }

    private static Set<String> copyValidParticipants(Set<String> participantUserIds) {
        require(participantUserIds != null,
                PEER_REVIEW_TARGET_INVALID,
                "有效参与者集合不能为空");
        Set<String> copied = new LinkedHashSet<>();
        for (String participantUserId : participantUserIds) {
            requireNotBlank(participantUserId,
                    PEER_REVIEW_TARGET_INVALID,
                    "有效参与者编号不能为空");
            copied.add(participantUserId);
        }
        return copied;
    }

    private static Set<String> collectBusinessIds(Collection<PeerReviewItem> items) {
        Set<String> businessIds = new HashSet<>();
        for (PeerReviewItem item : items) {
            require(businessIds.add(item.businessId()),
                    PEER_REVIEW_DIMENSION_CONFLICT,
                    "评价项业务编号必须唯一");
        }
        return businessIds;
    }

    private static String nextUniqueBusinessId(
            PeerReviewIdGenerator idGenerator,
            Set<String> existingBusinessIds) {
        require(idGenerator != null,
                PEER_REVIEW_DIMENSION_CONFLICT,
                "评价项业务编号生成器不能为空");
        String businessId = idGenerator.nextReviewId();
        requireNotBlank(businessId,
                PEER_REVIEW_DIMENSION_CONFLICT,
                "生成的评价项业务编号不能为空");
        require(!existingBusinessIds.contains(businessId),
                PEER_REVIEW_DIMENSION_CONFLICT,
                "生成的评价项业务编号已存在");
        return businessId;
    }

    static String normalizeValue(PeerReviewType type, String value) {
        require(type != null, PEER_REVIEW_VALUE_INVALID, "评价维度不能为空");
        return switch (type) {
            case LEVEL_VOTE -> LevelVote.parse(value).storageValue();
            case ATTENDANCE_VOTE -> AttendanceVote.parse(value).storageValue();
            case TAG -> normalizeTag(value);
        };
    }

    private static String normalizeTag(String value) {
        require(value != null,
                PEER_REVIEW_VALUE_INVALID,
                "标签值不能为空");
        return value;
    }

    private static boolean isLegalValue(PeerReviewType type, String value) {
        try {
            return Objects.equals(value, normalizeValue(type, value));
        } catch (PeerReviewDomainException ignored) {
            return false;
        }
    }

    private static void require(boolean condition, String identifier, String message) {
        if (!condition) {
            throw new PeerReviewDomainException(identifier, message);
        }
    }

    private static void requireNotBlank(String value, String identifier, String message) {
        require(value != null && !value.strip().isEmpty(), identifier, message);
    }

    private record DimensionKey(String toUserId, PeerReviewType reviewType) {
    }

}
