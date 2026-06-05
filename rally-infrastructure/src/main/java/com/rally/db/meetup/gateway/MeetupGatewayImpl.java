package com.rally.db.meetup.gateway;

import com.rally.db.meetup.convert.MeetupConvertMapper;
import com.rally.db.meetup.entity.MeetupPO;
import com.rally.db.meetup.entity.RegistrationPO;
import com.rally.db.meetup.repository.MeetupRepository;
import com.rally.db.meetup.repository.RegistrationRepository;
import com.rally.domain.meetup.gateway.MeetupGateway;
import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.model.RegistrationData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 约球主表网关实现
 */
@Component
@RequiredArgsConstructor
public class MeetupGatewayImpl implements MeetupGateway {

    private final MeetupRepository meetupRepository;
    private final RegistrationRepository registrationRepository;


    @Override
    public void save(MeetupData data) {
        MeetupPO po = MeetupConvertMapper.INSTANCE.toMeetupPO(data);
        if (data.getBizId() != null) {
            MeetupPO existing = meetupRepository.findByBizId(data.getBizId());
            if (existing != null) {
                po.setId(existing.getId());
                meetupRepository.updateById(po);
                return;
            }
        }
        meetupRepository.save(po);
    }

    @Override
    public void save(Meetup meetup) {
        MeetupConvertMapper mapper = MeetupConvertMapper.INSTANCE;

        // 1. 通过聚合根计算 currentPlayers 并回写
        MeetupData data = meetup.getData();
        data.setCurrentPlayers(meetup.countApprovedPlayers());

        // 2. 保存约球主表
        MeetupPO meetupPO = mapper.toMeetupPO(data);
        if (data.getBizId() != null) {
            MeetupPO existing = meetupRepository.findByBizId(data.getBizId());
            if (existing != null) {
                meetupPO.setId(existing.getId());
                meetupRepository.updateById(meetupPO);
            } else {
                meetupRepository.save(meetupPO);
            }
        } else {
            meetupRepository.save(meetupPO);
        }

        // 3. 保存报名记录（bizId 已在聚合根工厂中生成）
        for (RegistrationData regData : meetup.getRegistrations()) {
            RegistrationPO regPO = mapper.toRegistrationPO(regData);
            registrationRepository.save(regPO);
        }
    }

    @Override
    public MeetupData findByBizId(String bizId) {
        MeetupPO po = meetupRepository.findByBizId(bizId);
        return MeetupConvertMapper.INSTANCE.toMeetupData(po);
    }

    @Override
    public List<MeetupData> findByBizIds(List<String> bizIds) {
        return MeetupConvertMapper.INSTANCE.toMeetupDataList(meetupRepository.findByBizIds(bizIds));
    }

    @Override
    public List<MeetupData> findByCityCodeAndStatus(String cityCode, List<String> statusList) {
        return MeetupConvertMapper.INSTANCE.toMeetupDataList(meetupRepository.findByCityCodeAndStatus(cityCode, statusList));
    }

    @Override
    public void updateStatus(String bizId, String status) {
        MeetupPO po = meetupRepository.findByBizId(bizId);
        if (po != null) {
            po.setStatus(status);
            meetupRepository.updateById(po);
        }
    }

    @Override
    public int incrementPlayers(String bizId) {
        return meetupRepository.incrementPlayers(bizId);
    }

    @Override
    public int decrementPlayers(String bizId) {
        return meetupRepository.decrementPlayers(bizId);
    }

    @Override
    public long countTodayActive(String userId) {
        return meetupRepository.countTodayActive(userId);
    }

    @Override
    public List<String> listActiveIds(String cityCode) {
        return meetupRepository.listActiveIds(cityCode);
    }

    @Override
    public int batchUpdateToFinished() {
        return meetupRepository.batchUpdateToFinished();
    }

    @Override
    public boolean isParticipant(String meetupId, String userId) {
        MeetupPO meetup = meetupRepository.findByBizId(meetupId);
        if (meetup == null) {
            return false;
        }
        // 检查是否在报名表中（含创建者）
        return registrationRepository.findActiveByMeetupAndUser(meetupId, userId) != null;
    }

    @Override
    public List<String> listParticipantUserIds(String meetupId) {
        // 所有参与者（含创建者）都在 registration 表中
        return registrationRepository.listApprovedUserIds(meetupId);
    }

    @Override
    public boolean isFinished(String meetupId) {
        MeetupPO meetup = meetupRepository.findByBizId(meetupId);
        if (meetup == null) {
            return false;
        }
        // 懒判定：end_time < NOW() 即视为 finished
        return meetup.getEndTime() != null && meetup.getEndTime().isBefore(LocalDateTime.now());
    }

    @Override
    public long countFinishedMatches(String userId, int days) {
        return meetupRepository.countFinishedMatches(userId, days);
    }
}
