package com.rally.meetup;

import com.rally.domain.court.enums.CourtBackgroundEnum;
import com.rally.domain.court.enums.CourtEnvironmentEnum;
import com.rally.domain.court.enums.CourtSurfaceEnum;
import com.rally.domain.court.model.CourtData;
import com.rally.domain.court.service.CourtDomainService;
import com.rally.domain.meetup.model.MeetupData;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 约球卡片背景样式解析：按球场材质、室内外、开球时段和天气返回样式标识，前端用 CSS 渐变渲染。
 * 天气模块未上线，weather 暂传 null 降级晴天；无 courtId 时降级 HARD/OUTDOOR。
 */
@Service
@RequiredArgsConstructor
public class MeetupBackgroundResolver {

    private final CourtDomainService courtDomainService;

    /**
     * 解析背景样式标识，如 hard-day-clear、indoor-clay。
     */
    public String resolveStyle(MeetupData data) {
        if (data == null || data.getStartTime() == null) {
            return null;
        }
        CourtSurfaceEnum surface = null;
        CourtEnvironmentEnum venue = null;
        String courtId = data.getCourtId();
        if (StringUtils.isNotBlank(courtId)) {
            CourtData court = courtDomainService.getByBizId(courtId);
            if (court != null) {
                surface = court.getSurface();
                venue = court.getType();
            }
        }
        return CourtBackgroundEnum.resolveStyle(surface, venue, data.getStartTime(), null);
    }
}
