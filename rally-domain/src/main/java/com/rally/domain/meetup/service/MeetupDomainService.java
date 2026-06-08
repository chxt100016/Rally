package com.rally.domain.meetup.service;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.meetup.convert.MeetupDomainConvertMapper;
import com.rally.domain.meetup.enums.ActionStateEnum;
import com.rally.domain.meetup.enums.JoinModeEnum;
import com.rally.domain.meetup.enums.MeetupStatusEnum;
import com.rally.domain.meetup.enums.RegistrationStatusEnum;
import com.rally.domain.meetup.gateway.MeetupGateway;
import com.rally.domain.meetup.gateway.NearbyGateway;
import com.rally.domain.meetup.gateway.RegistrationGateway;
import com.rally.domain.meetup.model.*;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 约球领域服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MeetupDomainService {

    private final MeetupGateway meetupGateway;

    private final NearbyGateway nearbyGateway;

    private final RegistrationGateway registrationGateway;

    private final MeetupPolicy meetupPolicy;

    /**
     * 获取约球聚合根（含全部报名记录，适用于报名/审批等场景）
     * @param meetupId 约球ID
     * @return Meetup 聚合根（含全部报名记录）
     */
    public Meetup get(String meetupId) {
        MeetupData data = meetupGateway.findByBizId(meetupId);
        Assert.notNull(data, BizErrorCode.MEETUP_NOT_FOUND);
        List<RegistrationData> registrations = registrationGateway.findByMeetupId(meetupId);
        return new Meetup(data, registrations);
    }

    /**
     * 编辑约球（更新字段 + 保存）
     * @param cmd 编辑命令
     */
    public void edit(String userId, Meetup meetup, MeetupEditCmd cmd) {
        meetup.assertOwner(userId);

        // 1. 更新字段（MapStruct）
        MeetupDomainConvertMapper.INSTANCE.updateMeetupData(meetup.getData(), cmd);

        // 2. 保存
        meetupGateway.save(meetup.getData());
    }

    /**
     * 构建约球聚合根（含创建者报名）并一次性持久化
     */
    public void add(String userId, MeetupPublishCmd cmd) {
        // 1. 通过聚合根工厂创建（自动将创建者加入报名表）
        Meetup meetup = MeetupFactory.create(cmd, userId);

        // 2. 一次性持久化（约球主表 + 报名记录）
        meetupGateway.save(meetup);

        // 3. GEO 写入
        nearbyGateway.add(cmd.getCityCode(), meetup.getData().getBizId(), cmd.getCourtLng(), cmd.getCourtLat());
    }

    /**
     * 关闭约球（权限校验 + 状态更新）
     * @param userId 当前用户
     * @param meetup 聚合根
     */
    public void close(String userId, Meetup meetup) {
        // 1. 权限和状态校验
        meetupPolicy.assertClose(userId, meetup);

        // 2. 更新状态
        meetup.getData().setStatus(MeetupStatusEnum.CLOSED);
        meetupGateway.save(meetup.getData());
    }



    /**
     * 计算关闭约球的阶梯扣分
     */
    public int calculateCancelPenalty(LocalDateTime startTime, int penalty24h, int penalty12h, int penalty6h, int penaltyUnder6h) {
        long hoursUntilStart = Duration.between(LocalDateTime.now(), startTime).toHours();

        if (hoursUntilStart >= 24) {
            return penalty24h;
        } else if (hoursUntilStart >= 12) {
            return penalty12h;
        } else if (hoursUntilStart >= 6) {
            return penalty6h;
        } else {
            return penaltyUnder6h;
        }
    }

}
