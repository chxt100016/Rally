package com.rally.meetup.activity;

import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.model.MeetupEditCmd;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.meetup.service.MeetupPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 revise-meetup：按既有顺序校验并保存约球编辑资料。
 */
@Component
@RequiredArgsConstructor
public class ReviseMeetupActivity {

    private final MeetupDomainService meetupDomainService;

    private final MeetupPolicy meetupPolicy;

    public MeetupData execute(String operatorId, MeetupEditCmd cmd) {
        // A1：加载完整约球聚合；赛事关联比赛状态由编辑策略先行核实。
        Meetup meetup = meetupDomainService.get(cmd.getMeetupId());

        // A2：沿用既有顺序校验状态、锁定点、城市和参与者锁定规则。
        meetupPolicy.assertEdit(meetup, cmd);

        // A3-A5：创建者校验晚于上述规则；解析可选球场、映射并保存。
        meetupDomainService.edit(operatorId, meetup, cmd);
        return meetup.getData();
    }
}
