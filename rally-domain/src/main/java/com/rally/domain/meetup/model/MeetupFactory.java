package com.rally.domain.meetup.model;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.domain.meetup.convert.MeetupDomainConvertMapper;
import com.rally.domain.meetup.enums.RegistrationStatusEnum;

import java.util.ArrayList;
import java.util.List;

/**
 * 约球聚合根工厂
 */
public class MeetupFactory {

    /**
     * 创建新约球（含创建者自动报名）
     *
     * @param cmd    发布命令（含 cityCode）
     * @param userId 创建者 ID
     * @return 完整的 Meetup 聚合根（含创建者报名记录）
     */
    public static Meetup create(PublishCmd cmd, String userId) {
        // 1. 映射 PublishCmd -> MeetupData（currentPlayers 已在 MapStruct 中设为 1）
        MeetupData data = MeetupDomainConvertMapper.INSTANCE.toMeetupData(cmd, userId);
        data.setBizId(IdWorker.getIdStr());

        // 2. 创建者自动加入报名表，状态为 approved
        RegistrationData creatorRegistration = new RegistrationData();
        creatorRegistration.setBizId(IdWorker.getIdStr());
        creatorRegistration.setRallyMeetupId(data.getBizId());
        creatorRegistration.setUserId(userId);
        creatorRegistration.setStatus(RegistrationStatusEnum.approved);

        // 3. 组装聚合根
        List<RegistrationData> registrations = new ArrayList<>();
        registrations.add(creatorRegistration);
        return new Meetup(data, registrations);
    }
}
