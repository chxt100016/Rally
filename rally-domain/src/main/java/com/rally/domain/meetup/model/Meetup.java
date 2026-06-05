package com.rally.domain.meetup.model;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.meetup.enums.*;
import lombok.Getter;

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
                .filter(r -> r.getStatus() == RegistrationStatusEnum.approved)
                .count();
    }

    /** 是否为创建人 */
    public boolean isCreator(String userId) {
        return userId.equals(data.getCreatorId());
    }

    /** 是否可编辑 */
    public boolean canEdit(String userId, int lockMinutes) {
        MeetupStatusEnum realStatus = getRealStatus();
        return isCreator(userId)
                && realStatus != MeetupStatusEnum.FINISHED
                && realStatus != MeetupStatusEnum.CLOSED
                && LocalDateTime.now().isBefore(data.getStartTime().minusMinutes(lockMinutes));
    }

    /** 是否可关闭 */
    public boolean canClose(String userId) {
        MeetupStatusEnum realStatus = getRealStatus();
        return isCreator(userId)
                && realStatus != MeetupStatusEnum.FINISHED
                && realStatus != MeetupStatusEnum.CLOSED;
    }

    // ======================== 报名记录查询 ========================

    /** 查找用户的有效报名记录（pending/approved） */
    public RegistrationData findActiveRegistration(String userId) {
        return registrations.stream()
                .filter(r -> userId.equals(r.getUserId()))
                .filter(r -> r.getStatus() == RegistrationStatusEnum.pending
                        || r.getStatus() == RegistrationStatusEnum.approved)
                .findFirst().orElse(null);
    }

    /** 查找用户任意状态的报名记录 */
    public RegistrationData findAnyRegistration(String userId) {
        return registrations.stream()
                .filter(r -> userId.equals(r.getUserId()))
                .findFirst().orElse(null);
    }

    /** 获取待审批的报名列表 */
    public List<RegistrationData> getPendingRegistrations() {
        return registrations.stream()
                .filter(r -> r.getStatus() == RegistrationStatusEnum.pending)
                .toList();
    }

    // ======================== 报名领域行为 ========================

    /**
     * 断言可以报名（校验约球状态、时间、创建人）
     */
    public void assertCanJoin(String userId) {
        MeetupStatusEnum realStatus = getRealStatus();

        // 1. 状态校验
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
    }

    /**
     * 性别限制校验
     * @param userGender 用户性别（大写字符串），可为 null
     */
    public void checkGenderLimit(String userGender) {
        if (data.getGenderLimit() == GenderLimitEnum.ANY || userGender == null) {
            return;
        }
        if (data.getGenderLimit() == GenderLimitEnum.MALE && !"MALE".equals(userGender)) {
            throw new BusinessException(BizErrorCode.GENDER_NOT_MATCH);
        }
        if (data.getGenderLimit() == GenderLimitEnum.FEMALE && !"FEMALE".equals(userGender)) {
            throw new BusinessException(BizErrorCode.GENDER_NOT_MATCH);
        }
    }

    // ======================== 报名状态判断（静态工具方法） ========================

    /** 报名记录是否可撤回（仅 pending） */
    public static boolean canWithdraw(RegistrationData registration) {
        return registration != null && registration.getStatus() == RegistrationStatusEnum.pending;
    }

    /** 报名记录是否可退出（仅 approved） */
    public static boolean canQuit(RegistrationData registration) {
        return registration != null && registration.getStatus() == RegistrationStatusEnum.approved;
    }

    /** 报名记录是否可审批（仅 pending） */
    public static boolean canReview(RegistrationData registration) {
        return registration != null && registration.getStatus() == RegistrationStatusEnum.pending;
    }

    /** 报名记录是否可复活（rejected/withdrawn/expired 可重新报名） */
    public static boolean canRevive(RegistrationData registration) {
        if (registration == null) {
            return false;
        }
        RegistrationStatusEnum status = registration.getStatus();
        return status == RegistrationStatusEnum.rejected
                || status == RegistrationStatusEnum.withdrawn
                || status == RegistrationStatusEnum.expired;
    }
}
