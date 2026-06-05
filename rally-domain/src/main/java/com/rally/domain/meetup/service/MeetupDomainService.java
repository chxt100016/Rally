package com.rally.domain.meetup.service;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.meetup.enums.*;
import com.rally.domain.meetup.gateway.MeetupGateway;
import com.rally.domain.meetup.gateway.NearbyGateway;
import com.rally.domain.meetup.gateway.RegistrationGateway;
import com.rally.domain.meetup.convert.MeetupDomainConvertMapper;
import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.model.MeetupFactory;
import com.rally.domain.meetup.model.PublishCmd;
import com.rally.domain.meetup.model.RegistrationData;
import com.rally.domain.system.CityLocator;
import com.rally.domain.system.SystemConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.time.LocalDateTime;

/**
 * 约球领域服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MeetupDomainService {

    private final MeetupGateway meetupGateway;

    private final NearbyGateway nearbyGateway;

    private final RegistrationGateway registrationGateway;

    /**
     * 获取约球聚合根
     * @param meetupId 约球ID
     * @return Meetup 聚合根
     */
    public Meetup getMeetup(String meetupId) {
        MeetupData data = meetupGateway.findByBizId(meetupId);
        if (data == null) {
            throw new BusinessException(BizErrorCode.MEETUP_NOT_FOUND);
        }
        return new Meetup(data);
    }

    /**
     * 编辑约球（更新字段 + 保存）
     * @param data 约球数据
     * @param cmd 编辑命令
     */
    public void edit(MeetupData data, PublishCmd cmd) {
        // 1. 更新字段（MapStruct）
        MeetupDomainConvertMapper.INSTANCE.updateMeetupData(data, cmd);
        // 2. 保存
        meetupGateway.save(data);
    }

    /**
     * 断言
     */
    public void assertPublish(String userId, PublishCmd cmd) {

        // 1. 当日发布上限
        this.assertTimes(userId);
        // 2. 城市开通校验
        CityLocator.assertCityOpened(cmd.getCityCode());
        // 3. 字段校验
        this.assertParam(cmd);

    }


    /**
     * 判断场地是否变更（供 app 层 GEO 更新判断使用）
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

    private void assertTimes(String userId) {
        int publishLimit = SystemConfig.getInt("anti_abuse.publish_per_day_limit", 5);
        long todayCount = meetupGateway.countTodayActive(userId);
        if (todayCount >= publishLimit) {
            throw new BusinessException(BizErrorCode.PUBLISH_LIMIT_EXCEEDED);
        }
    }

    /**
     * 校验发布参数
     */
    private void assertParam(PublishCmd cmd) {
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

        // level 校验：1.5–7.0，步长 0.5
        if (cmd.getLevelMode() != null) {
            if (cmd.getLevelValue() == null || cmd.getLevelValue().isBlank()) {
                throw new BusinessException(BizErrorCode.PARAM_ERROR, "请填写水平值");
            }
            if (cmd.getLevelMode() == LevelModeEnum.RANGE) {
                String[] parts = cmd.getLevelValue().split(":");
                if (parts.length != 2) {
                    throw new BusinessException(BizErrorCode.PARAM_ERROR, "水平范围格式应为 min:max");
                }
                BigDecimal min = parseLevel(parts[0], "水平最小值");
                BigDecimal max = parseLevel(parts[1], "水平最大值");
                if (min.compareTo(max) > 0) {
                    throw new BusinessException(BizErrorCode.PARAM_ERROR, "水平最小值不能大于最大值");
                }
            } else {
                parseLevel(cmd.getLevelValue(), "水平值");
            }
        }
    }

    /**
     * 校验单个水平值：1.5–7.0，步长 0.5
     */
    private BigDecimal parseLevel(String value, String fieldName) {
        BigDecimal level;
        try {
            level = new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, fieldName + "格式不正确");
        }
        if (level.compareTo(new BigDecimal("1.5")) < 0 || level.compareTo(new BigDecimal("7.0")) > 0) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, fieldName + "范围为1.5~7.0");
        }
        if (level.multiply(new BigDecimal("10")).remainder(new BigDecimal("5")).compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, fieldName + "步长为0.5");
        }
        return level;
    }

    /**
     * 构建约球聚合根（含创建者报名）并一次性持久化
     */
    public void add(String userId, PublishCmd cmd) {
        // 1. 通过聚合根工厂创建（自动将创建者加入报名表）
        Meetup meetup = MeetupFactory.create(cmd, userId);

        // 2. 一次性持久化（约球主表 + 报名记录）
        meetupGateway.save(meetup);

        // 3. GEO 写入
        nearbyGateway.add(cmd.getCityCode(), meetup.getData().getBizId(), cmd.getCourtLng(), cmd.getCourtLat());
    }



    /**
     * 计算关闭约球的阶梯扣分
     */
    public int calculateCancelPenalty(LocalDateTime startTime, int penalty24h, int penalty12h, int penalty6h, int penaltyUnder6h) {
        long hoursUntilStart = Duration.between(LocalDateTime.now(), startTime).toHours();

        if (hoursUntilStart >= 24) {
            return penalty24h;
        } else if (hoursUntilStart >= 12) {
            return penalty12h;
        } else if (hoursUntilStart >= 6) {
            return penalty6h;
        } else {
            return penaltyUnder6h;
        }
    }

    /**
     * 计算操作状态
     */
    public ActionStateEnum calculateActionState(Meetup meetup, String currentUserId, int lockMinutes) {
        MeetupStatusEnum realStatus = meetup.getRealStatus();
        boolean isCreator = meetup.isCreator(currentUserId);

        // 终态判断
        if (realStatus == MeetupStatusEnum.FINISHED || realStatus == MeetupStatusEnum.CLOSED) {
            return isCreator ? ActionStateEnum.OWNER_DISABLED : ActionStateEnum.DISABLED;
        }

        // 创建人视角
        if (isCreator) {
            boolean locked = LocalDateTime.now().isAfter(meetup.getData().getStartTime().minusMinutes(lockMinutes));
            return locked ? ActionStateEnum.OWNER_EDIT_LOCKED : ActionStateEnum.OWNER_EDITABLE;
        }

        // 访客视角
        if (realStatus == MeetupStatusEnum.FULL) {
            return ActionStateEnum.FULL;
        }
        return meetup.getData().getJoinMode() == JoinModeEnum.DIRECT
                ? ActionStateEnum.JOIN_DIRECT
                : ActionStateEnum.APPLY_APPROVAL;
    }

    /**
     * 计算每人费用
     */
    public Integer calculatePerPersonCost(MeetupData data) {
        if (data.getCostItems() == null || data.getCostItems().isEmpty() || data.getMaxPlayers() == null) {
            return null;
        }
        int totalAmount = data.getCostItems().stream()
                .mapToInt(item -> item.getTotalAmount() != null ? item.getTotalAmount() : 0)
                .sum();
        return (int) Math.ceil((double) totalAmount / data.getMaxPlayers());
    }

    /**
     * 计算退出是否扣分
     */
    public boolean calculateQuitWillPenalize(MeetupData data, String currentUserId) {
        if (currentUserId.equals(data.getCreatorId())) {
            return false;
        }
        long hoursUntilStart = Duration.between(LocalDateTime.now(), data.getStartTime()).toHours();
        return hoursUntilStart < 6;
    }

    // ======================== 报名领域逻辑（需要 Gateway 的部分） ========================

    /**
     * 信誉分门槛校验
     * @param reputationScore 用户信誉分，可为 null
     */
    public void checkReputationScore(BigDecimal reputationScore) {
        if (reputationScore == null) {
            return;
        }
        BigDecimal threshold = new BigDecimal(SystemConfig.getString("meetup.join.min_reputation_score", "30"));
        if (reputationScore.compareTo(threshold) < 0) {
            throw new BusinessException(BizErrorCode.LOW_REPUTATION_BANNED);
        }
    }

    /**
     * 报名时间冲突检测
     */
    public void checkTimeConflict(String userId, LocalDateTime startTime, LocalDateTime endTime,
                                  String excludeMeetupId) {
        int bufferMinutes = SystemConfig.getInt("meetup.conflict.buffer_minutes", 30);
        LocalDateTime conflictStart = startTime.minusMinutes(bufferMinutes);
        LocalDateTime conflictEnd = endTime.plusMinutes(bufferMinutes);

        List<RegistrationData> conflicts = registrationGateway.findConflict(
                userId, conflictStart, conflictEnd, excludeMeetupId);

        if (!conflicts.isEmpty()) {
            throw new BusinessException(BizErrorCode.TIME_CONFLICT);
        }
    }

    /**
     * 水平匹配判断（纯领域算法）
     * @param data 约球数据
     * @param userLevelMin 用户水平最小值，可为 null
     * @param userLevelMax 用户水平最大值，可为 null
     * @return true 表示匹配
     */
    public boolean matchLevel(MeetupData data, BigDecimal userLevelMin, BigDecimal userLevelMax) {
        if (data.getLevelMode() == null || data.getLevelValue() == null) {
            return true;
        }

        BigDecimal meetupMin, meetupMax;
        switch (data.getLevelMode()) {
            case RANGE:
                String[] parts = data.getLevelValue().split(":");
                meetupMin = new BigDecimal(parts[0]);
                meetupMax = new BigDecimal(parts[1]);
                break;
            case EXACT:
                meetupMin = meetupMax = new BigDecimal(data.getLevelValue());
                break;
            case ABOVE:
                meetupMin = new BigDecimal(data.getLevelValue());
                meetupMax = new BigDecimal("7.0");
                break;
            case BELOW:
                meetupMin = new BigDecimal("1.5");
                meetupMax = new BigDecimal(data.getLevelValue());
                break;
            default:
                return true;
        }

        BigDecimal userMin = userLevelMin != null ? userLevelMin : new BigDecimal("1.5");
        BigDecimal userMax = userLevelMax != null ? userLevelMax : new BigDecimal("7.0");
        return userMin.compareTo(meetupMax) <= 0 && userMax.compareTo(meetupMin) >= 0;
    }

    /**
     * 计算操作状态（含报名记录上下文，用于详情页等需要区分 PENDING_REVIEW/JOINED 的场景）
     * @param userRegistration 用户在约球的报名记录，可为 null
     */
    public ActionStateEnum calculateActionState(Meetup meetup, String currentUserId, int lockMinutes,
                                                RegistrationData userRegistration) {
        MeetupStatusEnum realStatus = meetup.getRealStatus();
        boolean isCreator = meetup.isCreator(currentUserId);

        // 终态判断
        if (realStatus == MeetupStatusEnum.FINISHED || realStatus == MeetupStatusEnum.CLOSED) {
            return isCreator ? ActionStateEnum.OWNER_DISABLED : ActionStateEnum.DISABLED;
        }

        // 创建人视角
        if (isCreator) {
            boolean locked = LocalDateTime.now().isAfter(meetup.getData().getStartTime().minusMinutes(lockMinutes));
            return locked ? ActionStateEnum.OWNER_EDIT_LOCKED : ActionStateEnum.OWNER_EDITABLE;
        }

        // 访客视角：含报名记录上下文
        if (userRegistration != null) {
            if (userRegistration.getStatus() == RegistrationStatusEnum.pending) {
                return ActionStateEnum.PENDING_REVIEW;
            }
            if (userRegistration.getStatus() == RegistrationStatusEnum.approved) {
                return ActionStateEnum.JOINED;
            }
        }

        // 满员判断
        if (realStatus == MeetupStatusEnum.FULL) {
            return ActionStateEnum.FULL;
        }

        return meetup.getData().getJoinMode() == JoinModeEnum.DIRECT
                ? ActionStateEnum.JOIN_DIRECT : ActionStateEnum.APPLY_APPROVAL;
    }

    /**
     * 计算退出扣分
     * @return 扣分值，0 表示不扣分
     */
    public int calculateQuitPenalty(MeetupData data, String userId) {
        // 创建人退出不扣分
        if (userId.equals(data.getCreatorId())) {
            return 0;
        }
        long hoursUntilStart = Duration.between(LocalDateTime.now(), data.getStartTime()).toHours();
        int thresholdHours = SystemConfig.getInt("meetup.quit.penalty_threshold_hours", 6);
        if (hoursUntilStart < thresholdHours) {
            return SystemConfig.getInt("meetup.quit.penalty_under_6h", 25);
        }
        return 0;
    }

    /**
     * 计算结束时间
     */
    private LocalDateTime calculateEndTime(LocalDateTime startTime, BigDecimal duration) {
        return startTime.plusHours(duration.longValue())
                .plusMinutes((duration.remainder(BigDecimal.ONE).multiply(new BigDecimal("60"))).longValue());
    }
}
