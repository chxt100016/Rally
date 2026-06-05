package com.rally.db.meetup.gateway;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.db.meetup.convert.MeetupConvertMapper;
import com.rally.db.meetup.entity.RegistrationPO;
import com.rally.db.meetup.repository.RegistrationRepository;
import com.rally.domain.meetup.enums.WaitlistStatusEnum;
import com.rally.domain.meetup.gateway.RegistrationGateway;
import com.rally.domain.meetup.model.RegistrationData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 报名/注册表网关实现
 */
@Component
@RequiredArgsConstructor
public class RegistrationGatewayImpl implements RegistrationGateway {

    private final RegistrationRepository registrationRepository;
    private static final MeetupConvertMapper MAPPER = MeetupConvertMapper.INSTANCE;

    @Override
    public void save(RegistrationData data) {
        RegistrationPO po = MAPPER.toRegistrationPO(data);
        if (data.getBizId() != null) {
            RegistrationPO existing = registrationRepository.findByBizId(data.getBizId());
            if (existing != null) {
                po.setId(existing.getId());
                registrationRepository.updateById(po);
                return;
            }
        } else {
            // 新增时生成 bizId
            po.setBizId(IdWorker.getIdStr());
        }
        registrationRepository.save(po);
    }

    @Override
    public RegistrationData findByBizId(String bizId) {
        RegistrationPO po = registrationRepository.findByBizId(bizId);
        return MAPPER.toRegistrationData(po);
    }

    @Override
    public RegistrationData findActiveByMeetupAndUser(String meetupId, String userId) {
        RegistrationPO po = registrationRepository.findActiveByMeetupAndUser(meetupId, userId);
        return MAPPER.toRegistrationData(po);
    }

    @Override
    public RegistrationData findByMeetupAndUserAny(String meetupId, String userId) {
        RegistrationPO po = registrationRepository.findByMeetupAndUserAny(meetupId, userId);
        return MAPPER.toRegistrationData(po);
    }

    @Override
    public List<RegistrationData> findByUserAndStatus(String userId, WaitlistStatusEnum status) {
        return MAPPER.toRegistrationDataList(
                registrationRepository.findByUserAndStatus(userId, status.name()));
    }

    @Override
    public List<RegistrationData> findPendingByMeetupId(String meetupId) {
        return MAPPER.toRegistrationDataList(
                registrationRepository.findPendingByMeetupId(meetupId));
    }

    @Override
    public List<RegistrationData> findConflict(String userId, LocalDateTime startTime,
                                                LocalDateTime endTime, String excludeMeetupId) {
        return MAPPER.toRegistrationDataList(
                registrationRepository.findConflict(userId, startTime, endTime, excludeMeetupId));
    }

    @Override
    public void updateStatus(String bizId, WaitlistStatusEnum status) {
        RegistrationPO po = registrationRepository.findByBizId(bizId);
        if (po != null) {
            po.setStatus(status.name());
            po.setOptTime(LocalDateTime.now());
            registrationRepository.updateById(po);
        }
    }

    @Override
    public void revive(String bizId, LocalDateTime expiresAt) {
        registrationRepository.revive(bizId, expiresAt);
    }

    @Override
    public List<String> listApprovedUserIds(String meetupId) {
        return registrationRepository.listApprovedUserIds(meetupId);
    }
}
