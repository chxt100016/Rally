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
    /** 关联业务对象 ID，供渠道生成跳转链接或内容 */
    private String refBizId;
    /** 渠道无关的语义化内容变量，由渠道适配器转换为具体模板字段 */
    private Map<String, Object> data;

    public static NotifyMessage of(String userId, String refBizId, NoticeScene scene, Map<String, Object> data) {
        NotifyMessage message = new NotifyMessage();
        message.setUserId(userId);
        message.setRefBizId(refBizId);
        message.setScene(scene);
        message.setData(data);
        return message;
    }
}
