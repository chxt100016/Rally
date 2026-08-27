package com.rally.domain.delivery;

import com.rally.domain.notify.enums.NoticeScene;
import com.rally.domain.notify.enums.NotifyBizType;
import com.rally.domain.notify.enums.NotifyChannel;
import com.rally.domain.notify.enums.NotifyDeliveryStatus;
import com.rally.domain.notify.model.NotifyDeliveryLog;
import com.rally.domain.notify.model.NotifyResult;

import java.time.LocalDateTime;
/**
 * {@code notification_delivery_log} 的单行聚合根。
 *
 * <p>创建和终态迁移都先完成 I1-I5 校验，再向外暴露可持久化快照。数据库的两个唯一约束
 * 仍是并发下取得执行权的最终判定。</p>
 */
public final class NotificationDelivery {

    public static final String DELIVERY_LOG_INVALID = "DELIVERY_LOG_INVALID";
    public static final String DELIVERY_STATE_INVALID = "DELIVERY_STATE_INVALID";

    private static final int BIZ_ID_MAX = 32;
    private static final int EVENT_ID_MAX = 128;
    private static final int REF_BIZ_ID_MAX = 32;
    private static final int RECIPIENT_ID_MAX = 64;
    private static final int TEMPLATE_ID_MAX = 128;
    private static final int MESSAGE_ID_MAX = 128;
    private static final int ERROR_CODE_MAX = 64;
    private static final int FAIL_REASON_MAX = 255;

    private final String bizId;
    private final String eventId;
    private final NotifyBizType bizType;
    private final String refBizId;
    private final NoticeScene noticeScene;
    private final String recipientId;
    private final NotifyChannel channel;
    private final NotifyDeliveryStatus status;
    private final String providerTemplateId;
    private final String providerMessageId;
    private final String errorCode;
    private final String failReason;
    private final LocalDateTime sendTime;

    private NotificationDelivery(
            String bizId,
            String eventId,
            NotifyBizType bizType,
            String refBizId,
            NoticeScene noticeScene,
            String recipientId,
            NotifyChannel channel,
            NotifyDeliveryStatus status,
            String providerTemplateId,
            String providerMessageId,
            String errorCode,
            String failReason,
            LocalDateTime sendTime) {
        this.bizId = bizId;
        this.eventId = eventId;
        this.bizType = bizType;
        this.refBizId = refBizId;
        this.noticeScene = noticeScene;
        this.recipientId = recipientId;
        this.channel = channel;
        this.status = status;
        this.providerTemplateId = providerTemplateId;
        this.providerMessageId = providerMessageId;
        this.errorCode = errorCode;
        this.failReason = failReason;
        this.sendTime = sendTime;
        checkInvariants();
    }

    /** C2：建立 SENDING 聚合；仓储唯一插入成功后调用方才取得外部渠道执行权。 */
    public static NotificationDelivery acquire(
            AcquireDeliveryCommand command, DeliveryIdGenerator idGenerator) {
        require(command != null, DELIVERY_LOG_INVALID, "取得触达执行权命令不能为空");
        require(idGenerator != null, DELIVERY_LOG_INVALID, "触达流水号生成器不能为空");
        return new NotificationDelivery(
                idGenerator.nextDeliveryId(),
                command.eventId(),
                command.bizType(),
                command.refBizId(),
                command.noticeScene(),
                command.recipientId(),
                command.channel(),
                NotifyDeliveryStatus.SENDING,
                null,
                null,
                null,
                null,
                null);
    }

    /** C4：从 SENDING 迁移到渠道终态，并在同一快照写入结束时间及审计结果。 */
    public NotificationDelivery recordResult(
            RecordDeliveryResultCommand command, LocalDateTime finishedAt) {
        require(command != null, DELIVERY_STATE_INVALID, "触达结果命令不能为空");
        require(status == NotifyDeliveryStatus.SENDING,
                DELIVERY_STATE_INVALID,
                "只有 SENDING 日志可以回写结果");
        require(isTerminal(command.status()),
                DELIVERY_STATE_INVALID,
                "触达结果必须是 SENT、FAILED 或 SKIPPED");
        require(finishedAt != null, DELIVERY_STATE_INVALID, "终态必须记录结束时间");

        NotificationDelivery completed = new NotificationDelivery(
                bizId,
                eventId,
                bizType,
                refBizId,
                noticeScene,
                recipientId,
                channel,
                command.status(),
                truncate(command.providerTemplateId(), TEMPLATE_ID_MAX),
                truncate(command.providerMessageId(), MESSAGE_ID_MAX),
                truncate(command.errorCode(), ERROR_CODE_MAX),
                truncate(command.failReason(), FAIL_REASON_MAX),
                finishedAt);
        completed.checkInvariants();
        return completed;
    }

    /** 兼容既有 notify 仓储端口的 SENDING 写入载体。 */
    public NotifyDeliveryLog toLegacyStartLog() {
        require(status == NotifyDeliveryStatus.SENDING,
                DELIVERY_STATE_INVALID,
                "只有 SENDING 聚合可以创建触达日志");
        NotifyDeliveryLog log = new NotifyDeliveryLog();
        log.setBizId(bizId);
        log.setEventId(eventId);
        log.setBizType(bizType);
        log.setRefBizId(refBizId);
        log.setNoticeScene(noticeScene);
        log.setRecipientId(recipientId);
        log.setChannel(channel);
        log.setStatus(status);
        return log;
    }

    /** 兼容既有 notify 仓储端口的终态写入载体。 */
    public NotifyResult toLegacyResult() {
        require(isTerminal(status), DELIVERY_STATE_INVALID, "只有终态聚合可以回写触达结果");
        NotifyResult result = new NotifyResult();
        result.setStatus(status);
        result.setProviderMessageId(providerMessageId);
        result.setProviderTemplateId(providerTemplateId);
        result.setErrorCode(errorCode);
        result.setFailReason(failReason);
        return result;
    }

    public String bizId() {
        return bizId;
    }

    public NotifyDeliveryStatus status() {
        return status;
    }

    public String failReason() {
        return failReason;
    }

    /** I1-I5：每次命令完成后校验当前单行快照。 */
    private void checkInvariants() {
        requireSized(bizId, BIZ_ID_MAX, "触达流水号");
        requireSized(eventId, EVENT_ID_MAX, "稳定业务事件标识");
        require(bizType != null, DELIVERY_LOG_INVALID, "业务方向不能为空");
        requireSized(refBizId, REF_BIZ_ID_MAX, "关联业务编号");
        require(noticeScene != null, DELIVERY_LOG_INVALID, "通知场景不能为空");
        requireSized(recipientId, RECIPIENT_ID_MAX, "接收人编号");
        require(channel != null, DELIVERY_LOG_INVALID, "触达渠道不能为空");
        require(status != null, DELIVERY_STATE_INVALID, "触达状态不能为空");

        if (status == NotifyDeliveryStatus.SENDING) {
            require(sendTime == null, DELIVERY_STATE_INVALID, "SENDING 日志不得提前记录结束时间");
            require(providerTemplateId == null && providerMessageId == null
                            && errorCode == null && failReason == null,
                    DELIVERY_STATE_INVALID,
                    "SENDING 日志不得提前记录渠道结果");
            return;
        }

        require(isTerminal(status), DELIVERY_STATE_INVALID, "触达日志包含未知状态");
        require(sendTime != null, DELIVERY_STATE_INVALID, "终态必须记录结束时间");
        if (status == NotifyDeliveryStatus.FAILED || status == NotifyDeliveryStatus.SKIPPED) {
            require(!isBlank(errorCode) || !isBlank(failReason),
                    DELIVERY_STATE_INVALID,
                    "失败或跳过结果必须保存错误或原因摘要");
        }
    }

    private static boolean isTerminal(NotifyDeliveryStatus value) {
        return value == NotifyDeliveryStatus.SENT
                || value == NotifyDeliveryStatus.FAILED
                || value == NotifyDeliveryStatus.SKIPPED;
    }

    private static void requireSized(String value, int maxLength, String label) {
        require(!isBlank(value), DELIVERY_LOG_INVALID, label + "不能为空");
        require(value.length() <= maxLength,
                DELIVERY_LOG_INVALID,
                label + "长度不能超过 " + maxLength);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static void require(boolean condition, String errorIdentifier, String message) {
        if (!condition) {
            throw new DeliveryDomainException(errorIdentifier, message);
        }
    }
}
