package com.rally.domain.notify.service;

import com.rally.domain.notify.enums.NoticeScene;
import com.rally.domain.notify.enums.NotifyBizType;
import com.rally.domain.notify.enums.NotifyChannel;
import com.rally.domain.notify.gateway.Notifier;
import com.rally.domain.notify.gateway.NotifySubscribeGateway;
import com.rally.domain.notify.model.NotifyMessage;
import com.rally.domain.notify.model.NotifyResult;
import com.rally.domain.notify.model.NotifySubscribe;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/**
 * 订阅通知领域服务
 * <p>
 * grant：订阅授权成功后建流水（UNUSED）。
 * notify：触发通知，事务提交后用线程池异步发送，立即返回。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifySubscribeService {

    private final NotifySubscribeGateway gateway;
    private final List<Notifier> notifiers;

    private final Map<NotifyChannel, Notifier> notifierMap = new EnumMap<>(NotifyChannel.class);

    private final ExecutorService executor = new ThreadPoolExecutor(2, 4, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1024), newThreadFactory(), new ThreadPoolExecutor.CallerRunsPolicy());

    @PostConstruct
    public void init() {
        for (Notifier notifier : notifiers) {
            notifierMap.put(notifier.channel(), notifier);
        }
    }

    /**
     * 订阅授权成功后建流水（UNUSED）。授权了几个场景就建几条。
     */
    public void grant(String userId, NotifyBizType bizType, String refBizId, List<NoticeScene> scenes) {
        if (scenes == null || scenes.isEmpty()) {
            return;
        }
        // 通知额度建立失败不应影响用户主流程，吞掉异常仅记日志
        try {
            List<NotifySubscribe> list = scenes.stream().filter(java.util.Objects::nonNull).map(scene -> NotifySubscribe.granted(userId, bizType, refBizId, scene)).toList();
            if (!list.isEmpty()) {
                gateway.saveBatch(list);
            }
        } catch (Exception e) {
            log.error("建立订阅通知额度失败: userId={}, bizType={}, refBizId={}, scenes={}", userId, bizType, refBizId, scenes, e);
        }
    }

    /**
     * 触发通知：查未使用流水 -> 事务提交后线程池异步发送，每个用户消费最早一条。
     */
    public void notify(NotifyBizType bizType, String refBizId, NoticeScene scene, List<String> userIds, Map<String, Object> data) {
        notify(bizType, refBizId, scene, userIds, data, null);
    }

    /**
     * 触发通知，并在发送时（异步、事务提交后）对每个接收人做成员校验，已退出活动的不再发送。
     * @param recipientFilter 接收人校验，返回 false 则跳过该用户；为 null 时不校验
     */
    public void notify(NotifyBizType bizType, String refBizId, NoticeScene scene, List<String> userIds, Map<String, Object> data, Predicate<String> recipientFilter) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        // 通知触发失败不应影响用户主流程，吞掉异常仅记日志
        try {
            afterCommit(() -> doNotify(bizType, refBizId, scene, userIds, data, recipientFilter));
        } catch (Exception e) {
            log.error("触发订阅通知失败: bizType={}, refBizId={}, scene={}, userIds={}", bizType, refBizId, scene, userIds, e);
        }
    }

    private void doNotify(NotifyBizType bizType, String refBizId, NoticeScene scene, List<String> userIds, Map<String, Object> data, Predicate<String> recipientFilter) {
        List<NotifySubscribe> list;
        try {
            list = gateway.findUnused(bizType, refBizId, scene, userIds);
        } catch (Exception e) {
            log.error("查询待发订阅流水失败: bizType={}, refBizId={}, scene={}", bizType, refBizId, scene, e);
            return;
        }
        // 无可发送额度（前端未授权该场景模板，或额度已被消费）—— 这是“没收到通知”的最常见原因
        if (list.isEmpty()) {
            log.info("订阅通知无可用额度，跳过发送: scene={}, refBizId={}, userIds={}", scene, refBizId, userIds);
            return;
        }
        for (NotifySubscribe subscribe : list) {
            try {
                // 发送时成员校验：用户已退出活动则跳过（保留 UNUSED，不发送、不标记失败）
                if (!shouldSend(recipientFilter, subscribe.getUserId(), scene)) {
                    continue;
                }
                if (!gateway.casToSending(subscribe.getId())) {
                    continue;
                }
                Notifier notifier = notifierMap.get(scene.getChannel());
                if (notifier == null) {
                    gateway.markResult(subscribe.getId(), NotifyResult.fail("无可用通知渠道:" + scene.getChannel()));
                    continue;
                }
                NotifyResult result = notifier.send(NotifyMessage.of(subscribe, data));
                gateway.markResult(subscribe.getId(), result);
                if (result.isSuccess()) {
                    log.info("订阅通知发送成功: scene={}, userId={}, refBizId={}", scene, subscribe.getUserId(), refBizId);
                } else {
                    log.warn("订阅通知发送失败: scene={}, userId={}, refBizId={}, reason={}", scene, subscribe.getUserId(), refBizId, result.getFailReason());
                }
            } catch (Exception e) {
                log.error("发送订阅通知异常: id={}, scene={}", subscribe.getId(), scene, e);
                gateway.markResult(subscribe.getId(), NotifyResult.fail(e.getMessage()));
            }
        }
    }

    /** 成员校验：无校验器视为可发；校验异常按可发处理（fail-open，不漏发），仅退出用户跳过 */
    private boolean shouldSend(Predicate<String> recipientFilter, String userId, NoticeScene scene) {
        if (recipientFilter == null) {
            return true;
        }
        try {
            if (recipientFilter.test(userId)) {
                return true;
            }
            log.info("接收人已退出活动，跳过通知: scene={}, userId={}", scene, userId);
            return false;
        } catch (Exception e) {
            log.warn("通知成员校验异常，按仍在活动处理: scene={}, userId={}", scene, userId, e);
            return true;
        }
    }

    /** 存在事务时提交后再异步发送，避免读到未提交的流水；否则直接异步发送。 */
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
            Thread thread = new Thread(runnable, "notify-sender-" + seq.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
    }
}
