package com.rally.meetup.activity;

import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.meetup.service.RegistrationDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 reject-pending-registration：拒绝指定待审报名并保留报名历史。
 */
@Component
@RequiredArgsConstructor
public class RejectPendingRegistrationActivity {

    private final MeetupDomainService meetupDomainService;

    private final RegistrationDomainService registrationDomainService;

    public void execute(String meetupId, String registrationId, String rejectorId) {
        // A1-A2：加载完整聚合；报名归属、创建者身份与 PENDING 状态由聚合命令校验。
        Meetup meetup = meetupDomainService.get(meetupId);

        // A3：只迁移为 REJECTED 并整体保存；不附加状态、模式、容量、过期或 optTime 规则。
        registrationDomainService.reject(meetup, registrationId, rejectorId);
    }
}
