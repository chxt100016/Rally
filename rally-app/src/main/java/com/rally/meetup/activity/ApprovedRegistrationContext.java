package com.rally.meetup.activity;

import com.rally.domain.meetup.model.MeetupData;

import java.util.List;

/**
 * 审批通过后交给群聊加入与通知步骤的内部上下文。
 *
 * <p>报名与人数信息只用于后续流程编排，不会作为审批接口响应返回。</p>
 */
public record ApprovedRegistrationContext(
        String registrationId,
        String meetupId,
        String approvedUserId,
        List<String> participantUserIds,
        Integer maxPlayers,
        boolean fullAfterApproval,
        MeetupData meetupData) {

    public ApprovedRegistrationContext {
        participantUserIds = List.copyOf(participantUserIds);
    }
}
