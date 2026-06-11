package com.rally.domain.meetup.model;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.meetup.enums.RegistrationStatusEnum;
import com.rally.domain.utils.Assert;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报名/注册领域数据对象（记录所有参与者：创建者、等待审批、已通过等）
 */
@Data
public class RegistrationData {
    private String bizId;
    private String rallyMeetupId;
    private String userId;
    private RegistrationStatusEnum status;
    private LocalDateTime expiresAt;
    private LocalDateTime optTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public RegistrationData() {
        bizId = IdWorker.getIdStr();
    }

    // ======================== 报名状态判断 ========================

    /** 是否为待审批状态（PENDING） */
    public boolean isPending() {
        return status == RegistrationStatusEnum.PENDING;
    }

    /** 是否为有效参与者状态（JOINED 或 REVIEWED） */
    public boolean isActiveParticipant() {
        return status == RegistrationStatusEnum.JOINED || status == RegistrationStatusEnum.REVIEWED;
    }

    /** 是否可撤回（仅 PENDING） */
    public boolean canWithdraw() {
        return isPending();
    }

    /** 是否可退出（JOINED 或 REVIEWED） */
    public boolean canQuit() {
        return isActiveParticipant();
    }

    /** 是否可审批（仅 PENDING） */
    public boolean canReview() {
        return isPending();
    }

    /** 断言可审批，否则抛出业务异常 */
    public void assertCanReview() {
        Assert.isTrue(canReview(), BizErrorCode.WAITLIST_NOT_PENDING);
    }
}
