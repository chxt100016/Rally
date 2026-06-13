package com.rally.domain.meetup.gateway;

import com.rally.domain.meetup.enums.RegistrationStatusEnum;
import com.rally.domain.meetup.model.RegistrationData;

import java.util.List;

/**
 * 报名/注册表读写网关接口（记录所有参与者：创建者、等待审批、已通过等）
 */
public interface RegistrationGateway {
    /**
     * 保存报名记录（新增或更新）
     */
    void save(RegistrationData data);

    /**
     * 根据 bizId 查询
     */
    RegistrationData findByBizId(String bizId);

    /**
     * 查询用户在某约球的最新有效报名（pending/JOINED）
     */
    RegistrationData findActiveByMeetupAndUser(String meetupId, String userId);


    /**
     * 根据 bizId 更新状态
     */
    void updateStatus(String bizId, RegistrationStatusEnum status);


    /**
     * 统计约球已批准的参与者数量（含创建者）
     */
    int countApprovedByMeetupId(String meetupId);

    /**
     * 查询约球的所有报名记录（加载聚合根用）
     */
    List<RegistrationData> findByMeetupId(String meetupId);

    void toReviewed(String userId);
}
