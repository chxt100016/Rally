package com.rally.db.meetup.gateway;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.db.meetup.convert.MeetupConvertMapper;
import com.rally.db.meetup.entity.RegistrationPO;
import com.rally.db.meetup.service.RegistrationService;
import com.rally.domain.meetup.enums.RegistrationStatusEnum;
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

    private final RegistrationService registrationService;
    private static final MeetupConvertMapper MAPPER = MeetupConvertMapper.INSTANCE;





    @Override
    public RegistrationData findActiveByMeetupAndUser(String meetupId, String userId) {
        RegistrationPO po = get(meetupId, userId);
        return MAPPER.toRegistrationData(po);
    }



    @Override
    public void updateStatus(String bizId, RegistrationStatusEnum status) {
        RegistrationPO po = get(bizId);
        if (po != null) {
            po.setStatus(status.name());
            po.setOptTime(LocalDateTime.now());
            registrationService.updateById(po);
        }
    }


    @Override
    public int countApprovedByMeetupId(String meetupId) {
        Long count = registrationService.lambdaQuery()
                .eq(RegistrationPO::getRallyMeetupId, meetupId)
                .eq(RegistrationPO::getStatus, "JOINED")
                .count();
        return count.intValue();
    }

    @Override
    public List<RegistrationData> findByMeetupId(String meetupId) {
        return MAPPER.toRegistrationDataList(list(meetupId));
    }

    @Override
    public void toReviewed(String userId) {
        this.registrationService.lambdaUpdate()
                .eq(RegistrationPO::getUserId, userId)
                .eq(RegistrationPO::getStatus, RegistrationStatusEnum.JOINED)
                .set(RegistrationPO::getStatus, RegistrationStatusEnum.REVIEWED)
                .set(RegistrationPO::getOptTime, LocalDateTime.now());
    }


    private RegistrationPO get(String bizId) {
        return registrationService.lambdaQuery()
                .eq(RegistrationPO::getBizId, bizId)
                .one();
    }
    private RegistrationPO get(String meetupId, String userId) {
        return registrationService.lambdaQuery()
                .eq(RegistrationPO::getRallyMeetupId, meetupId)
                .eq(RegistrationPO::getUserId, userId)
                .in(RegistrationPO::getStatus, "pending", "JOINED")
                .one();
    }
    private List<RegistrationPO> list(String meetupId) {
        return registrationService.lambdaQuery()
                .eq(RegistrationPO::getRallyMeetupId, meetupId)
                .list();
    }







}
