package com.rally.domain.meetup.service;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.meetup.gateway.RegistrationGateway;
import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.model.PublishCmd;
import com.rally.domain.system.SystemConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 约球断言服务（收纳编辑/发布等场景的校验逻辑）
 */
@Service
@RequiredArgsConstructor
public class MeetupAssertService {

    private final RegistrationGateway registrationGateway;

    /**
     * 断言可以编辑（权限 + 状态 + 参数校验）
     * @param meetup 聚合根
     * @param cmd 编辑命令
     */
    public void assertEdit(Meetup meetup, PublishCmd cmd) {
        MeetupData data = meetup.getData();
        int lockMinutes = SystemConfig.getInt("meetup.edit_lock_minutes_before_start", 60);

        // 1. 权限和状态校验
        if (!meetup.canEdit(data.getCreatorId(), lockMinutes)) {
            throw new BusinessException(BizErrorCode.MEETUP_STATUS_ILLEGAL);
        }

        // 2. 城市不可修改
        if (cmd.getCityCode() != null && !cmd.getCityCode().equals(data.getCityCode())) {
            throw new BusinessException(BizErrorCode.CITY_CHANGE_FORBIDDEN);
        }

        // 3. 已有参与者时，不可修改时间、地点、持续时长
        int participantCount = registrationGateway.countApprovedByMeetupId(data.getBizId());
        if (participantCount > 1) {
            boolean timeChanged = cmd.getStartTime() != null
                    && !cmd.getStartTime().equals(data.getStartTime());
            boolean durationChanged = cmd.getDuration() != null
                    && cmd.getDuration().compareTo(data.getDuration()) != 0;
            boolean locationChanged = isLocationChanged(data, cmd);

            if (timeChanged || durationChanged || locationChanged) {
                throw new BusinessException(BizErrorCode.LOCATION_TIME_CHANGE_FORBIDDEN);
            }
        }
    }

    /**
     * 判断场地是否变更
     * @param data 约球数据
     * @param cmd 编辑命令
     * @return true 表示场地变更
     */
    public boolean isLocationChanged(MeetupData data, PublishCmd cmd) {
        // 场地名称变更
        if (cmd.getCourtName() != null && !cmd.getCourtName().equals(data.getCourtName())) {
            return true;
        }
        // 场地地址变更
        if (cmd.getCourtAddress() != null && !cmd.getCourtAddress().equals(data.getCourtAddress())) {
            return true;
        }
        // 经纬度变更
        if (cmd.getCourtLng() != null && cmd.getCourtLat() != null) {
            if (!cmd.getCourtLng().equals(data.getCourtLng()) || !cmd.getCourtLat().equals(data.getCourtLat())) {
                return true;
            }
        }
        return false;
    }
}
