package com.rally.domain.meetup.service;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.meetup.enums.MeetupStatusEnum;
import com.rally.domain.meetup.gateway.MeetupGateway;
import com.rally.domain.meetup.gateway.RegistrationGateway;
import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.model.MeetupPublishCmd;
import com.rally.domain.system.CityConfig;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.system.enums.SystemConfigKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 约球断言服务（收纳编辑/发布等场景的校验逻辑）
 */
@Service
@RequiredArgsConstructor
public class MeetupPolicy {

    private final MeetupGateway meetupGateway;

    private final RegistrationGateway registrationGateway;

    /**
     * 发布前校验：发布上限 + 城市开通 + 字段校验
     */
    public void assertPublish(String userId, MeetupPublishCmd cmd) {
        // 1. 当日发布上限
        assertTimes(userId);
        // 2. 城市开通校验
        CityConfig.assertCityOpened(cmd.getCityCode());
        // 3. 字段校验
        assertParam(cmd);
    }

    /**
     * 校验当日发布上限
     */
    private void assertTimes(String userId) {
        int publishLimit = SystemConfig.getInt(SystemConfigKey.ANTI_ABUSE_PUBLISH_PER_DAY_LIMIT.getKey(), Integer.parseInt(SystemConfigKey.ANTI_ABUSE_PUBLISH_PER_DAY_LIMIT.getDefaultValue()));
        long todayCount = meetupGateway.countTodayActive(userId);
        if (todayCount >= publishLimit) {
            throw new BusinessException(BizErrorCode.PUBLISH_LIMIT_EXCEEDED);
        }
    }

    /**
     * 校验发布参数
     */
    private void assertParam(MeetupPublishCmd cmd) {
        // 开始时间必须大于当前时间
        if (cmd.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, "不能发布过去的约球");
        }

        // duration 校验
        BigDecimal[] validDurations = {
                new BigDecimal("0.5"), new BigDecimal("1.0"), new BigDecimal("1.5"),
                new BigDecimal("2.0"), new BigDecimal("2.5"), new BigDecimal("3.0")
        };
        boolean validDuration = false;
        for (BigDecimal d : validDurations) {
            if (d.compareTo(cmd.getDuration()) == 0) {
                validDuration = true;
                break;
            }
        }
        if (!validDuration) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, "持续时长必须是0.5的倍数");
        }

        // level 校验：按模式校验必填 + 步长 0.5
        if (cmd.getLevelMode() != null) {
            switch (cmd.getLevelMode()) {
                case RANGE:
                    if (cmd.getLevelMin() == null) throw new BusinessException(BizErrorCode.PARAM_ERROR, "请填写水平最小值");
                    if (cmd.getLevelMax() == null) throw new BusinessException(BizErrorCode.PARAM_ERROR, "请填写水平最大值");
                    validateLevelStep(cmd.getLevelMin(), "水平最小值");
                    validateLevelStep(cmd.getLevelMax(), "水平最大值");
                    if (cmd.getLevelMin().compareTo(cmd.getLevelMax()) > 0) {
                        throw new BusinessException(BizErrorCode.PARAM_ERROR, "水平最小值不能大于最大值");
                    }
                    break;
                case EXACT:
                    if (cmd.getLevelMin() == null) throw new BusinessException(BizErrorCode.PARAM_ERROR, "请填写水平值");
                    validateLevelStep(cmd.getLevelMin(), "水平值");
                    // EXACT 模式：levelMax 未传时默认等于 levelMin
                    if (cmd.getLevelMax() == null) cmd.setLevelMax(cmd.getLevelMin());
                    break;
                case ABOVE:
                    if (cmd.getLevelMin() == null) throw new BusinessException(BizErrorCode.PARAM_ERROR, "请填写水平最小值");
                    validateLevelStep(cmd.getLevelMin(), "水平最小值");
                    break;
                case BELOW:
                    if (cmd.getLevelMax() == null) throw new BusinessException(BizErrorCode.PARAM_ERROR, "请填写水平最大值");
                    validateLevelStep(cmd.getLevelMax(), "水平最大值");
                    break;
            }
        }
    }

    /**
     * 校验水平值步长：必须是 0.5 的倍数
     */
    private void validateLevelStep(BigDecimal level, String fieldName) {
        if (level.multiply(new BigDecimal("10")).remainder(new BigDecimal("5")).compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, fieldName + "步长为0.5");
        }
    }

    /**
     * 断言可以关闭（权限 + 状态校验）
     * @param userId 当前用户
     * @param meetup 聚合根
     */
    public void assertClose(String userId, Meetup meetup) {
        if (!meetup.isCreator(userId)) {
            throw new BusinessException(BizErrorCode.NOT_CREATOR);
        }
        MeetupStatusEnum realStatus = meetup.getRealStatus();
        if (realStatus == MeetupStatusEnum.FINISHED || realStatus == MeetupStatusEnum.CLOSED) {
            throw new BusinessException(BizErrorCode.MEETUP_STATUS_ILLEGAL);
        }
    }

    /**
     * 断言可以编辑（权限 + 状态 + 参数校验）
     * @param meetup 聚合根
     * @param cmd 编辑命令
     */
    public void assertEdit(Meetup meetup, MeetupPublishCmd cmd) {
        MeetupData data = meetup.getData();
        int lockMinutes = SystemConfig.getInt(SystemConfigKey.MEETUP_EDIT_LOCK_MINUTES_BEFORE_START.getKey(), Integer.parseInt(SystemConfigKey.MEETUP_EDIT_LOCK_MINUTES_BEFORE_START.getDefaultValue()));

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
            boolean locationChanged = meetup.isLocationChanged(cmd);

            if (timeChanged || durationChanged || locationChanged) {
                throw new BusinessException(BizErrorCode.LOCATION_TIME_CHANGE_FORBIDDEN);
            }
        }
    }




}
