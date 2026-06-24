package com.rally.domain.notify.gateway;

import com.rally.domain.notify.enums.NoticeScene;
import com.rally.domain.notify.enums.NotifyBizType;
import com.rally.domain.notify.model.NotifyResult;
import com.rally.domain.notify.model.NotifySubscribe;

import java.util.List;

/**
 * 订阅通知流水读写网关
 */
public interface NotifySubscribeRepository {

    /** 批量保存流水（UNUSED） */
    void saveBatch(List<NotifySubscribe> subscribes);

    /** 查询指定业务对象、场景下，目标用户的未使用流水（按 id 升序，最早优先） */
    List<NotifySubscribe> findUnused(NotifyBizType bizType, String refBizId, NoticeScene scene, List<String> userIds);

    /** CAS 占用：UNUSED -> SENDING，成功返回 true */
    boolean casToSending(Long id);

    /** 回写发送结果（SENT/FAILED） */
    void markResult(Long id, NotifyResult result);
}
