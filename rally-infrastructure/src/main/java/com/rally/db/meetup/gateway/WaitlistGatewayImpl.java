package com.rally.db.meetup.gateway;

import com.rally.db.meetup.convert.MeetupConvertMapper;
import com.rally.db.meetup.entity.WaitlistPO;
import com.rally.db.meetup.repository.WaitlistRepository;
import com.rally.domain.meetup.enums.WaitlistStatusEnum;
import com.rally.domain.meetup.gateway.WaitlistGateway;
import com.rally.domain.meetup.model.WaitlistData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 报名等待表网关实现
 */
@Component
@RequiredArgsConstructor
public class WaitlistGatewayImpl implements WaitlistGateway {

    private final WaitlistRepository waitlistRepository;
    private static final MeetupConvertMapper MAPPER = MeetupConvertMapper.INSTANCE;

    @Override
    public void save(WaitlistData data) {
        WaitlistPO po = MAPPER.toWaitlistPO(data);
        if (data.getBizId() != null) {
            WaitlistPO existing = waitlistRepository.findByBizId(data.getBizId());
            if (existing != null) {
                po.setId(existing.getId());
                waitlistRepository.updateById(po);
                return;
            }
        }
        waitlistRepository.save(po);
    }

    @Override
    public WaitlistData findByBizId(String bizId) {
        WaitlistPO po = waitlistRepository.findByBizId(bizId);
        return MAPPER.toWaitlistData(po);
    }

    @Override
    public WaitlistData findActiveByMeetupAndUser(String meetupId, String userId) {
        WaitlistPO po = waitlistRepository.findActiveByMeetupAndUser(meetupId, userId);
        return MAPPER.toWaitlistData(po);
    }

    @Override
    public WaitlistData findByMeetupAndUserAny(String meetupId, String userId) {
        WaitlistPO po = waitlistRepository.findByMeetupAndUserAny(meetupId, userId);
        return MAPPER.toWaitlistData(po);
    }

    @Override
    public List<WaitlistData> findByUserAndStatus(String userId, WaitlistStatusEnum status) {
        return MAPPER.toWaitlistDataList(
                waitlistRepository.findByUserAndStatus(userId, status.name()));
    }

    @Override
    public List<WaitlistData> findPendingByMeetupId(String meetupId) {
        return MAPPER.toWaitlistDataList(
                waitlistRepository.findPendingByMeetupId(meetupId));
    }

    @Override
    public List<WaitlistData> findConflict(String userId, LocalDateTime startTime,
                                            LocalDateTime endTime, String excludeMeetupId) {
        return MAPPER.toWaitlistDataList(
                waitlistRepository.findConflict(userId, startTime, endTime, excludeMeetupId));
    }

    @Override
    public void updateStatus(String bizId, WaitlistStatusEnum status) {
        WaitlistPO po = waitlistRepository.findByBizId(bizId);
        if (po != null) {
            po.setStatus(status.name());
            po.setOptTime(LocalDateTime.now());
            waitlistRepository.updateById(po);
        }
    }

    @Override
    public void revive(String bizId, LocalDateTime expiresAt) {
        waitlistRepository.revive(bizId, expiresAt);
    }
}
