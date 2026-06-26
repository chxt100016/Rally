package com.rally.meetup;

import com.rally.domain.meetup.enums.MeetupStatusEnum;
import com.rally.domain.meetup.enums.PendingReasonEnum;
import com.rally.domain.meetup.enums.UserMeetupTabEnum;
import com.rally.domain.meetup.model.MeetupCardDTO;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.utils.GeoUtils;
import com.rally.meetup.convert.MeetupAppConvertMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 约球卡片包装服务
 * 负责 MeetupData → MeetupCardDTO 的转换，包括 primaryLabel 和距离计算
 */
@Service
public class MeetupCardPackingService {

    /**
     * 列表查询包装：OPEN 状态 primaryLabel 展示区域名，其余展示状态文案，计算距离
     */
    public MeetupCardDTO packCard(MeetupData data, Double lng, Double lat) {
        MeetupCardDTO card = MeetupAppConvertMapper.INSTANCE.toMeetupCardDTO(data);
        card.setPrimaryLabel(toListPrimaryLabel(data));
        if (lng != null && lat != null) {
            card.setDistanceKm(GeoUtils.distance(lat, lng, data.getCourtLat(), data.getCourtLng()));
        }
        return card;
    }

    /**
     * 用户约球列表包装：按 Tab 计算 primaryLabel，不计算距离
     */
    public MeetupCardDTO packCardForTab(MeetupData data, UserMeetupTabEnum tab) {
        MeetupCardDTO card = MeetupAppConvertMapper.INSTANCE.toMeetupCardDTO(data);
        card.setPrimaryLabel(toTabPrimaryLabel(data, tab));
        return card;
    }

    /**
     * 列表查询 primaryLabel：OPEN→districtName，其他→status.label
     */
    private String toListPrimaryLabel(MeetupData data) {
        return switch (data.getStatus()) {
            case OPEN -> data.getDistrictName();
            default -> data.getStatus().getLabel();
        };
    }

    private String toTabPrimaryLabel(MeetupData data, UserMeetupTabEnum tab) {
        return switch (tab) {
            case RECENT, MY_PUBLISH -> effectiveStatusLabel(data);
            case PENDING -> toPendingLabel(data.getPendingReason());
            case IN_PROGRESS -> data.getDistrictName();
            case COMPLETED -> data.getDistrictName();
        };
    }

    private String effectiveStatusLabel(MeetupData data) {
        boolean expired = data.getStatus() == MeetupStatusEnum.OPEN
                && data.getEndTime() != null && data.getEndTime().isBefore(LocalDateTime.now());
        return expired ? MeetupStatusEnum.FINISHED.getLabel() : data.getStatus().getLabel();
    }

    private String toPendingLabel(PendingReasonEnum reason) {
        return reason != null ? reason.getLabel() : null;
    }
}
