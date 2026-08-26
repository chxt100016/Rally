package com.rally.client.wechat;

import com.rally.domain.notify.enums.NoticeScene;

import java.util.LinkedHashMap;
import java.util.Map;

/** 微信订阅渠道的场景模板与字段映射。 */
enum WechatSubscribeTemplate {

    JOIN_SUCCESS(NoticeScene.JOIN_SUCCESS, "gfKQH9Rsf1s3LwbZ8WV-c7Fz3Y7gF9l38_DvFpPPEsI", "pages/meetup/detail/detail",
            Map.of("thing2", "activityName", "date4", "activityTime", "thing31", "activityLocation", "thing19", "remark")),
    PENDING_APPROVAL(NoticeScene.PENDING_APPROVAL, "dGH_yvXzWeLFpgRo87vYxkxPt5DCOHU00ufB4_-tNjE", "pages/meetup/detail/detail",
            Map.of("thing2", "activityName", "date3", "activityTime", "thing6", "activityLocation", "name1", "applicantName")),
    MEETUP_CANCEL(NoticeScene.MEETUP_CANCEL, "u60MifrcIQZNihgroNt3dCVn2tAt7B5kDDlnZ5s_Zz4", "pages/meetup/detail/detail",
            Map.of("thing1", "activityName", "date2", "activityTime", "thing3", "activityLocation", "thing5", "cancelReason")),
    TEAM_SUCCESS(NoticeScene.TEAM_SUCCESS, "GV_XfB27iGPWoXJsaPbV-xJjGz59KmRcJYuJWgFgmLk", "pages/meetup/detail/detail",
            Map.of("thing10", "activityName", "date3", "activityTime", "thing4", "activityLocation", "thing12", "remark")),
    MEMBER_QUIT(NoticeScene.MEMBER_QUIT, "vRTf8GAOXTTeewdS8ivCEifKTf42uIAH4eIrdERtycc", "pages/meetup/detail/detail",
            Map.of("thing1", "activityName", "time2", "activityTime", "thing3", "quitMember", "time4", "quitTime")),
    TOURNAMENT_MATCHED(NoticeScene.TOURNAMENT_MATCHED, "_pf-ADsRnNZs8Pc3cIA3fpz1a4fimPnT0p7OU2suEl8", "pages/tournament-detail/tournament-detail",
            Map.of("thing7", "tournamentName", "phrase2", "matchStatus")),
    TOURNAMENT_BOOKING_SUBMITTED(NoticeScene.TOURNAMENT_BOOKING_SUBMITTED, "zLs0GKZdKVZAM4efd6TMq4zJUQppXxJCDWzi5ssmeqE", "pages/tournament-detail/tournament-detail",
            Map.of("thing5", "tournamentName", "time1", "matchTime", "thing4", "location", "thing3", "reminder")),
    TOURNAMENT_REJECTED(NoticeScene.TOURNAMENT_REJECTED, "zIdEOoMCKYP8tW_5zUNBJTW2EHjeG5HiklWGV0YV8ilpfI", "pages/tournament-detail/tournament-detail",
            Map.of("thing1", "tournamentName", "thing3", "rejectReason"));

    private final NoticeScene scene;
    private final String templateId;
    private final String page;
    private final Map<String, String> fieldMapping;

    WechatSubscribeTemplate(NoticeScene scene, String templateId, String page, Map<String, String> fieldMapping) {
        this.scene = scene;
        this.templateId = templateId;
        this.page = page;
        this.fieldMapping = fieldMapping;
    }

    String templateId() {
        return templateId;
    }

    String page(String refBizId) {
        return page + "?id=" + refBizId;
    }

    Map<String, Object> mapData(Map<String, Object> variables) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (variables == null) {
            return data;
        }
        fieldMapping.forEach((templateField, variableName) -> {
            if (variables.containsKey(variableName)) {
                data.put(templateField, variables.get(variableName));
            }
        });
        return data;
    }

    static WechatSubscribeTemplate from(NoticeScene scene) {
        for (WechatSubscribeTemplate template : values()) {
            if (template.scene == scene) {
                return template;
            }
        }
        return null;
    }
}
