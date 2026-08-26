package com.rally.notify;

import com.rally.domain.meetup.model.MeetupData;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 约球通知的渠道无关内容变量装配。
 */
public final class MeetupNotifyAssembler {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private MeetupNotifyAssembler() {
    }

    /** 报名成功通知。 */
    public static Map<String, Object> joinSuccessData(MeetupData data) {
        Map<String, Object> map = new HashMap<>();
        map.put("activityName", data.getTitle());
        map.put("activityTime", data.getStartTime().format(TIME_FMT));
        map.put("activityLocation", data.getCourtName());
        map.put("remark", courtNoRemark(data));
        return map;
    }

    /** 活动申请审批提醒。 */
    public static Map<String, Object> pendingApprovalData(MeetupData data, String applicantNickname) {
        Map<String, Object> map = new HashMap<>();
        map.put("activityName", data.getTitle());
        map.put("activityTime", data.getStartTime().format(TIME_FMT));
        map.put("activityLocation", data.getCourtName());
        map.put("applicantName", applicantNickname);
        return map;
    }

    /** 活动取消通知。 */
    public static Map<String, Object> meetupCancelData(MeetupData data) {
        Map<String, Object> map = new HashMap<>();
        map.put("activityName", data.getTitle());
        map.put("activityTime", data.getStartTime().format(TIME_FMT));
        map.put("activityLocation", data.getCourtName());
        map.put("cancelReason", "创建人取消");
        return map;
    }

    /** 组团成功提醒。 */
    public static Map<String, Object> teamSuccessData(MeetupData data) {
        Map<String, Object> map = new HashMap<>();
        map.put("activityName", data.getTitle());
        map.put("activityTime", data.getStartTime().format(TIME_FMT));
        map.put("activityLocation", data.getCourtName());
        map.put("remark", courtNoRemark(data));
        return map;
    }

    /** 成员退出通知。 */
    public static Map<String, Object> memberQuitData(MeetupData data, String quitNickname) {
        Map<String, Object> map = new HashMap<>();
        map.put("activityName", data.getTitle());
        map.put("activityTime", data.getStartTime().format(TIME_FMT));
        map.put("quitMember", quitNickname);
        map.put("quitTime", LocalDateTime.now().format(TIME_FMT));
        return map;
    }

    private static String courtNoRemark(MeetupData data) {
        return "场地号：" + (data.getCourtIndex() == null ? "" : data.getCourtIndex());
    }
}
