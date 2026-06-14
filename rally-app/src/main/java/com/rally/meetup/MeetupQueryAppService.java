package com.rally.meetup;

import com.rally.domain.meetup.model.*;
import com.rally.domain.meetup.service.MeetupQueryDomainService;
import com.rally.domain.utils.GeoUtils;
import com.rally.meetup.convert.MeetupAppConvertMapper;
import com.rally.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 约球查询应用服务
 * 编排领域服务完成查询场景
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MeetupQueryAppService {

    private final MeetupQueryDomainService meetupQueryDomainService;

    /**
     * 约球列表查询（按时间/距离）
     */
    public PageDTO<MeetupCardDTO> queryMeetupList(MeetupListCmd query) {
        List<MeetupData> dataList = switch (query.getSort()) {
            case DISTANCE -> meetupQueryDomainService.listByDistance(query);
            case TIME -> meetupQueryDomainService.listByTime(query);
            default -> List.of();
        };
        // searchAfter：多查了 1 条用于判断是否还有下一页，命中则去掉多余的一条
        boolean hasMore = dataList.size() > query.getPageSize();
        List<MeetupData> pageData = hasMore ? dataList.subList(0, query.getPageSize()) : dataList;


        List<MeetupCardDTO> res = pageData.stream().map(item -> packCard(item, query.getLng(), query.getLat())).toList();
        return new PageDTO<>(res, null, hasMore);
    }




    /**
     * 用户约球列表查询（按 Tab 筛选：待处理/进行中/我发布/已完成）
     */
    public PageDTO<MeetupCardDTO> queryUserMeetupList(UserMeetupListCmd cmd) {
        String userId = UserContext.get();
        return meetupQueryDomainService.listByUser(cmd, userId);
    }

    private MeetupCardDTO packCard(MeetupData data, Double lng, Double lat) {
        MeetupCardDTO card = MeetupAppConvertMapper.INSTANCE.toMeetupCardDTO(data);
        card.setPrimaryLabel(toPrimaryLabel((card)));
        if (lng != null && lat != null) {
            card.setDistanceKm(GeoUtils.distance(lat, lng, data.getCourtLat(), data.getCourtLng()));
        }
        return card;
    }


    /**
     * 计算主标签：OPEN 状态展示区域名，其余状态展示状态文案
     */
    private String toPrimaryLabel(MeetupCardDTO card) {
        return switch (card.getStatus()) {
            case OPEN -> card.getDistrictName();
            default -> card.getStatus().getLabel();
        };
    }

}
