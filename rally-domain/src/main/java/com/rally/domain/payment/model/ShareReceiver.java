package com.rally.domain.payment.model;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.payment.enums.PayChannelEnum;
import com.rally.domain.payment.enums.ShareReceiverStatusEnum;
import com.rally.domain.utils.Assert;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 分账接收方账本聚合根（充血模型，见设计 §15.3）。
 * 状态机：UNBOUND ⇄ BOUND。聚合根只管状态翻转，调渠道 add/delete 由 DomainService 编排。
 */
@Getter
public class ShareReceiver {

    private final ShareReceiverData data;

    public ShareReceiver(ShareReceiverData data) {
        this.data = data;
    }

    public String getBizId() {
        return data.getBizId();
    }

    /** 工厂：初态 UNBOUND */
    public static ShareReceiver create(PayChannelEnum channel, String userId, String openid) {
        ShareReceiverData data = new ShareReceiverData();
        data.setBizId(IdWorker.getIdStr());
        data.setChannel(channel);
        data.setUserId(userId);
        data.setAccountType("PERSONAL_OPENID");
        data.setAccount(openid);
        data.setBindStatus(ShareReceiverStatusEnum.UNBOUND);
        return new ShareReceiver(data);
    }

    public boolean isBound() {
        return data.getBindStatus() == ShareReceiverStatusEnum.BOUND;
    }

    /** UNBOUND → BOUND + 刷新 lastShareTime（渠道 add 成功后由 service 调） */
    public void bind() {
        data.setBindStatus(ShareReceiverStatusEnum.BOUND);
        LocalDateTime now = LocalDateTime.now();
        data.setBoundTime(now);
        data.setLastShareTime(now);
    }

    /** 已绑定时刷新 lastShareTime（LRU 依据，取发起收款时间） */
    public void touch(LocalDateTime t) {
        data.setLastShareTime(t);
    }

    /** BOUND → UNBOUND（前置"无 PROCESSING 分账"由 service 校验后调） */
    public void unbind() {
        data.setBindStatus(ShareReceiverStatusEnum.UNBOUND);
        data.setUnbindTime(LocalDateTime.now());
    }

    /** 分账前置防御 */
    public void assertBound() {
        Assert.isTrue(isBound(), BizErrorCode.SHARE_RECEIVER_BIND_FAILED);
    }
}
