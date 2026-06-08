package com.rally.domain.meetup.service;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.meetup.convert.MeetupDomainConvertMapper;
import com.rally.domain.meetup.gateway.MeetupGateway;
import com.rally.domain.meetup.model.*;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 约球查询领域服务
 * 负责列表查询（按时间/距离）、详情查询等读操作的核心逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeetupQueryDomainService {

    private final MeetupGateway meetupGateway;
    private final MeetupQueryPlanner queryPlanner;

    /**
     * 按时间排序的列表查询
     * @param query 查询条件
     * @return 约球卡片分页结果
     */
    public PageDTO<MeetupCardDTO> listByTime(MeetupListCmd query) {
        MeetupListQueryParam param = queryPlanner.plan(query);
        if (param == null) {
            return MeetupQueryPlanner.emptyPage();
        }
        return doList(param);
    }

    /**
     * 按距离排序的列表查询（GEO 特化）
     * @param query 查询条件（必须包含 lng/lat）
     * @return 约球卡片分页结果
     */
    public PageDTO<MeetupCardDTO> listByDistance(MeetupListCmd query) {
        Assert.notNull(query.getLng(), BizErrorCode.PARAM_ERROR);
        Assert.notNull(query.getLat(), BizErrorCode.PARAM_ERROR);

        MeetupListQueryParam param = queryPlanner.plan(query);
        if (param == null) {
            return MeetupQueryPlanner.emptyPage();
        }
        return doList(param);
    }

    /**
     * 执行查询并转换结果
     */
    private PageDTO<MeetupCardDTO> doList(MeetupListQueryParam param) {
        PageDTO<MeetupData> pageResult = meetupGateway.listAvailable(param);

        List<MeetupCardDTO> cardList = pageResult.getList().stream()
                .map(MeetupDomainConvertMapper.INSTANCE::toMeetupCardDTO)
                .collect(Collectors.toList());

        return new PageDTO<>(cardList, pageResult.getTotal(), pageResult.getHasMore());
    }

    /**
     * 查询约球详情（核心数据）
     * @param meetupId 约球ID
     * @return 约球数据
     */
    public MeetupData getDetail(String meetupId) {
        MeetupData data = meetupGateway.findByBizId(meetupId);
        Assert.notNull(data, BizErrorCode.MEETUP_NOT_FOUND);
        return data;
    }
}
