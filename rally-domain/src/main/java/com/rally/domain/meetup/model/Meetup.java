package com.rally.domain.meetup.model;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.meetup.enums.*;
import com.rally.domain.system.SystemConfig;
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
     * 仅约球数据（不包含报名记录，适用于编辑/关闭等场景）
     */
    public Meetup(MeetupData data) {
        this(data, new ArrayList<>());
    }

    /**
     * 完整聚合根（约球数据 + 报名记录）
     */
    public Meetup(MeetupData data, List<RegistrationData> registrations) {
        this.data = data;
        this.registrations = registrations != null ? registrations : new ArrayList<>();
    }

    // ======================== 约球状态判断 ========================

    /**
     * 懒判定：计算真实状态（endTime 已过则视为 FINISHED）
     */
    public MeetupStatusEnum getRealStatus() {
        if ((data.getStatus() == MeetupStatusEnum.OPEN || data.getStatus() == MeetupStatusEnum.FULL)
                && data.getEndTime().isBefore(LocalDateTime.now())) {
            return MeetupStatusEnum.FINISHED;
        }
        return data.getStatus();
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
                .filter(r -> r.getStatus() == RegistrationStatusEnum.APPROVED)
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

    /** 断言当前用户为创建人，否则抛出异常 */
    public void assertOwner(String userId) {
        if (!isCreator(userId)) {
            throw new BusinessException(BizErrorCode.NOT_CREATOR);
        }
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
            if (!cmd.getCourtLng().equals(data.getCourtLng()) || !cmd.getCourtLat().equals(data.getCourtLat())) {
                return true;
            }
        }
        return false;
    }

    // ======================== 报名记录查询 ========================

    /** 查找用户的有效报名记录（pending/APPROVED） */
    public RegistrationData findActiveRegistration(String userId) {
        return registrations.stream()
                .filter(r -> userId.equals(r.getUserId()))
                .filter(r -> r.getStatus() == RegistrationStatusEnum.PENDING
                        || r.getStatus() == RegistrationStatusEnum.APPROVED)
                .findFirst().orElse(null);
    }



    // ======================== 报名领域行为 ========================

    /**
     * 报名（包含校验 + 创建报名记录）
     * @param userProfile 用户档案领域对象
     * @param autoWithdrawAt 自动撤回时间，可为 null
     * @return 新增的报名记录
     */
    public RegistrationData join(UserProfile userProfile, LocalDateTime autoWithdrawAt) {
        // 1. 校验
        assertCanJoin(userProfile);

        // 2. 创建报名记录
        RegistrationData registration = new RegistrationData();
        registration.setRallyMeetupId(data.getBizId());
        registration.setUserId(userProfile.getUser().getUserId());
        registration.setExpiresAt(autoWithdrawAt);

        // 3. 根据加入模式设置状态
        registration.setStatus(data.getJoinMode() == JoinModeEnum.DIRECT ? RegistrationStatusEnum.APPROVED : RegistrationStatusEnum.PENDING);

        this.registrations.add(registration);

        return registration;
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
        BigDecimal reputationScore = userProfile.getProfile().getReputationScore();
        BigDecimal threshold = new BigDecimal(SystemConfig.getString("meetup.join.min_reputation_score", "30"));
        if (reputationScore.compareTo(threshold) < 0) {
            throw new BusinessException(BizErrorCode.LOW_REPUTATION_BANNED);
        }
    }

    // ======================== 报名状态判断（静态工具方法） ========================

    /** 报名记录是否可撤回（仅 pending） */
    public static boolean canWithdraw(RegistrationData registration) {
        return registration != null && registration.getStatus() == RegistrationStatusEnum.PENDING;
    }

    /** 报名记录是否可退出（仅 APPROVED） */
    public static boolean canQuit(RegistrationData registration) {
        return registration != null && registration.getStatus() == RegistrationStatusEnum.APPROVED;
    }

    /** 报名记录是否可审批（仅 pending） */
    public static boolean canReview(RegistrationData registration) {
        return registration != null && registration.getStatus() == RegistrationStatusEnum.PENDING;
    }

    /** 断言报名记录可审批（仅 pending） */
    public static void assertCanReview(RegistrationData registration) {
        Assert.isTrue(canReview(registration), BizErrorCode.WAITLIST_NOT_PENDING);
    }

    // ======================== 报名操作（需聚合根上下文） ========================

    /**
     * 退出报名（已加入 → WITHDRAWN）
     * @param userId 当前用户 ID
     * @return 退出结果（是否在 6h 内需扣分）
     */
    public QuitResult quit(String userId) {
        // 1. 查找报名记录并校验
        RegistrationData registration = findActiveRegistration(userId);
        Assert.notNull(registration, BizErrorCode.NOT_JOINED);
        Assert.isTrue(canQuit(registration), BizErrorCode.NOT_JOINED);

        // 2. 更新状态
        registration.setStatus(RegistrationStatusEnum.WITHDRAWN);

        // 3. 判断是否在 6h 内
        long hoursUntilStart = Duration.between(LocalDateTime.now(), data.getStartTime()).toHours();
        int thresholdHours = SystemConfig.getInt("meetup.quit.penalty_threshold_hours", 6);
        return hoursUntilStart < thresholdHours ? QuitResult.PENALIZED : QuitResult.NORMAL;
    }

    /**
     * 审批通过
     * @param registrationId 报名记录 ID
     * @param currentUserId 当前用户 ID（审批人）
     */
    public void approve(String registrationId, String currentUserId) {
        // 1. 查找报名记录
        RegistrationData registration = findRegistrationById(registrationId);
        Assert.notNull(registration, BizErrorCode.WAITLIST_NOT_FOUND);

        // 2. 权限校验
        assertOwner(currentUserId);

        // 3. 状态校验
        assertCanReview(registration);
        Assert.isTrue(isActive(), BizErrorCode.MEETUP_STATUS_ILLEGAL);

        // 4. 更新状态
        registration.setStatus(RegistrationStatusEnum.APPROVED);
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
        assertCanReview(registration);

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

        // 终态：全部置灰（不区分创建人/访客）
        if (realStatus == MeetupStatusEnum.FINISHED) {
            return ActionStateEnum.FINISHED;
        }
        if (realStatus == MeetupStatusEnum.CLOSED) {
            return ActionStateEnum.CLOSED;
        }

        // 创建人视角
        if (isCreator) {
            int lockMinutes = SystemConfig.getInt("meetup.edit_lock_minutes_before_start", 60);
            boolean locked = LocalDateTime.now().isAfter(data.getStartTime().minusMinutes(lockMinutes));
            return locked ? ActionStateEnum.OWNER_EDIT_LOCKED : ActionStateEnum.OWNER_EDITABLE;
        }

        // 访客视角：根据报名状态判断
        RegistrationData userRegistration = findActiveRegistration(currentUserId);
        if (userRegistration != null) {
            if (userRegistration.getStatus() == RegistrationStatusEnum.PENDING) {
                return ActionStateEnum.PENDING_REVIEW;
            }
            if (userRegistration.getStatus() == RegistrationStatusEnum.APPROVED) {
                return ActionStateEnum.JOINED;
            }
        }

        // 未报名：根据满员和加入模式判断
        if (realStatus == MeetupStatusEnum.FULL) {
            return ActionStateEnum.FULL;
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
            if (r.getStatus() == RegistrationStatusEnum.APPROVED) {
                userIds.add(r.getUserId());
            } else if (isCreator(userId) && r.getStatus() == RegistrationStatusEnum.PENDING) {
                userIds.add(r.getUserId());
            }
        }
        return userIds;
    }

}
