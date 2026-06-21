package com.rally.domain.notify.model;

import com.rally.domain.notify.enums.NoticeScene;
import lombok.Data;

import java.util.Map;

/**
 * 通知消息（渠道无关）。openid 等渠道身份由具体 Notifier 实现自行解析。
 */
@Data
public class NotifyMessage {

    /** 接收人 userId */
    private String userId;
    /** 场景 */
    private NoticeScene scene;
    /** 模板ID */
    private String templateId;
    /** 跳转页面 */
    private String page;
    /** 模板字段数据（key 为模板字段名，value 为原始值，由渠道实现包装） */
    private Map<String, Object> data;

    public static NotifyMessage of(NotifySubscribe subscribe, Map<String, Object> data) {
        NotifyMessage message = new NotifyMessage();
        message.setUserId(subscribe.getUserId());
        message.setScene(subscribe.getNoticeScene());
        message.setTemplateId(subscribe.getNoticeScene().getTemplateId());
        // 跳转到对应活动详情页（refBizId 即 meetupId，详情页参数名为 id）
        message.setPage(subscribe.getNoticeScene().getPage() + "?id=" + subscribe.getRefBizId());
        message.setData(data);
        return message;
    }
}
