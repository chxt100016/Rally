package com.rally.db.meetup.gateway;

import com.rally.db.meetup.convert.MeetupConvertMapper;
import com.rally.db.meetup.entity.MeetupPO;
import com.rally.db.meetup.repository.MeetupRepository;
import com.rally.db.meetup.repository.WaitlistRepository;
import com.rally.domain.meetup.gateway.MeetupGateway;
import com.rally.domain.meetup.model.MeetupData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 约球主表网关实现
 */
@Component
@RequiredArgsConstructor
public class MeetupGatewayImpl implements MeetupGateway {

    private final MeetupRepository meetupRepository;
    private final WaitlistRepository waitlistRepository;


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
        // 发布者本身是参与者
        if (userId.equals(meetup.getCreatorId())) {
            return true;
        }
        // 检查是否在已批准的报名列表中
        return waitlistRepository.findActiveByMeetupAndUser(meetupId, userId) != null;
    }

    @Override
    public List<String> listParticipantUserIds(String meetupId) {
        MeetupPO meetup = meetupRepository.findByBizId(meetupId);
        if (meetup == null) {
            return List.of();
        }
        List<String> participants = new ArrayList<>();
        // 发布者加入列表
        participants.add(meetup.getCreatorId());
        // 已批准的报名者加入列表
        List<String> approved = waitlistRepository.listApprovedUserIds(meetupId);
        for (String uid : approved) {
            if (!participants.contains(uid)) {
                participants.add(uid);
            }
        }
        return participants;
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
