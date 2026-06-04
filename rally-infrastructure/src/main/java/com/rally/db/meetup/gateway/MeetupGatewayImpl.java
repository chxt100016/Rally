package com.rally.db.meetup.gateway;

import com.rally.db.meetup.convert.MeetupConvertMapper;
import com.rally.db.meetup.entity.MeetupPO;
import com.rally.db.meetup.repository.MeetupRepository;
import com.rally.domain.meetup.gateway.MeetupGateway;
import com.rally.domain.meetup.model.MeetupData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 约球主表网关实现
 */
@Component
@RequiredArgsConstructor
public class MeetupGatewayImpl implements MeetupGateway {

    private final MeetupRepository meetupRepository;


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
}
