package com.rally.meetup.activity;

import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.meetup.model.MeetupPublishCmd;
import com.rally.domain.meetup.model.RegistrationData;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.meetup.service.MeetupPolicy;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.user.service.UserProfileDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 create-open-meetup：校验发布资格与内容，建立开放约球和发布者报名。
 */
@Component
@RequiredArgsConstructor
public class CreateOpenMeetupActivity {

    private final UserProfileDomainService userProfileDomainService;

    private final MeetupPolicy meetupPolicy;

    private final MeetupDomainService meetupDomainService;

    public OpenMeetupContext execute(String publisherId, MeetupPublishCmd cmd) {
        // A1：沿用既有用户档案错误语义，并按既有存储状态口径校验当天发布次数。
        UserProfile userProfile = userProfileDomainService.get(publisherId);
        userProfile.assertCompleted();

        // A2：城市、开始时间、六档 duration 与水平模式组合按既有顺序校验。
        meetupPolicy.assertPublish(publisherId, cmd);

        // A3-A5：解析可选球场，生成标题、时间与业务编号，并整体保存创建者 JOINED 报名。
        Meetup meetup = meetupDomainService.save(publisherId, cmd);
        RegistrationData publisherRegistration = meetup.findActiveRegistration(publisherId);
        return new OpenMeetupContext(
                meetup.getMeetupId(),
                publisherId,
                publisherRegistration.getBizId(),
                meetup.getData());
    }
}
