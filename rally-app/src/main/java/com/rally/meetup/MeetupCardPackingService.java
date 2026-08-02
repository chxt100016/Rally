package com.rally.meetup;

import com.rally.domain.court.enums.CourtBackgroundEnum;
import com.rally.domain.court.enums.CourtEnvironmentEnum;
import com.rally.domain.court.enums.CourtSurfaceEnum;
import com.rally.domain.court.model.CourtData;
import com.rally.domain.court.service.CourtDomainService;
import com.rally.domain.meetup.enums.MeetupStatusEnum;
import com.rally.domain.meetup.enums.PendingReasonEnum;
import com.rally.domain.meetup.enums.UserMeetupTabEnum;
import com.rally.domain.meetup.model.MeetupCardDTO;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.utils.GeoUtils;
import com.rally.meetup.convert.MeetupAppConvertMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 约球卡片包装服务
 * 负责 MeetupData → MeetupCardDTO 的转换，包括 primaryLabel、距离计算和背景 key
 */
@Service
@RequiredArgsConstructor
public class MeetupCardPackingService {

    private final CourtDomainService courtDomainService;

    /**
     * 列表查询包装：OPEN 状态 primaryLabel 展示区域名，其余展示状态文案，计算距离
     */
    public MeetupCardDTO packCard(MeetupData data, Double lng, Double lat) {
        MeetupCardDTO card = packBaseCard(data);
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
        MeetupCardDTO card = packBaseCard(data);
        card.setPrimaryLabel(toTabPrimaryLabel(data, tab));
        return card;
    }

    private MeetupCardDTO packBaseCard(MeetupData data) {
        MeetupCardDTO card = MeetupAppConvertMapper.INSTANCE.toMeetupCardDTO(data);
        card.setBackgroundKey(resolveBackgroundKey(data));
        return card;
    }

    /**
     * 根据球场材质、室内外和开球时段解析背景 key。
     * 天气模块未上线，暂传 null 并由枚举降级为晴天。
     */
    public String resolveBackgroundKey(MeetupData data) {
        if (data == null) {
            return null;
        }
        CourtSurfaceEnum surface = null;
        CourtEnvironmentEnum environment = null;
        if (data.getCourtId() != null && !data.getCourtId().isBlank()) {
            CourtData court = courtDomainService.getByBizId(data.getCourtId());
            if (court != null) {
                surface = court.getSurface();
                environment = court.getType();
            }
        }
        return CourtBackgroundEnum.resolveKey(surface, environment, data.getStartTime(), null);
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
