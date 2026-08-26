package com.rally.notify;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/** 赛事通知的渠道无关内容变量装配。 */
public final class TournamentNotifyAssembler {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private TournamentNotifyAssembler() {
    }

    /** 匹配通知。 */
    public static Map<String, Object> matchedData(String tournamentName) {
        Map<String, Object> data = new HashMap<>();
        data.put("tournamentName", tournamentName);
        data.put("matchStatus", "匹配成功");
        return data;
    }

    /** 订场通知。 */
    public static Map<String, Object> bookingSubmittedData(String tournamentName, LocalDateTime startTime, String courtName) {
        Map<String, Object> data = new HashMap<>();
        data.put("tournamentName", tournamentName);
        data.put("matchTime", startTime.format(TIME_FMT));
        data.put("location", courtName);
        data.put("reminder", "点击打开比赛页面确认赛约");
        return data;
    }

    /** 拒绝通知。 */
    public static Map<String, Object> rejectedData(String tournamentName) {
        Map<String, Object> data = new HashMap<>();
        data.put("tournamentName", tournamentName);
        data.put("rejectReason", "对手使用拒绝比赛机会。");
        return data;
    }
}
