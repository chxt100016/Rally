package com.rally.domain.notify.service;

import com.rally.domain.notify.enums.NoticeScene;
import com.rally.domain.notify.enums.NotifyBizType;
import com.rally.domain.notify.enums.NotifyChannel;
import com.rally.domain.notify.enums.NotifyDeliveryStatus;
import com.rally.domain.notify.gateway.Notifier;
import com.rally.domain.notify.gateway.NotifyDeliveryLogRepository;
import com.rally.domain.notify.model.NotifyDeliveryLog;
import com.rally.domain.notify.model.NotifyMessage;
import com.rally.domain.notify.model.NotifyResult;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/**
 * 渠道无关的触达服务。
 * <p>
 * 核心事务提交后，以 eventId + recipientId + channel 建立唯一触达日志，
 * 只有首次建立成功的任务会调用渠道。未订阅等预期不可触达结果记为 SKIPPED。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDeliveryService {

    private static final Set<NotifyChannel> DEFAULT_CHANNELS = EnumSet.of(NotifyChannel.WECHAT_SUBSCRIBE);

    private final NotifyDeliveryLogRepository deliveryLogRepository;
    private final List<Notifier> notifiers;
    private final Map<NotifyChannel, Notifier> notifierMap = new EnumMap<>(NotifyChannel.class);
    private final ExecutorService executor = new ThreadPoolExecutor(2, 4, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1024), newThreadFactory(), new ThreadPoolExecutor.CallerRunsPolicy());

    @PostConstruct
    public void init() {
        for (Notifier notifier : notifiers) {
            notifierMap.put(notifier.channel(), notifier);
        }
    }

    public void notify(String eventId, NotifyBizType bizType, String refBizId, NoticeScene scene,
                       List<String> recipientIds, Map<String, Object> data) {
        notify(eventId, bizType, refBizId, scene, recipientIds, data, null, DEFAULT_CHANNELS);
    }

    public void notify(String eventId, NotifyBizType bizType, String refBizId, NoticeScene scene,
                       List<String> recipientIds, Map<String, Object> data, Predicate<String> recipientFilter) {
        notify(eventId, bizType, refBizId, scene, recipientIds, data, recipientFilter, DEFAULT_CHANNELS);
    }

    /**
     * 触发指定渠道的通知。每个接收人与渠道独立幂等、独立记录结果。
     */
    public void notify(String eventId, NotifyBizType bizType, String refBizId, NoticeScene scene,
                       List<String> recipientIds, Map<String, Object> data,
                       Predicate<String> recipientFilter, Set<NotifyChannel> channels) {
        if (eventId == null || eventId.isBlank() || recipientIds == null || recipientIds.isEmpty()
                || channels == null || channels.isEmpty()) {
            return;
        }
        try {
            List<String> recipients = new LinkedHashSet<>(recipientIds).stream()
                    .filter(id -> id != null && !id.isBlank())
                    .toList();
            Set<NotifyChannel> selectedChannels = EnumSet.copyOf(channels);
            afterCommit(() -> doNotify(eventId, bizType, refBizId, scene, recipients, data,
                    recipientFilter, selectedChannels));
        } catch (Exception e) {
            log.error("触发通知失败: eventId={}, bizType={}, refBizId={}, scene={}",
                    eventId, bizType, refBizId, scene, e);
        }
    }

    private void doNotify(String eventId, NotifyBizType bizType, String refBizId, NoticeScene scene,
                          List<String> recipientIds, Map<String, Object> data,
                          Predicate<String> recipientFilter, Set<NotifyChannel> channels) {
        for (String recipientId : recipientIds) {
            if (!shouldSend(recipientFilter, recipientId, scene)) {
                continue;
            }
            for (NotifyChannel channel : channels) {
                deliver(eventId, bizType, refBizId, scene, recipientId, data, channel);
            }
        }
    }

    private void deliver(String eventId, NotifyBizType bizType, String refBizId, NoticeScene scene,
                         String recipientId, Map<String, Object> data, NotifyChannel channel) {
        NotifyDeliveryLog deliveryLog = NotifyDeliveryLog.sending(
                eventId, bizType, refBizId, scene, recipientId, channel);
        try {
            if (!deliveryLogRepository.tryStart(deliveryLog)) {
                log.info("通知事件已触达或正在触达，跳过重复任务: eventId={}, recipientId={}, channel={}",
                        eventId, recipientId, channel);
                return;
            }
        } catch (Exception e) {
            // 日志是幂等依据；无法建立时 fail-closed，避免失去去重保护后重复触达。
            log.error("建立触达日志失败，跳过发送: eventId={}, recipientId={}, channel={}",
                    eventId, recipientId, channel, e);
            return;
        }

        NotifyResult result;
        Notifier notifier = notifierMap.get(channel);
        if (notifier == null) {
            result = NotifyResult.failed("CHANNEL_UNAVAILABLE", "无可用通知渠道:" + channel, null);
        } else {
            try {
                result = notifier.send(NotifyMessage.of(recipientId, refBizId, scene, data));
                if (result == null) {
                    result = NotifyResult.failed("EMPTY_CHANNEL_RESULT", "通知渠道未返回发送结果", null);
                }
            } catch (Exception e) {
                log.error("通知渠道调用异常: eventId={}, recipientId={}, channel={}",
                        eventId, recipientId, channel, e);
                result = NotifyResult.failed("CHANNEL_EXCEPTION", e.getMessage(), null);
            }
        }

        try {
            deliveryLogRepository.markResult(deliveryLog.getBizId(), result);
        } catch (Exception e) {
            log.error("回写触达结果失败: deliveryId={}, eventId={}", deliveryLog.getBizId(), eventId, e);
        }

        if (result.getStatus() == NotifyDeliveryStatus.SENT) {
            log.info("通知触达成功: eventId={}, scene={}, recipientId={}, channel={}",
                    eventId, scene, recipientId, channel);
        } else if (result.getStatus() == NotifyDeliveryStatus.SKIPPED) {
            log.info("通知不可触达，已跳过: eventId={}, scene={}, recipientId={}, channel={}, reason={}",
                    eventId, scene, recipientId, channel, result.getFailReason());
        } else {
            log.warn("通知触达失败: eventId={}, scene={}, recipientId={}, channel={}, reason={}",
                    eventId, scene, recipientId, channel, result.getFailReason());
        }
    }

    private boolean shouldSend(Predicate<String> recipientFilter, String recipientId, NoticeScene scene) {
        if (recipientFilter == null) {
            return true;
        }
        try {
            if (recipientFilter.test(recipientId)) {
                return true;
            }
            log.info("接收人不再符合触达条件，跳过通知: scene={}, recipientId={}", scene, recipientId);
            return false;
        } catch (Exception e) {
            log.warn("通知接收人校验异常，按仍可触达处理: scene={}, recipientId={}", scene, recipientId, e);
            return true;
        }
    }

    private void afterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    executor.submit(task);
                }
            });
        } else {
            executor.submit(task);
        }
    }

    private static java.util.concurrent.ThreadFactory newThreadFactory() {
        AtomicInteger seq = new AtomicInteger(1);
        return runnable -> {
            Thread thread = new Thread(runnable, "notification-delivery-" + seq.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
    }
}
