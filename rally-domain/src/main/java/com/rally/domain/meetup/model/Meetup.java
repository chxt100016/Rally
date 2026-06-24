package com.rally.domain.meetup.model;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.meetup.enums.*;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.system.enums.SystemConfigKey;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.utils.Assert;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 约球聚合根（包含活动信息 + 所有参与者报名信息）
 * <p>
 * 所有针对约球和报名的领域操作都应通过此聚合根进行。
 */
@Getter
public class Meetup {

    private final MeetupData data;

    /** 所有参与者的报名记录（创建者、等待审批、已通过等） */
    private final List<RegistrationData> registrations;

    /**
     * 完整聚合根（约球数据 + 报名记录）
     */
    public Meetup(MeetupData data, List<RegistrationData> registrations) {
        this.data = data;
        this.registrations = registrations != null ? registrations : new ArrayList<>();
    }

    public String getMeetupId() {
        return this.data.getBizId();
    }

    // ======================== 约球状态判断 ========================

    /**
     * 懒判定：计算真实状态
     * - OPEN/FULL + endTime 已过 → FINISHED
     * - OPEN/FULL + startTime 已过 + endTime 未过 → ONGOING
     */
    public MeetupStatusEnum getRealStatus() {
        MeetupStatusEnum status = data.getStatus();
        if (status == MeetupStatusEnum.OPEN || status == MeetupStatusEnum.FULL) {
            LocalDateTime now = LocalDateTime.now();
            if (data.getEndTime().isBefore(now)) {
                return MeetupStatusEnum.FINISHED;
            }
            if (data.getStartTime().isBefore(now)) {
                return MeetupStatusEnum.ONGOING;
            }
        }
        return status;
    }

    /** 是否已过期（开始时间已过） */
    public boolean isExpired() {
        return data.getStartTime().isBefore(LocalDateTime.now());
    }

    /** 是否为活跃状态（非终态） */
    public boolean isActive() {
        MeetupStatusEnum realStatus = getRealStatus();
        return realStatus != MeetupStatusEnum.FINISHED && realStatus != MeetupStatusEnum.CLOSED;
    }

    /** 是否已满 */
    public boolean isFull() {
        return countApprovedPlayers() >= data.getMaxPlayers();
    }

    /**
     * 统计已批准的参与者数量（含创建者）
     */
    public int countApprovedPlayers() {
        return (int) registrations.stream()
                .filter(RegistrationData::isActiveParticipant)
                .count();
    }

    /** 获取创建人 userId */
    public String getCreatorId() {
        return data.getCreatorId();
    }

    /** 是否为创建人 */
    public boolean isCreator(String userId) {
        return userId.equals(data.getCreatorId());
    }

    /** 是否为参与者（创建人或已加入的报名用户） */
    public boolean isParticipant(String userId) {
        if (isCreator(userId)) {
            return true;
        }
        RegistrationData registration = findActiveRegistration(userId);
        return registration != null && registration.isActiveParticipant();
    }

    /** 是否应给该用户发送通知（仍是活动成员：创建人或有效参与者；已退出/被拒则不再通知） */
    public boolean shouldNotice(String userId) {
        return isParticipant(userId);
    }

    /** 是否存在创建人以外的有效参与者 */
    public boolean hasOtherParticipants() {
        return registrations.stream().filter(RegistrationData::isActiveParticipant).anyMatch(r -> !isCreator(r.getUserId()));
    }

    /** 断言当前用户为创建人，否则抛出异常 */
    public void assertOwner(String userId) {
        if (!isCreator(userId)) {
            throw new BusinessException(BizErrorCode.NOT_CREATOR);
        }
    }

    /**
     * 断言用户是活动参与者（创建者或已报名用户）
     * @param userId 用户ID
     */
    public void assertIn(String userId) {
        // 创建者有权限
        if (isCreator(userId)) {
            return;
        }
        // 已报名用户有权限
        RegistrationData registration = findActiveRegistration(userId);
        Assert.notNull(registration, BizErrorCode.NOT_JOINED);
    }

    /** 是否可编辑 */
    public boolean canEdit(String userId, int lockMinutes) {
        MeetupStatusEnum realStatus = getRealStatus();
        return isCreator(userId)
                && realStatus != MeetupStatusEnum.FINISHED
                && realStatus != MeetupStatusEnum.CLOSED
                && LocalDateTime.now().isBefore(data.getStartTime().minusMinutes(lockMinutes));
    }

    /**
     * 判断场地是否变更
     * @param cmd 编辑命令
     * @return true 表示场地变更
     */
    public boolean isLocationChanged(MeetupPublishCmd cmd) {
        // 场地名称变更
        if (cmd.getCourtName() != null && !cmd.getCourtName().equals(data.getCourtName())) {
            return true;
        }
        // 场地地址变更
        if (cmd.getCourtAddress() != null && !cmd.getCourtAddress().equals(data.getCourtAddress())) {
            return true;
        }
        // 经纬度变更
        if (cmd.getCourtLng() != null && cmd.getCourtLat() != null) {
            return !cmd.getCourtLng().equals(data.getCourtLng()) || !cmd.getCourtLat().equals(data.getCourtLat());
        }
        return false;
    }

    // ======================== 报名记录查询 ========================

    /** 查找用户的有效报名记录（pending/JOINED/REVIEWED） */
    public RegistrationData findActiveRegistration(String userId) {
        return registrations.stream()
                .filter(r -> userId.equals(r.getUserId()))
                .filter(r -> r.isPending() || r.isActiveParticipant())
                .findFirst().orElse(null);
    }



    // ======================== 报名领域行为 ========================

    /**
     * 报名（包含校验 + 创建报名记录）
     *
     * @param userProfile    用户档案领域对象
     * @param autoWithdrawAt 自动撤回时间，可为 null
     */
    public RegistrationStatusEnum join(UserProfile userProfile, LocalDateTime autoWithdrawAt) {
        // 1. 校验
        assertCanJoin(userProfile);

        // 2. 创建报名记录
        RegistrationData registration = new RegistrationData();
        registration.setRallyMeetupId(data.getBizId());
        registration.setUserId(userProfile.getUser().getUserId());
        registration.setExpiresAt(autoWithdrawAt);

        // 3. 根据加入模式设置状态
        registration.setStatus(data.getJoinMode() == JoinModeEnum.DIRECT ? RegistrationStatusEnum.JOINED : RegistrationStatusEnum.PENDING);
        this.registrations.add(registration);

        return registration.getStatus();

    }

    /**
     * 断言可以报名（校验约球状态、时间、创建人、重复报名、性别限制、信誉分门槛）
     * @param userProfile 用户档案领域对象
     */
    public void assertCanJoin(UserProfile userProfile) {
        String userId = userProfile.getUser().getUserId();
        MeetupStatusEnum realStatus = getRealStatus();

        // 1. 约球状态校验
        if (realStatus == MeetupStatusEnum.FULL) {
            throw new BusinessException(BizErrorCode.MEETUP_FULL);
        }
        if (realStatus == MeetupStatusEnum.CLOSED) {
            throw new BusinessException(BizErrorCode.MEETUP_CLOSED);
        }
        if (realStatus == MeetupStatusEnum.FINISHED) {
            throw new BusinessException(BizErrorCode.MEETUP_EXPIRED);
        }
        if (realStatus == MeetupStatusEnum.ONGOING) {
            throw new BusinessException(BizErrorCode.MEETUP_ONGOING);
        }

        // 2. 开始时间校验
        if (isExpired()) {
            throw new BusinessException(BizErrorCode.MEETUP_EXPIRED);
        }

        // 3. 不能报名自己的约球
        if (isCreator(userId)) {
            throw new BusinessException(BizErrorCode.CANNOT_JOIN_OWN);
        }

        // 4. 重复报名校验
        if (findActiveRegistration(userId) != null) {
            throw new BusinessException(BizErrorCode.ALREADY_JOINED);
        }

        // 5. 性别限制校验
        checkGenderLimit(userProfile);

        // 6. 信誉分门槛校验
        checkReputationScore(userProfile);

        // 7. 级别限制校验
        checkLevelLimit(userProfile);

    }

    private void checkLevelLimit(UserProfile userProfile) {
        if (data.getLevelMode() == null) {
            return;
        }
        if (userProfile.getProfile() == null || userProfile.getProfile().getNtrpScore() == null) {
            return;
        }
        BigDecimal userLevel = userProfile.getProfile().getNtrpScore();
        switch (data.getLevelMode()) {
            case RANGE:
                if ((data.getLevelMin() != null && userLevel.compareTo(data.getLevelMin()) < 0)
                        || (data.getLevelMax() != null && userLevel.compareTo(data.getLevelMax()) > 0)) {
                    throw new BusinessException(BizErrorCode.LEVEL_NOT_MATCH);
                }
                break;
            case EXACT:
                if (data.getLevelMin() != null && userLevel.compareTo(data.getLevelMin()) != 0) {
                    throw new BusinessException(BizErrorCode.LEVEL_NOT_MATCH);
                }
                break;
            case ABOVE:
                if (data.getLevelMin() != null && userLevel.compareTo(data.getLevelMin()) < 0) {
                    throw new BusinessException(BizErrorCode.LEVEL_NOT_MATCH);
                }
                break;
            case BELOW:
                if (data.getLevelMax() != null && userLevel.compareTo(data.getLevelMax()) > 0) {
                    throw new BusinessException(BizErrorCode.LEVEL_NOT_MATCH);
                }
                break;
            default:
                break;
        }
    }

    /**
     * 性别限制校验
     * @param userProfile 用户档案领域对象
     */
    public void checkGenderLimit(UserProfile userProfile) {
        if (data.getGenderLimit() == GenderLimitEnum.ANY || userProfile.getUser().getGender() == null) {
            return;
        }
        String userGender = userProfile.getUser().getGender().name();
        if (data.getGenderLimit() == GenderLimitEnum.MALE && !"MALE".equals(userGender)) {
            throw new BusinessException(BizErrorCode.GENDER_NOT_MATCH);
        }
        if (data.getGenderLimit() == GenderLimitEnum.FEMALE && !"FEMALE".equals(userGender)) {
            throw new BusinessException(BizErrorCode.GENDER_NOT_MATCH);
        }
    }

    /**
     * 信誉分门槛校验
     * @param userProfile 用户档案领域对象
     */
    public void checkReputationScore(UserProfile userProfile) {
        if (userProfile.getProfile() == null || userProfile.getProfile().getReputationScore() == null) {
            return;
        }
        Integer reputationScore = userProfile.getProfile().getReputationScore();
        int threshold = SystemConfig.getInt(SystemConfigKey.MEETUP_JOIN_MIN_REPUTATION_SCORE.getKey());
        if (reputationScore < threshold) {
            throw new BusinessException(BizErrorCode.LOW_REPUTATION_BANNED);
        }
    }

    // ======================== 报名操作（需聚合根上下文） ========================

    /**
     * 退出报名（已加入 → QUIT）
     * @param userId 当前用户 ID
     * @return 退出结果（是否在 6h 内需扣分）
     */
    public QuitResult quit(String userId) {
        // 1. 查找报名记录并校验
        RegistrationData registration = findActiveRegistration(userId);
        Assert.notNull(registration, BizErrorCode.NOT_JOINED);
        Assert.isTrue(registration.canQuit(), BizErrorCode.NOT_JOINED);

        // 2. 更新状态
        registration.setStatus(RegistrationStatusEnum.QUIT);

        // 3. 判断是否在 6h 内
        long hoursUntilStart = Duration.between(LocalDateTime.now(), data.getStartTime()).toHours();
        int thresholdHours = SystemConfig.getInt(SystemConfigKey.MEETUP_QUIT_PENALTY_THRESHOLD_HOURS.getKey());
        return hoursUntilStart < thresholdHours ? QuitResult.PENALIZED : QuitResult.NORMAL;
    }

    /**
     * 审批通过
     * @param registrationId 报名记录 ID
     * @param currentUserId 当前用户 ID（审批人）
     */
    public String approve(String registrationId, String currentUserId) {
        // 1. 查找报名记录
        RegistrationData registration = findRegistrationById(registrationId);
        Assert.notNull(registration, BizErrorCode.WAITLIST_NOT_FOUND);

        // 2. 权限校验
        assertOwner(currentUserId);

        // 3. 状态校验
        registration.assertCanReview();
        Assert.isTrue(isActive(), BizErrorCode.MEETUP_STATUS_ILLEGAL);

        // 4. 更新状态
        registration.setStatus(RegistrationStatusEnum.JOINED);

        return registration.getUserId();
    }

    /**
     * 审批拒绝
     * @param registrationId 报名记录 ID
     * @param currentUserId 当前用户 ID（审批人）
     */
    public void reject(String registrationId, String currentUserId) {
        // 1. 查找报名记录
        RegistrationData registration = findRegistrationById(registrationId);
        Assert.notNull(registration, BizErrorCode.WAITLIST_NOT_FOUND);

        // 2. 权限校验
        assertOwner(currentUserId);

        // 3. 状态校验
        registration.assertCanReview();

        // 4. 更新状态
        registration.setStatus(RegistrationStatusEnum.REJECTED);
    }

    /** 按 registrationId 在聚合根内查找报名记录 */
    private RegistrationData findRegistrationById(String registrationId) {
        return registrations.stream()
                .filter(r -> registrationId.equals(r.getBizId()))
                .findFirst().orElse(null);
    }

    // ======================== 操作状态计算 ========================

    /**
     * 计算当前用户的操作状态（沉淀到聚合根，内部已有所有数据）
     * @param currentUserId 当前用户 ID
     * @return 操作状态枚举
     */
    public ActionStateEnum getActionState(String currentUserId) {
        MeetupStatusEnum realStatus = getRealStatus();
        boolean isCreator = isCreator(currentUserId);

        // 创建人 + 无其他参与者：永远处于可编辑状态
        if (isCreator && !hasOtherParticipants()) {
            return ActionStateEnum.OWNER_EDITABLE;
        }

        // 终态：参与者按是否已评价返回不同状态，其余置灰
        if (realStatus == MeetupStatusEnum.FINISHED) {
            if (!isParticipant(currentUserId)) return ActionStateEnum.FINISHED;
            return hasReview(currentUserId) ? ActionStateEnum.FINISHED_REVIEWED : ActionStateEnum.FINISHED_JOINED;
        }
        if (realStatus == MeetupStatusEnum.CLOSED) {
            return isParticipant(currentUserId) ? ActionStateEnum.CLOSED_JOINED : ActionStateEnum.CLOSED;
        }

        // 创建人视角（此时必有其他参与者）
        if (isCreator) {
            if (realStatus == MeetupStatusEnum.ONGOING) {
                return ActionStateEnum.ONGOING_JOINED;
            }
            int lockMinutes = SystemConfig.getInt(SystemConfigKey.MEETUP_EDIT_LOCK_MINUTES_BEFORE_START.getKey());
            boolean locked = LocalDateTime.now().isAfter(data.getStartTime().minusMinutes(lockMinutes));
            return locked ? ActionStateEnum.OWNER_EDIT_LOCKED : ActionStateEnum.OWNER_EDITABLE;
        }

        // 访客视角：根据报名状态判断
        RegistrationData userRegistration = findActiveRegistration(currentUserId);
        if (userRegistration != null) {
            if (userRegistration.isPending()) {
                return ActionStateEnum.PENDING_REVIEW;
            }
            if (userRegistration.isActiveParticipant()) {
                return realStatus == MeetupStatusEnum.ONGOING ? ActionStateEnum.ONGOING_JOINED : ActionStateEnum.JOINED;
            }
        }

        // 未报名根据满员和加入模式判断
        if (realStatus == MeetupStatusEnum.FULL) {
            return ActionStateEnum.FULL;
        }
        if (realStatus == MeetupStatusEnum.ONGOING) {
            return ActionStateEnum.ONGOING;
        }
        return data.getJoinMode() == JoinModeEnum.DIRECT
                ? ActionStateEnum.JOIN_DIRECT : ActionStateEnum.APPLY_APPROVAL;
    }



    /**
     * 按视角获取报名参与者 userId 列表
     * <ul>
     *   <li>创建人视角：已批准 + 待审批</li>
     *   <li>非创建人视角：仅已批准</li>
     * </ul>
     * @param userId 当前用户 ID，内部判断是否为创建人
     * @return 报名参与者 userId 列表
     */
    public List<String> getParticipantUserIds(String userId) {
        List<String> userIds = new ArrayList<>();
        for (RegistrationData r : registrations) {
            if (r.isActiveParticipant()) {
                userIds.add(r.getUserId());
            } else if (isCreator(userId) && r.isPending()) {
                userIds.add(r.getUserId());
            }
        }
        return userIds;
    }

    /**
     * 获取除指定用户外的全部有效参与者 userId（用于群发通知，excludeUserId 传 null 表示全部）
     */
    public List<String> getActiveParticipantIds(String excludeUserId) {
        return registrations.stream().filter(RegistrationData::isActiveParticipant).map(RegistrationData::getUserId).filter(uid -> !uid.equals(excludeUserId)).toList();
    }

    public List<String> getReviewWaitlistIds(String userId) {
        List<String> res = new ArrayList<>();
        for (RegistrationData r : registrations) {
            if (r.getUserId().equals(userId)) {
                continue;

            }
            if (r.isActiveParticipant()) {
                res.add(r.getUserId());
            }
        }
        return res;
    }

    /** 用户是否已完成评价（REVIEWED 或 SKIPPED，用于短路判断） */
    public boolean hasReview(String userId) {
        return registrations.stream()
                .filter(r -> userId.equals(r.getUserId()))
                .anyMatch(r -> r.getStatus() == RegistrationStatusEnum.REVIEWED || r.getStatus() == RegistrationStatusEnum.SKIPPED);
    }

    public void assertCanReview() {
        Assert.isTrue(canReview(), BizErrorCode.MEETUP_CANT_REVIEW);
    }

    public boolean canReview() {
        MeetupStatusEnum realStatus = getRealStatus();
        return realStatus == MeetupStatusEnum.FINISHED || realStatus == MeetupStatusEnum.ONGOING;
    }

    public void assertReviewAvailable(String userId) {
        assertIn(userId);
        assertCanReview();
        int deadlineDays = SystemConfig.getInt(SystemConfigKey.REVIEW_DEADLINE_DAYS.getKey());
        LocalDateTime deadlineAt = this.getData().getEndTime().plusDays(deadlineDays);
        if (LocalDateTime.now().isAfter(deadlineAt)) {
            throw new BusinessException(BizErrorCode.REVIEW_DEADLINE_PASSED);
        }
    }

    public boolean canChat(String userId) {
        ActionStateEnum actionState = this.getActionState(userId);
        return actionState == ActionStateEnum.JOINED || actionState == ActionStateEnum.ONGOING_JOINED || actionState == ActionStateEnum.OWNER_EDITABLE || actionState == ActionStateEnum.OWNER_EDIT_LOCKED || actionState == ActionStateEnum.FINISHED_JOINED || actionState == ActionStateEnum.FINISHED_REVIEWED || actionState == ActionStateEnum.CLOSED_JOINED;
    }
}
