package com.rally.notify;

import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.notify.enums.NoticeScene;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 约球通知装配：场景解析 + 各场景模板字段数据组装。
 * 各场景模板字段名不同，逐场景维护，与微信公众平台申请的模板字段保持一致。
 * 通用：活动名称=title，活动时间=startTime，活动地点=courtName。
 */
public final class MeetupNotifyAssembler {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private MeetupNotifyAssembler() {
    }

    /** 解析前端上报的授权场景，忽略非法值 */
    public static List<NoticeScene> parseScenes(List<String> names) {
        if (names == null) {
            return List.of();
        }
        return names.stream().map(NoticeScene::parse).filter(Objects::nonNull).toList();
    }

    /** 报名成功通知：thing2 活动名称 / date4 活动时间 / thing31 活动地点 / thing19 备注(场地号) */
    public static Map<String, Object> joinSuccessData(MeetupData data) {
        Map<String, Object> map = new HashMap<>();
        map.put("thing2", data.getTitle());
        map.put("date4", data.getStartTime().format(TIME_FMT));
        map.put("thing31", data.getCourtName());
        map.put("thing19", courtNoRemark(data));
        return map;
    }

    /** 活动申请审批提醒：thing2 活动名称 / date3 活动时间 / thing6 活动地点 / name1 申请方昵称 */
    public static Map<String, Object> pendingApprovalData(MeetupData data, String applicantNickname) {
        Map<String, Object> map = new HashMap<>();
        map.put("thing2", data.getTitle());
        map.put("date3", data.getStartTime().format(TIME_FMT));
        map.put("thing6", data.getCourtName());
        map.put("name1", applicantNickname);
        return map;
    }

    /** 活动取消通知：thing1 活动名称 / date2 活动时间 / thing3 活动地点 / thing5 取消原因(默认创建人取消) */
    public static Map<String, Object> meetupCancelData(MeetupData data) {
        Map<String, Object> map = new HashMap<>();
        map.put("thing1", data.getTitle());
        map.put("date2", data.getStartTime().format(TIME_FMT));
        map.put("thing3", data.getCourtName());
        map.put("thing5", "创建人取消");
        return map;
    }

    /** 组团成功提醒：thing10 活动名称 / date3 活动时间 / thing4 活动地点 / thing12 备注(场地号) */
    public static Map<String, Object> teamSuccessData(MeetupData data) {
        Map<String, Object> map = new HashMap<>();
        map.put("thing10", data.getTitle());
        map.put("date3", data.getStartTime().format(TIME_FMT));
        map.put("thing4", data.getCourtName());
        map.put("thing12", courtNoRemark(data));
        return map;
    }

    /** 成员退出通知：thing1 活动名称 / time2 活动开始时间 / thing3 退出成员 / time4 退出时间 */
    public static Map<String, Object> memberQuitData(MeetupData data, String quitNickname) {
        Map<String, Object> map = new HashMap<>();
        map.put("thing1", data.getTitle());
        map.put("time2", data.getStartTime().format(TIME_FMT));
        map.put("thing3", quitNickname);
        map.put("time4", LocalDateTime.now().format(TIME_FMT));
        return map;
    }

    private static String courtNoRemark(MeetupData data) {
        return "场地号：" + (data.getCourtIndex() == null ? "" : data.getCourtIndex());
    }
}
