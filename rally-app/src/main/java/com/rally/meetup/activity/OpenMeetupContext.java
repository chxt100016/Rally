package com.rally.meetup.activity;

import com.rally.domain.meetup.model.MeetupData;

/**
 * 新建开放约球后交给发布流程后续步骤的内部上下文。
 *
 * <p>该上下文不会作为发布接口响应返回；发布接口仍保持无业务数据响应。</p>
 */
public record OpenMeetupContext(
        String meetupId,
        String publisherId,
        String publisherRegistrationId,
        MeetupData meetupData) {
}
