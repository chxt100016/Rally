package com.rally.meetup;

import com.rally.config.property.QiniuConfiguration;
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
 * 约球卡片底图解析：按球场材质 + 室内外 + 开球时段 + 天气动态算 key，转签名 URL。
 * 天气模块未上线，weather 暂传 null（降级晴天）；FREE 模式无 courtId 时降级 HARD/OUTDOOR。
 */
@Service
@RequiredArgsConstructor
public class MeetupBackgroundResolver {

    private final CourtDomainService courtDomainService;

    /**
     * 解析底图签名 URL。
     */
    public String resolveUrl(MeetupData data) {
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
        String key = CourtBackgroundEnum.resolveKey(surface, venue, data.getStartTime(), null);
        return QiniuConfiguration.buildSignedUrl(key);
    }
}
