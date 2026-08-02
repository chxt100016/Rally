package com.rally.notify;

import com.rally.domain.notify.enums.NoticeScene;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 赛事订阅场景解析与微信模板字段装配。 */
public final class TournamentNotifyAssembler {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final EnumSet<NoticeScene> TOURNAMENT_SCENES = EnumSet.of(
            NoticeScene.TOURNAMENT_MATCHED,
            NoticeScene.TOURNAMENT_BOOKING_SUBMITTED,
            NoticeScene.TOURNAMENT_REJECTED);

    private TournamentNotifyAssembler() {
    }

    /** 只接受赛事业务的三种订阅场景，忽略非法或其他业务场景。 */
    public static List<NoticeScene> parseScenes(List<String> names) {
        if (names == null) {
            return List.of();
        }
        return names.stream()
                .map(NoticeScene::parse)
                .filter(Objects::nonNull)
                .filter(TOURNAMENT_SCENES::contains)
                .distinct()
                .toList();
    }

    /** 匹配通知：thing7 赛事名称 / phrase2 匹配状态。 */
    public static Map<String, Object> matchedData(String tournamentName) {
        Map<String, Object> data = new HashMap<>();
        data.put("thing7", tournamentName);
        // 微信模板的 phrase 类型最多支持 5 个汉字，详细对手信息由赛事详情页展示。
        data.put("phrase2", "匹配成功");
        return data;
    }

    /** 订场通知：thing5 赛事名称 / time1 比赛时间 / thing4 地点 / thing3 提醒。 */
    public static Map<String, Object> bookingSubmittedData(String tournamentName, LocalDateTime startTime, String courtName) {
        Map<String, Object> data = new HashMap<>();
        data.put("thing5", tournamentName);
        data.put("time1", startTime.format(TIME_FMT));
        data.put("thing4", courtName);
        data.put("thing3", "点击打开比赛页面确认赛约");
        return data;
    }

    /** 拒绝通知：thing1 比赛名称 / thing3 取消原因。 */
    public static Map<String, Object> rejectedData(String tournamentName) {
        Map<String, Object> data = new HashMap<>();
        data.put("thing1", tournamentName);
        data.put("thing3", "对手使用拒绝比赛机会。");
        return data;
    }
}
