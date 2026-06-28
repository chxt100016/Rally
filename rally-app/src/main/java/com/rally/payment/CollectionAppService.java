package com.rally.payment;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.enums.ChannelEnum;
import com.rally.domain.auth.gateway.AccountRepository;
import com.rally.domain.meetup.enums.MeetupStatusEnum;
import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.payment.enums.PayChannelEnum;
import com.rally.domain.payment.model.CollectionBatchDTO;
import com.rally.domain.payment.model.CollectionInitiateCmd;
import com.rally.domain.payment.model.PaymentOrder;
import com.rally.domain.payment.service.PaymentDomainService;
import com.rally.domain.payment.service.PaymentQueryDomainService;
import com.rally.domain.payment.service.ShareReceiverDomainService;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.system.enums.SystemConfigKey;
import com.rally.domain.utils.Assert;
import com.rally.payment.convert.PaymentAppConvertMapper;
import com.rally.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 发起收款 / 关闭收款应用服务。
 * app 层只做跨域编排与鉴权：取约球域只读名单 + 校验发起人/已结束/参与者，支付业务规则下沉领域层。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionAppService {

    private final MeetupDomainService meetupDomainService;
    private final PaymentDomainService paymentDomainService;
    private final PaymentQueryDomainService paymentQueryDomainService;
    private final ShareReceiverDomainService shareReceiverDomainService;
    private final AccountRepository accountRepository;

    /**
     * 发起收款（活动结束后，发起人触发，MVP 每场仅一次）。
     */
    @Transactional
    public CollectionBatchDTO initiate(CollectionInitiateCmd cmd) {
        String userId = UserContext.get();

        // 1. 取约球只读聚合根，校验「已结束 + 当前用户为发起人」
        Meetup meetup = meetupDomainService.get(cmd.getMeetupId());
        Assert.isTrue(meetup.getRealStatus() == MeetupStatusEnum.FINISHED, BizErrorCode.COLLECTION_NOT_ALLOWED);
        meetup.assertOwner(userId);

        // 2. 解析应付人：未勾选则取全体有效参与者（排除发起人）；勾选则校验均为有效参与者
        List<String> validParticipants = meetup.getActiveParticipantIds(meetup.getCreatorId());
        List<String> payerUserIds = resolvePayers(cmd.getPayerUserIds(), validParticipants);

        // 3. 分账接收方前置绑定（设计 §5.1-4）：取发起人 openid，提前在发起阶段暴露绑定失败
        String payeeOpenid = accountRepository.findIdentifierByUser(meetup.getCreatorId(), ChannelEnum.WECHAT_MINIAPP);
        Assert.notBlank(payeeOpenid, BizErrorCode.SHARE_RECEIVER_BIND_FAILED);
        shareReceiverDomainService.ensureBound(meetup.getCreatorId(), payeeOpenid, PayChannelEnum.WECHAT);

        // 4. 发起收款（领域层：校验 + 平摊 + 批量建单，uk_batch_payer 防重）
        List<PaymentOrder> orders = paymentDomainService.createBatch(cmd.getMeetupId(), meetup.getCreatorId(), payeeOpenid, payerUserIds, cmd.getTotalAmount(), PayChannelEnum.WECHAT);

        // TODO(P1): 通知应付人「{发起人}发起了收款，请支付」（复用 NotifySubscribe，设计 §5.1-7）

        return toBatchDTO(cmd, orders);
    }

    /**
     * 关闭收款（发起人手动，未付单 → CLOSED，已付不动）。
     */
    @Transactional
    public void close(String meetupId) {
        String userId = UserContext.get();

        // 1. 校验当前用户为发起人，且该场已存在收款
        Meetup meetup = meetupDomainService.get(meetupId);
        meetup.assertOwner(userId);
        Assert.isTrue(paymentQueryDomainService.hasAnyOrder(meetupId), BizErrorCode.COLLECTION_NOT_ALLOWED);

        // 2. 领域层关闭本场全部 PENDING 单 + 渠道关单（best-effort）
        paymentDomainService.closeBatch(meetupId, userId);
    }

    private List<String> resolvePayers(List<String> selected, List<String> validParticipants) {
        if (selected == null || selected.isEmpty()) {
            Assert.isTrue(!validParticipants.isEmpty(), BizErrorCode.COLLECTION_NOT_ALLOWED);
            return validParticipants;
        }
        Set<String> validSet = new HashSet<>(validParticipants);
        Assert.isTrue(validSet.containsAll(selected), BizErrorCode.PARAM_ERROR);
        return selected;
    }

    private CollectionBatchDTO toBatchDTO(CollectionInitiateCmd cmd, List<PaymentOrder> orders) {
        CollectionBatchDTO dto = new CollectionBatchDTO();
        dto.setMeetupId(cmd.getMeetupId());
        dto.setTotalAmount(cmd.getTotalAmount());
        dto.setPayerCount(orders.size());
        dto.setBatchId(orders.get(0).getData().getCollectionBatchId());
        dto.setFeeDesc(SystemConfig.getString(SystemConfigKey.PAYMENT_WECHAT_FEE_DESC.getKey()));
        dto.setOrders(PaymentAppConvertMapper.INSTANCE.toSummaryList(orders));
        return dto;
    }
}

