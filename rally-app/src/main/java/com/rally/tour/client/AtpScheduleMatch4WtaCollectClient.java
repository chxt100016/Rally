package com.rally.tour.client;

import com.rally.tour.parser.CollectType;
import com.rally.tour.parser.DrawMeta;
import com.rally.tour.parser.DrawParams;
import com.rally.tour.parser.DrawResult;

import com.rally.client.atp.AtpClient;
import com.rally.client.wta.model.WtaScheduleResponse;
import com.rally.tour.model.Discipline;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 通过 ATP 赛程接口采集 WTA 比赛的 Client。
 * 响应结构与 WtaScheduleResponse 相同，按赛事+年份拉取，
 * 过滤 Tour=ATP 且 MatchId 以 "MS" 开头的单打比赛。
 */
@Slf4j
@Component
public class AtpScheduleMatch4WtaCollectClient extends AtpScheduleMatchCollectClient {

    private static final DateTimeFormatter UTC_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId UTC_ZONE = ZoneId.of("UTC");
    private static final ZoneId CST_ZONE = ZoneId.of("Asia/Shanghai");

    @Resource
    private AtpClient atpClient;

    @Override
    protected WtaScheduleResponse request(DrawParams params) {
        return atpClient.getSchedule(collectType().getApiUrl(), params.getTournamentId(), params.getYear());
    }


    @Override
    protected List<DrawResult<WtaScheduleResponse.ScheduleData>> ls(WtaScheduleResponse data, DrawParams params) {
        if (data == null || data.getData() == null || CollectionUtils.isEmpty(data.getData().getScheduleDays())) {
            return List.of();
        }

        for (WtaScheduleResponse.ScheduleDay day : data.getData().getScheduleDays()) {
            if (CollectionUtils.isEmpty(day.getScheduleCourts())) continue;
            for (WtaScheduleResponse.ScheduleCourt court : day.getScheduleCourts()) {
                if (CollectionUtils.isEmpty(court.getScheduleMatches())) continue;
                // 只保留 ATP 单打比赛
                court.setScheduleMatches(court.getScheduleMatches().stream()
                        .filter(m -> "WTA".equals(m.getTour())
                                && m.getMatchId() != null
                                && m.getMatchId().startsWith("LS"))
                        .toList());
            }
        }

        return List.of(new DrawResult<>(data.getData(), Discipline.SINGLES, "LS",
                new DrawMeta(null, null), params.getTournamentId(), params.getYear()));
    }

    @Override
    protected List<DrawResult<WtaScheduleResponse.ScheduleData>> ms(WtaScheduleResponse data, DrawParams params) {
        return List.of();
    }

    @Override
    protected List<DrawResult<WtaScheduleResponse.ScheduleData>> md(WtaScheduleResponse data, DrawParams params) {
        return List.of();
    }
    @Override
    protected List<DrawResult<WtaScheduleResponse.ScheduleData>> ld(WtaScheduleResponse data, DrawParams params) {
        return List.of();
    }



    @Override
    public CollectType collectType() {
        return CollectType.ATP_SCHEDULE_FOR_WTA;
    }
}
