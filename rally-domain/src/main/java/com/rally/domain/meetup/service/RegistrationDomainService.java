package com.rally.domain.meetup.service;

import com.rally.domain.meetup.gateway.MeetupGateway;
import com.rally.domain.meetup.gateway.RegistrationGateway;
import com.rally.domain.meetup.model.JoinResult;
import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.user.model.UserProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 报名领域服务（负责报名的持久化操作）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationDomainService {

    private final MeetupGateway meetupGateway;

    /**
     * 保存报名（调用聚合根 join + 持久化）
     * @param meetup 约球聚合根
     * @param userProfile 用户档案领域对象
     * @param autoWithdrawAt 自动撤回时间，可为 null
     * @return 报名结果
     */
    public JoinResult save(Meetup meetup, UserProfile userProfile, LocalDateTime autoWithdrawAt) {
        // 1. 调用聚合根 join 方法（校验 + 创建报名记录）
        JoinResult result = meetup.join(userProfile, autoWithdrawAt);

        // 2. 保存报名记录
        meetupGateway.save(meetup);
        
        return result;
    }
}
