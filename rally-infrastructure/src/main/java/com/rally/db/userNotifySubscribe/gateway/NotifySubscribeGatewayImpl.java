package com.rally.db.userNotifySubscribe.gateway;

import com.rally.db.userNotifySubscribe.convert.NotifySubscribeConvertMapper;
import com.rally.db.userNotifySubscribe.entity.UserNotifySubscribePO;
import com.rally.db.userNotifySubscribe.service.UserNotifySubscribeService;
import com.rally.domain.notify.enums.NoticeScene;
import com.rally.domain.notify.enums.NotifyBizType;
import com.rally.domain.notify.enums.NotifySubscribeStatus;
import com.rally.domain.notify.gateway.NotifySubscribeGateway;
import com.rally.domain.notify.model.NotifyResult;
import com.rally.domain.notify.model.NotifySubscribe;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 订阅通知流水网关实现
 */
@Component
@RequiredArgsConstructor
public class NotifySubscribeGatewayImpl implements NotifySubscribeGateway {

    private final UserNotifySubscribeService service;
    private static final NotifySubscribeConvertMapper MAPPER = NotifySubscribeConvertMapper.INSTANCE;

    @Override
    public void saveBatch(List<NotifySubscribe> subscribes) {
        if (subscribes == null || subscribes.isEmpty()) {
            return;
        }
        service.saveBatch(MAPPER.toPOList(subscribes));
    }

    @Override
    public List<NotifySubscribe> findUnused(NotifyBizType bizType, String refBizId, NoticeScene scene, List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<UserNotifySubscribePO> list = service.lambdaQuery()
                .eq(UserNotifySubscribePO::getBizType, bizType.name())
                .eq(UserNotifySubscribePO::getRefBizId, refBizId)
                .eq(UserNotifySubscribePO::getNoticeScene, scene.name())
                .in(UserNotifySubscribePO::getUserId, userIds)
                .eq(UserNotifySubscribePO::getStatus, NotifySubscribeStatus.UNUSED.name())
                .orderByAsc(UserNotifySubscribePO::getId)
                .list();
        return MAPPER.toDataList(list);
    }

    @Override
    public boolean casToSending(Long id) {
        return service.lambdaUpdate()
                .eq(UserNotifySubscribePO::getId, id)
                .eq(UserNotifySubscribePO::getStatus, NotifySubscribeStatus.UNUSED.name())
                .set(UserNotifySubscribePO::getStatus, NotifySubscribeStatus.SENDING.name())
                .update();
    }

    @Override
    public void markResult(Long id, NotifyResult result) {
        NotifySubscribeStatus status = result.isSuccess() ? NotifySubscribeStatus.SENT : NotifySubscribeStatus.FAILED;
        service.lambdaUpdate()
                .eq(UserNotifySubscribePO::getId, id)
                .set(UserNotifySubscribePO::getStatus, status.name())
                .set(UserNotifySubscribePO::getFailReason, result.getFailReason())
                .set(UserNotifySubscribePO::getSendTime, LocalDateTime.now())
                .update();
    }
}
