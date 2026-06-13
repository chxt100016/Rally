package com.rally.db.meetup.gateway;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rally.db.meetup.convert.MeetupConvertMapper;
import com.rally.db.meetup.entity.MeetupPO;
import com.rally.db.meetup.entity.RegistrationPO;
import com.rally.db.meetup.repository.MeetupRepository;
import com.rally.db.meetup.service.RegistrationService;
import com.rally.domain.meetup.gateway.MeetupGateway;
import com.rally.domain.meetup.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 约球主表网关实现
 */
@Component
@RequiredArgsConstructor
public class MeetupGatewayImpl implements MeetupGateway {

    private final MeetupRepository meetupRepository;
    private final RegistrationService registrationService;


    @Override
    public void save(MeetupData data) {
        meetupRepository.saveOrUpdateByBizId(MeetupConvertMapper.INSTANCE.toMeetupPO(data));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(Meetup meetup) {
        // 聚合根计算 currentPlayers 后整体落库：主表 + 报名表均按 bizId upsert
        MeetupData data = meetup.getData();
        data.setCurrentPlayers(meetup.countApprovedPlayers());
        save(data);

        meetup.getRegistrations().forEach(reg -> saveOrUpdateByBizId(MeetupConvertMapper.INSTANCE.toRegistrationPO(reg)));
    }

    /**
     * 按 bizId upsert：存在则更新，不存在则新增
     */
    private void saveOrUpdateByBizId(RegistrationPO po) {
        boolean updated = po.getBizId() != null && registrationService.lambdaUpdate().eq(RegistrationPO::getBizId, po.getBizId()).update(po);
        if (!updated) {
            registrationService.save(po);
        }
    }

    @Override
    public MeetupData findByBizId(String bizId) {
        MeetupPO po = meetupRepository.findByBizId(bizId);
        return MeetupConvertMapper.INSTANCE.toMeetupData(po);
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
    public long countFinishedMatches(String userId, int days) {
        return meetupRepository.countFinishedMatches(userId, days);
    }

    @Override
    public List<MeetupData> listAvailable(MeetupListQueryParam param) {
        List<MeetupPO> poList = meetupRepository.listNew(param);
        return MeetupConvertMapper.INSTANCE.toMeetupDataList(poList);
    }

    @Override
    public List<MeetupData> listByMeetupIdsWithFilter(MeetupListQueryParam param) {
        List<MeetupPO> poList = meetupRepository.listByMeetupIdsWithFilter(param);
        return MeetupConvertMapper.INSTANCE.toMeetupDataList(poList);
    }

    @Override
    public long countByCreatorId(String userId) {
        return meetupRepository.countByCreatorId(userId);
    }

    @Override
    public long countFinishedByCreatorId(String userId) {
        return meetupRepository.countFinishedByCreatorId(userId);
    }

    @Override
    public PageDTO<MeetupData> listByUserFilter(MeetupListQueryParam param) {
        IPage<MeetupPO> page = meetupRepository.listByUserFilter(param);
        List<MeetupData> dataList = MeetupConvertMapper.INSTANCE.toMeetupDataList(page.getRecords());
        boolean hasMore = page.getCurrent() < page.getPages();
        return new PageDTO<>(dataList, page.getTotal(), hasMore);
    }

    @Override
    public PageDTO<MeetupData> listPendingMeetups(String userId, int deadlineDays, int pageNo, int pageSize) {
        IPage<MeetupPO> page = meetupRepository.listPendingMeetups(userId, deadlineDays, pageNo, pageSize);
        List<MeetupData> dataList = MeetupConvertMapper.INSTANCE.toMeetupDataList(page.getRecords());
        boolean hasMore = page.getCurrent() < page.getPages();
        return new PageDTO<>(dataList, page.getTotal(), hasMore);
    }

    @Override
    public PageDTO<MeetupData> listRecentByUser(String userId, int pageSize) {
        IPage<MeetupPO> page = meetupRepository.listRecentByUser(userId, pageSize);
        List<MeetupData> dataList = MeetupConvertMapper.INSTANCE.toMeetupDataList(page.getRecords());
        boolean hasMore = page.getCurrent() < page.getPages();
        return new PageDTO<>(dataList, page.getTotal(), hasMore);
    }
}
