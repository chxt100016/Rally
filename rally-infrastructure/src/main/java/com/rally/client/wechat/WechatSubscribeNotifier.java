package com.rally.client.wechat;

import com.rally.config.WechatAppProperties;
import com.rally.db.auth.entity.AccountPO;
import com.rally.db.auth.service.AccountService;
import com.rally.domain.auth.enums.ChannelEnum;
import com.rally.domain.notify.enums.NotifyChannel;
import com.rally.domain.notify.gateway.Notifier;
import com.rally.domain.notify.model.NotifyMessage;
import com.rally.domain.notify.model.NotifyResult;
import com.rally.domain.utils.Http;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 微信订阅消息渠道实现：解析 openid、获取 access_token、调用订阅消息发送接口。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WechatSubscribeNotifier implements Notifier {

    private final WechatAppProperties properties;
    private final AccountService accountService;
    private final WechatAccessTokenClient accessTokenClient;

    @Override
    public NotifyChannel channel() {
        return NotifyChannel.WECHAT_SUBSCRIBE;
    }

    @Override
    public NotifyResult send(NotifyMessage message) {
        WechatSubscribeTemplate template = WechatSubscribeTemplate.from(message.getScene());
        if (template == null) {
            return NotifyResult.failed("TEMPLATE_NOT_CONFIGURED", "微信订阅模板未配置: " + message.getScene(), null);
        }
        String openid = resolveOpenid(message.getUserId());
        if (StringUtils.isBlank(openid)) {
            return NotifyResult.skipped("OPENID_NOT_FOUND", "用户无微信openid: " + message.getUserId(), template.templateId());
        }
        String token = accessTokenClient.getAccessToken();
        if (StringUtils.isBlank(token)) {
            return NotifyResult.failed("ACCESS_TOKEN_UNAVAILABLE", "获取access_token失败", template.templateId());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("touser", openid);
        body.put("template_id", template.templateId());
        body.put("page", template.page(message.getRefBizId()));
        // 跳转版本：开发/体验测试需设为 developer/trial，否则默认 formal 会去打开正式版导致跳不到详情
        body.put("miniprogram_state", StringUtils.isNotBlank(properties.getSubscribeMiniprogramState()) ? properties.getSubscribeMiniprogramState() : "formal");
        body.put("data", wrapData(template.mapData(message.getData())));

        String url = properties.getSubscribeSendUrl() + "?access_token=" + token;
        SubscribeSendResponse resp = Http.uri(url).jsonHeader().entity(body).doPost().result(SubscribeSendResponse.class);
        if (resp == null) {
            return NotifyResult.failed("NO_RESPONSE", "订阅消息发送无响应", template.templateId());
        }
        if (resp.getErrcode() != 0) {
            if (resp.getErrcode() == 43101) {
                return NotifyResult.skipped("43101", resp.getErrmsg(), template.templateId());
            }
            log.warn("订阅消息发送失败: openid={}, template={}, errcode={}, errmsg={}",
                    openid, template.templateId(), resp.getErrcode(), resp.getErrmsg());
            return NotifyResult.failed(String.valueOf(resp.getErrcode()), resp.getErrmsg(), template.templateId());
        }
        return NotifyResult.sent(resp.getMsgid(), template.templateId());
    }

    /** 将原始 key->value 包装为微信要求的 key->{value:v} 结构 */
    private Map<String, Object> wrapData(Map<String, Object> raw) {
        Map<String, Object> data = new HashMap<>();
        if (raw == null) {
            return data;
        }
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            Map<String, Object> valueWrap = new HashMap<>();
            valueWrap.put("value", entry.getValue());
            data.put(entry.getKey(), valueWrap);
        }
        return data;
    }

    private String resolveOpenid(String userId) {
        AccountPO account = accountService.lambdaQuery().eq(AccountPO::getChannel, ChannelEnum.WECHAT_MINIAPP.name()).eq(AccountPO::getUserId, userId).last("LIMIT 1").one();
        return account != null ? account.getIdentifier() : null;
    }

    @Data
    private static class SubscribeSendResponse {
        private int errcode;
        private String errmsg;
        private String msgid;
    }
}
