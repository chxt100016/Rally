package com.rally.domain.notify.enums;

import lombok.Getter;

/**
 * 通知场景 -> 微信订阅模板映射（写死，后续新增场景在此维护）
 * 注意：templateId 为微信公众平台申请到的一次性订阅模板ID，需替换为真实值。
 */
@Getter
public enum NoticeScene {

    /** 报名成功通知（免审批活动加入即发；需审批活动审批通过后发）—— 收件人：参与人 */
    JOIN_SUCCESS("报名成功通知", "gfKQH9Rsf1s3LwbZ8WV-c7Fz3Y7gF9l38_DvFpPPEsI", "pages/meetup/detail/detail", NotifyChannel.WECHAT_SUBSCRIBE),

    /** 活动申请审批提醒 —— 收件人：创建人（仅需审批活动） */
    PENDING_APPROVAL("活动申请审批提醒", "dGH_yvXzWeLFpgRo87vYxkxPt5DCOHU00ufB4_-tNjE", "pages/meetup/detail/detail", NotifyChannel.WECHAT_SUBSCRIBE),

    /** 活动取消通知 —— 收件人：全体已加入参与人 */
    MEETUP_CANCEL("活动取消通知", "u60MifrcIQZNihgroNt3dCVn2tAt7B5kDDlnZ5s_Zz4", "pages/meetup/detail/detail", NotifyChannel.WECHAT_SUBSCRIBE),

    /** 组团成功提醒 —— 收件人：全体已加入参与人 */
    TEAM_SUCCESS("组团成功提醒", "GV_XfB27iGPWoXJsaPbV-xJjGz59KmRcJYuJWgFgmLk", "pages/meetup/detail/detail", NotifyChannel.WECHAT_SUBSCRIBE),

    /** 成员退出通知 —— 收件人：创建人/全体已加入参与人 */
    MEMBER_QUIT("成员退出通知", "vRTf8GAOXTTeewdS8ivCEifKTf42uIAH4eIrdERtycc", "pages/meetup/detail/detail", NotifyChannel.WECHAT_SUBSCRIBE);

    private final String desc;
    private final String templateId;
    private final String page;
    private final NotifyChannel channel;

    NoticeScene(String desc, String templateId, String page, NotifyChannel channel) {
        this.desc = desc;
        this.templateId = templateId;
        this.page = page;
        this.channel = channel;
    }

    /** 安全解析：非法值返回 null */
    public static NoticeScene parse(String name) {
        if (name == null) {
            return null;
        }
        for (NoticeScene scene : values()) {
            if (scene.name().equals(name)) {
                return scene;
            }
        }
        return null;
    }
}
