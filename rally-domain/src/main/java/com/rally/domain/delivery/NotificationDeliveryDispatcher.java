package com.rally.domain.delivery;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.domain.notify.enums.NotifyChannel;
import com.rally.domain.notify.enums.NotifyDeliveryStatus;
import com.rally.domain.notify.gateway.Notifier;
import com.rally.domain.notify.gateway.NotifyDeliveryLogRepository;
import com.rally.domain.notify.model.NotifyMessage;
import com.rally.domain.notify.model.NotifyResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * C1-C4 的领域编排入口。
 *
 * <p>该类复用既有 {@code com.rally.domain.notify} 渠道和仓储端口，便于渐进迁移；它不是
 * Spring Bean，不会替换或影响当前 {@code NotificationDeliveryService} 的公开签名和注入行为。</p>
 */
@Slf4j
public final class NotificationDeliveryDispatcher {

    private final NotifyDeliveryLogRepository repository;
    private final Map<NotifyChannel, Notifier> notifiers;
    private final DeliveryIdGenerator idGenerator;
    private final Executor executor;

    public NotificationDeliveryDispatcher(
            NotifyDeliveryLogRepository repository,
            List<Notifier> notifiers,
            DeliveryIdGenerator idGenerator,
            Executor executor) {
        this.repository = repository;
        this.notifiers = indexNotifiers(notifiers);
        this.idGenerator = idGenerator;
        this.executor = executor;
    }

    /** 使用既有雪花流水号策略创建兼容调度器。 */
    public NotificationDeliveryDispatcher(
            NotifyDeliveryLogRepository repository, List<Notifier> notifiers, Executor executor) {
        this(repository, notifiers, IdWorker::getIdStr, executor);
    }

    /** C1：去重候选接收人，在核心事务提交后异步拆分接收人与渠道组合。 */
    public void trigger(TriggerNotificationCommand command) {
        if (command == null
                || isBlank(command.eventId())
                || command.recipientIds().isEmpty()
                || command.channels().isEmpty()) {
            return;
        }
        List<String> recipients = new LinkedHashSet<>(command.recipientIds()).stream()
                .filter(id -> !isBlank(id))
                .toList();
        if (recipients.isEmpty()) {
            return;
        }
        scheduleAfterCommit(() -> dispatch(command, recipients));
    }

    private void dispatch(TriggerNotificationCommand command, List<String> recipients) {
        for (String recipientId : recipients) {
            if (!isEligible(command, recipientId)) {
                continue;
            }
            for (NotifyChannel channel : command.channels()) {
                deliver(command, recipientId, channel);
            }
        }
    }

    /** C2-C4：先原子取得执行权，再调用渠道，最后以聚合终态回写结果。 */
    private void deliver(
            TriggerNotificationCommand command, String recipientId, NotifyChannel channel) {
        NotificationDelivery delivery;
        try {
            delivery = NotificationDelivery.acquire(
                    new AcquireDeliveryCommand(
                            command.eventId(),
                            command.bizType(),
                            command.refBizId(),
                            command.noticeScene(),
                            recipientId,
                            channel),
                    idGenerator);
            if (!repository.tryStart(delivery.toLegacyStartLog())) {
                log.info("通知事件已触达或正在触达，跳过重复任务: eventId={}, recipientId={}, channel={}",
                        command.eventId(), recipientId, channel);
                return;
            }
        } catch (Exception e) {
            // I2/I3：唯一冲突或任何日志存储异常都 fail-closed，绝不无日志调用渠道。
            log.error("建立触达日志失败，跳过发送: eventId={}, recipientId={}, channel={}",
                    command.eventId(), recipientId, channel, e);
            return;
        }

        RecordDeliveryResultCommand channelResult = executeChannel(command, recipientId, channel);
        NotificationDelivery completed;
        try {
            completed = delivery.recordResult(channelResult, LocalDateTime.now());
        } catch (Exception e) {
            completed = delivery.recordResult(
                    failed("INVALID_CHANNEL_RESULT", "通知渠道返回了非法结果", null),
                    LocalDateTime.now());
            log.error("通知渠道返回结果不符合触达契约: eventId={}, recipientId={}, channel={}",
                    command.eventId(), recipientId, channel, e);
        }

        try {
            repository.markResult(completed.bizId(), completed.toLegacyResult());
        } catch (Exception e) {
            // I5：更新失败时数据库保持 SENDING，供应用日志人工排查；当前不自动重试。
            log.error("回写触达结果失败: deliveryId={}, eventId={}",
                    completed.bizId(), command.eventId(), e);
            return;
        }
        logResult(command, recipientId, channel, completed);
    }

    /** C3：渠道适配器自行解析地址、模板和字段，并把所有结果收敛为终态。 */
    private RecordDeliveryResultCommand executeChannel(
            TriggerNotificationCommand command, String recipientId, NotifyChannel channel) {
        Notifier notifier = notifiers.get(channel);
        if (notifier == null) {
            return failed("CHANNEL_UNAVAILABLE", "无可用通知渠道:" + channel, null);
        }
        try {
            NotifyResult result = notifier.send(NotifyMessage.of(
                    recipientId,
                    command.refBizId(),
                    command.noticeScene(),
                    command.data()));
            if (result == null) {
                return failed("EMPTY_CHANNEL_RESULT", "通知渠道未返回发送结果", null);
            }
            return new RecordDeliveryResultCommand(
                    result.getStatus(),
                    result.getProviderMessageId(),
                    result.getProviderTemplateId(),
                    result.getErrorCode(),
                    result.getFailReason());
        } catch (Exception e) {
            log.error("通知渠道调用异常: eventId={}, recipientId={}, channel={}",
                    command.eventId(), recipientId, channel, e);
            return failed("CHANNEL_EXCEPTION", safeReason(e), null);
        }
    }

    private boolean isEligible(TriggerNotificationCommand command, String recipientId) {
        if (command.recipientFilter() == null) {
            return true;
        }
        try {
            if (command.recipientFilter().test(recipientId)) {
                return true;
            }
            log.info("接收人不再符合触达条件，跳过通知: scene={}, recipientId={}",
                    command.noticeScene(), recipientId);
            return false;
        } catch (Exception e) {
            log.warn("通知接收人校验异常，按仍可触达处理: scene={}, recipientId={}",
                    command.noticeScene(), recipientId, e);
            return true;
        }
    }

    private void scheduleAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    submit(task);
                }
            });
        } else {
            submit(task);
        }
    }

    private void submit(Runnable task) {
        try {
            executor.execute(task);
        } catch (Exception e) {
            log.error("提交通知异步任务失败", e);
        }
    }

    private void logResult(
            TriggerNotificationCommand command,
            String recipientId,
            NotifyChannel channel,
            NotificationDelivery completed) {
        if (completed.status() == NotifyDeliveryStatus.SENT) {
            log.info("通知触达成功: eventId={}, scene={}, recipientId={}, channel={}",
                    command.eventId(), command.noticeScene(), recipientId, channel);
        } else if (completed.status() == NotifyDeliveryStatus.SKIPPED) {
            log.info("通知不可触达，已跳过: eventId={}, scene={}, recipientId={}, channel={}, reason={}",
                    command.eventId(), command.noticeScene(), recipientId, channel, completed.failReason());
        } else {
            log.warn("通知触达失败: eventId={}, scene={}, recipientId={}, channel={}, reason={}",
                    command.eventId(), command.noticeScene(), recipientId, channel, completed.failReason());
        }
    }

    private static Map<NotifyChannel, Notifier> indexNotifiers(List<Notifier> values) {
        Map<NotifyChannel, Notifier> indexed = new EnumMap<>(NotifyChannel.class);
        if (values != null) {
            for (Notifier notifier : values) {
                if (notifier != null && notifier.channel() != null) {
                    indexed.put(notifier.channel(), notifier);
                }
            }
        }
        return Map.copyOf(indexed);
    }

    private static RecordDeliveryResultCommand failed(
            String errorCode, String reason, String providerTemplateId) {
        return new RecordDeliveryResultCommand(
                NotifyDeliveryStatus.FAILED,
                null,
                providerTemplateId,
                errorCode,
                reason);
    }

    private static String safeReason(Exception exception) {
        return isBlank(exception.getMessage())
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
