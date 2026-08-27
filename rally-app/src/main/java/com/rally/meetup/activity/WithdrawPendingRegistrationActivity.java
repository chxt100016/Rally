package com.rally.meetup.activity;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.meetup.enums.RegistrationStatusEnum;
import com.rally.domain.meetup.gateway.RegistrationRepository;
import com.rally.domain.meetup.model.RegistrationData;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 withdraw-pending-registration：撤回本人唯一的待审报名。
 */
@Component
@RequiredArgsConstructor
public class WithdrawPendingRegistrationActivity {

    private final RegistrationRepository registrationRepository;

    public void execute(String meetupId, String userId) {
        // A1：仅查询本人 PENDING/JOINED 报名，不读取或校验约球主记录。
        RegistrationData registration = registrationRepository.findActiveByMeetupAndUser(meetupId, userId);
        Assert.notNull(registration, BizErrorCode.NOT_JOINED);

        // A2：保留现有状态边界，只有 PENDING 可撤回。
        Assert.isTrue(registration.canWithdraw(), BizErrorCode.WAITLIST_NOT_PENDING);

        // A3：按业务报名编号二次读取后更新；仓储同时写入 optTime。
        registrationRepository.updateStatus(registration.getBizId(), RegistrationStatusEnum.WITHDRAWN);
    }
}
