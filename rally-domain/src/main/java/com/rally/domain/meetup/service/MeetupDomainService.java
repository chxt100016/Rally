package com.rally.domain.meetup.service;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.meetup.convert.MeetupDomainConvertMapper;
import com.rally.domain.meetup.enums.ActionStateEnum;
import com.rally.domain.meetup.enums.JoinModeEnum;
import com.rally.domain.meetup.enums.MeetupStatusEnum;
import com.rally.domain.meetup.enums.RegistrationStatusEnum;
import com.rally.domain.meetup.gateway.MeetupGateway;
import com.rally.domain.meetup.gateway.NearbyGateway;
import com.rally.domain.meetup.gateway.RegistrationGateway;
import com.rally.domain.meetup.model.*;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

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

    private final MeetupPolicy meetupPolicy;

    /**
     * 获取约球聚合根（含全部报名记录，适用于报名/审批等场景）
     * @param meetupId 约球ID
     * @return Meetup 聚合根（含全部报名记录）
     */
    public Meetup get(String meetupId) {
        MeetupData data = meetupGateway.findByBizId(meetupId);
        Assert.notNull(data, BizErrorCode.MEETUP_NOT_FOUND);
        List<RegistrationData> registrations = registrationGateway.findByMeetupId(meetupId);
        return new Meetup(data, registrations);
    }

    /**
     * 编辑约球（更新字段 + 保存）
     * @param cmd 编辑命令
     */
    public void edit(String userId, Meetup meetup, MeetupEditCmd cmd) {
        meetup.assertOwner(userId);

        // 1. 更新字段（MapStruct）
        MeetupDomainConvertMapper.INSTANCE.updateMeetupData(meetup.getData(), cmd);

        // 2. 保存
        meetupGateway.save(meetup.getData());
    }

    /**
     * 构建约球聚合根（含创建者报名）并一次性持久化
     */
    public void add(String userId, MeetupPublishCmd cmd) {
        // 1. 通过聚合根工厂创建（自动将创建者加入报名表）
        Meetup meetup = MeetupFactory.create(cmd, userId);

        // 2. 一次性持久化（约球主表 + 报名记录）
        meetupGateway.save(meetup);

        // 3. GEO 写入
        nearbyGateway.add(cmd.getCityCode(), meetup.getData().getBizId(), cmd.getCourtLng(), cmd.getCourtLat());
    }

    /**
     * 关闭约球（权限校验 + 状态更新）
     * @param userId 当前用户
     * @param meetup 聚合根
     */
    public void close(String userId, Meetup meetup) {
        // 1. 权限和状态校验
        meetupPolicy.assertClose(userId, meetup);

        // 2. 更新状态
        meetup.getData().setStatus(MeetupStatusEnum.CLOSED);
        meetupGateway.save(meetup.getData());
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
            if (userRegistration.getStatus() == RegistrationStatusEnum.PENDING) {
                return ActionStateEnum.PENDING_REVIEW;
            }
            if (userRegistration.getStatus() == RegistrationStatusEnum.APPROVED) {
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

}
