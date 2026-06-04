package com.rally.meetup;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.cache.UserContext;
import com.rally.client.geo.CityLocator;
import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.config.gateway.ConfigGateway;
import com.rally.domain.meetup.enums.MeetupStatusEnum;
import com.rally.domain.meetup.gateway.MeetupGateway;
import com.rally.domain.meetup.gateway.NearbyGateway;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.model.MeetupVO;
import com.rally.domain.meetup.model.PublishCmd;
import com.rally.meetup.convert.MeetupAppConvertMapper;
import com.rally.domain.user.gateway.TennisProfileGateway;
import com.rally.domain.user.model.TennisProfileData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * 约球写流程编排：发布、编辑、关闭
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeetupAppService {

    private final MeetupGateway meetupGateway;
    private final NearbyGateway nearbyGateway;
    private final ConfigGateway configGateway;
    private final CityLocator cityLocator;
    private final TennisProfileGateway tennisProfileGateway;

    private static final MeetupAppConvertMapper MAPPER = MeetupAppConvertMapper.INSTANCE;

    /**
     * 发布约球
     */
    @Transactional
    public MeetupVO publish(PublishCmd cmd) {
        String userId = UserContext.get();

        // 1. 当日发布上限校验
        int publishLimit = configGateway.getInt("anti_abuse.publish_per_day_limit", 5);
        long todayCount = meetupGateway.countTodayActive(userId);
        if (todayCount >= publishLimit) {
            throw new BusinessException(BizErrorCode.PUBLISH_LIMIT_EXCEEDED);
        }

        // 3. 城市开通校验（直接通过 cityCode 校验）
        String cityCode = cityLocator.validateCityCode(cmd.getCityCode());
        if (cityCode == null) {
            throw new BusinessException(BizErrorCode.CITY_NOT_OPENED);
        }

        // 4. 字段校验
        validatePublishCmd(cmd);

        // 5. 构建 MeetupData
        MeetupData data = buildMeetupData(cmd, userId, cityCode);

        // 6. 落库
        meetupGateway.save(data);

        // 7. GEO 双写
        try {
            nearbyGateway.add(cityCode, data.getBizId(), cmd.getLng(), cmd.getLat());
        } catch (Exception e) {
            log.warn("GEO 写入失败，不影响主流程: {}", e.getMessage());
        }

        // 8. 返回详情
        return buildMeetupVO(data, userId);
    }

    /**
     * 编辑约球
     */
    @Transactional
    public MeetupVO edit(PublishCmd cmd) {
        String userId = UserContext.get();
        String meetupId = cmd.getMeetupId();

        if (meetupId == null) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, "约球ID不能为空");
        }

        // 1. 查询约球
        MeetupData data = meetupGateway.findByBizId(meetupId);
        if (data == null) {
            throw new BusinessException(BizErrorCode.MEETUP_NOT_FOUND);
        }

        // 2. 权限校验：仅创建人可编辑
        if (!userId.equals(data.getCreatorId())) {
            throw new BusinessException(BizErrorCode.NOT_CREATOR);
        }

        // 3. 状态校验：懒判定
        MeetupStatusEnum realStatus = lazyStatus(data);
        if (realStatus == MeetupStatusEnum.finished || realStatus == MeetupStatusEnum.closed) {
            throw new BusinessException(BizErrorCode.MEETUP_STATUS_ILLEGAL);
        }

        // 4. 编辑锁定校验
        int lockMinutes = configGateway.getInt("meetup.edit_lock_minutes_before_start", 60);
        if (LocalDateTime.now().isAfter(data.getStartTime().minusMinutes(lockMinutes))) {
            throw new BusinessException(BizErrorCode.EDIT_LOCKED);
        }

        // 5. 城市开通校验（如果改了场地）
        boolean locationChanged = false;
        String newCityCode = data.getCityCode();
        if (cmd.getCityCode() != null && !cmd.getCityCode().equals(data.getCityCode())) {
            String cityCode = cityLocator.validateCityCode(cmd.getCityCode());
            if (cityCode == null) {
                throw new BusinessException(BizErrorCode.CITY_NOT_OPENED);
            }
            newCityCode = cityCode;
            locationChanged = true;
        } else if (cmd.getLng() != null && cmd.getLat() != null) {
            if (!cmd.getLng().equals(data.getCourtLng()) || !cmd.getLat().equals(data.getCourtLat())) {
                locationChanged = true;
            }
        }

        // 6. 更新字段
        updateMeetupData(data, cmd, newCityCode);

        // 7. 落库
        meetupGateway.save(data);

        // 8. GEO 更新（如果场地变了）
        if (locationChanged) {
            try {
                nearbyGateway.remove(data.getCityCode(), meetupId);
                nearbyGateway.add(newCityCode, meetupId, cmd.getLng(), cmd.getLat());
            } catch (Exception e) {
                log.warn("GEO 更新失败，不影响主流程: {}", e.getMessage());
            }
        }

        // 9. 返回详情
        return buildMeetupVO(data, userId);
    }

    /**
     * 关闭约球
     */
    @Transactional
    public void close(String meetupId) {
        String userId = UserContext.get();

        // 1. 查询约球
        MeetupData data = meetupGateway.findByBizId(meetupId);
        if (data == null) {
            throw new BusinessException(BizErrorCode.MEETUP_NOT_FOUND);
        }

        // 2. 权限校验：仅创建人可关闭
        if (!userId.equals(data.getCreatorId())) {
            throw new BusinessException(BizErrorCode.NOT_CREATOR);
        }

        // 3. 状态校验：懒判定
        MeetupStatusEnum realStatus = lazyStatus(data);
        if (realStatus == MeetupStatusEnum.finished || realStatus == MeetupStatusEnum.closed) {
            throw new BusinessException(BizErrorCode.MEETUP_STATUS_ILLEGAL);
        }

        // 4. 阶梯扣分（如果有人报名）
        if (data.getCurrentPlayers() > 1) {
            int penalty = calculateCancelPenalty(data.getStartTime());
            if (penalty > 0) {
                // TODO: 调用评分域扣分（交叉引用 04）
                log.info("发布者关闭约球扣分: userId={}, meetupId={}, penalty={}", userId, meetupId, penalty);
            }
        }

        // 5. 更新状态
        meetupGateway.updateStatus(meetupId, MeetupStatusEnum.closed.name());

        // 6. GEO 清理
        try {
            nearbyGateway.remove(data.getCityCode(), meetupId);
        } catch (Exception e) {
            log.warn("GEO 清理失败: {}", e.getMessage());
        }

        // 7. 发送取消通知（交叉引用 05）
        // TODO: 调用通知域发送取消通知
        log.info("约球已关闭: meetupId={}", meetupId);
    }

    /**
     * 懒判定：计算真实状态
     */
    private MeetupStatusEnum lazyStatus(MeetupData data) {
        if ((data.getStatus() == MeetupStatusEnum.open || data.getStatus() == MeetupStatusEnum.full)
                && data.getEndTime().isBefore(LocalDateTime.now())) {
            return MeetupStatusEnum.finished;
        }
        return data.getStatus();
    }



    /**
     * 校验发布参数
     */
    private void validatePublishCmd(PublishCmd cmd) {
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

        // level 校验
        if (cmd.getLevelMode() != null && cmd.getLevelMode() == com.rally.domain.meetup.enums.LevelModeEnum.range) {
            if (cmd.getLevelMin() == null || cmd.getLevelMax() == null) {
                throw new BusinessException(BizErrorCode.PARAM_ERROR, "水平范围必须指定最小值和最大值");
            }
            if (cmd.getLevelMin().compareTo(cmd.getLevelMax()) > 0) {
                throw new BusinessException(BizErrorCode.PARAM_ERROR, "水平最小值不能大于最大值");
            }
        }
    }

    /**
     * 构建 MeetupData
     */
    private MeetupData buildMeetupData(PublishCmd cmd, String userId, String cityCode) {
        MeetupData data = new MeetupData();
        data.setBizId(IdWorker.getIdStr());
        data.setCreatorId(userId);
        data.setTitle(cmd.getTitle() != null ? cmd.getTitle() : generateTitle(cmd));
        data.setMatchType(cmd.getMatchType());
        data.setMaxPlayers(cmd.getMaxPlayers());
        data.setCurrentPlayers(1); // 发布者占位
        data.setCityCode(cityCode);
        data.setStartTime(cmd.getStartTime());
        data.setEndTime(cmd.getStartTime().plusHours(cmd.getDuration().longValue())
                .plusMinutes((cmd.getDuration().remainder(BigDecimal.ONE).multiply(new BigDecimal("60"))).longValue()));
        data.setDuration(cmd.getDuration());
        data.setCourtName(cmd.getCourtName());
        data.setCourtAddress(cmd.getCourtAddress());
        data.setCourtLng(cmd.getLng());
        data.setCourtLat(cmd.getLat());
        data.setCourtGrid(generateCourtGrid(cmd.getCourtName(), cmd.getLng(), cmd.getLat()));
        data.setLevelMode(cmd.getLevelMode() != null ? cmd.getLevelMode() : com.rally.domain.meetup.enums.LevelModeEnum.exact);
        data.setLevelValue(buildLevelValue(cmd));
        data.setGenderLimit(cmd.getGenderLimit());
        data.setJoinMode(cmd.getJoinMode());
        data.setCourtName(cmd.getCourtName());
        data.setCostItems(cmd.getCostItems());
        data.setStatus(MeetupStatusEnum.open);
        return data;
    }

    /**
     * 更新 MeetupData（编辑时）
     */
    private void updateMeetupData(MeetupData data, PublishCmd cmd, String newCityCode) {
        if (cmd.getTitle() != null) {
            data.setTitle(cmd.getTitle());
        }
        if (cmd.getMatchType() != null) {
            data.setMatchType(cmd.getMatchType());
        }
        if (cmd.getMaxPlayers() != null) {
            data.setMaxPlayers(cmd.getMaxPlayers());
        }
        if (cmd.getStartTime() != null) {
            data.setStartTime(cmd.getStartTime());
            data.setEndTime(cmd.getStartTime().plusHours(cmd.getDuration().longValue())
                    .plusMinutes((cmd.getDuration().remainder(BigDecimal.ONE).multiply(new BigDecimal("60"))).longValue()));
        }
        if (cmd.getDuration() != null) {
            data.setDuration(cmd.getDuration());
        }
        if (cmd.getCourtName() != null) {
            data.setCourtName(cmd.getCourtName());
        }
        if (cmd.getCourtAddress() != null) {
            data.setCourtAddress(cmd.getCourtAddress());
        }
        if (cmd.getLng() != null && cmd.getLat() != null) {
            data.setCourtLng(cmd.getLng());
            data.setCourtLat(cmd.getLat());
            data.setCourtGrid(generateCourtGrid(cmd.getCourtName() != null ? cmd.getCourtName() : data.getCourtName(),
                    cmd.getLng(), cmd.getLat()));
        }
        data.setCityCode(newCityCode);
        if (cmd.getLevelMode() != null) {
            data.setLevelMode(cmd.getLevelMode());
        }
        if (cmd.getLevelMin() != null || cmd.getLevelMax() != null) {
            data.setLevelValue(buildLevelValue(cmd));
        }
        if (cmd.getGenderLimit() != null) {
            data.setGenderLimit(cmd.getGenderLimit());
        }
        if (cmd.getJoinMode() != null) {
            data.setJoinMode(cmd.getJoinMode());
        }
        if (cmd.getCostItems() != null) {
            data.setCostItems(cmd.getCostItems());
        }
    }

    /**
     * 生成标题
     */
    private String generateTitle(PublishCmd cmd) {
        String type = cmd.getMatchType().name();
        DayOfWeek dayOfWeek = cmd.getStartTime().getDayOfWeek();
        String dayName = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINESE);
        String time = cmd.getStartTime().toLocalTime().toString().substring(0, 5);
        String court = cmd.getCourtName() != null ? cmd.getCourtName() : "待定";
        return type + "·" + dayName + " " + time + "·" + court;
    }

    /**
     * 生成场地网格键
     */
    private String generateCourtGrid(String courtName, double lng, double lat) {
        if (courtName == null || courtName.isEmpty()) {
            return null;
        }
        // 网格大小约 50m
        double grid = 0.00045;
        int gridX = (int) Math.floor(lng / grid);
        int gridY = (int) Math.floor(lat / grid);
        String normalized = courtName.trim().toLowerCase();
        String key = normalized + ":" + gridX + ":" + gridY;
        return String.valueOf(key.hashCode());
    }

    /**
     * 构建水平值字符串
     */
    private String buildLevelValue(PublishCmd cmd) {
        if (cmd.getLevelMode() == null) {
            return null;
        }
        return switch (cmd.getLevelMode()) {
            case range -> cmd.getLevelMin() + ":" + cmd.getLevelMax();
            case exact -> cmd.getLevelMin() != null ? cmd.getLevelMin().toString() : null;
            case above -> cmd.getLevelMin() != null ? cmd.getLevelMin().toString() : null;
            case below -> cmd.getLevelMax() != null ? cmd.getLevelMax().toString() : null;
        };
    }

    /**
     * 计算关闭约球的阶梯扣分
     */
    private int calculateCancelPenalty(LocalDateTime startTime) {
        long hoursUntilStart = java.time.Duration.between(LocalDateTime.now(), startTime).toHours();

        if (hoursUntilStart >= 24) {
            return configGateway.getInt("meetup.cancel.penalty_24h_out", 5);
        } else if (hoursUntilStart >= 12) {
            return configGateway.getInt("meetup.cancel.penalty_12_24h", 10);
        } else if (hoursUntilStart >= 6) {
            return configGateway.getInt("meetup.cancel.penalty_6_12h", 15);
        } else {
            return configGateway.getInt("meetup.cancel.penalty_under_6h", 25);
        }
    }

    /**
     * 构建 MeetupVO
     */
    private MeetupVO buildMeetupVO(MeetupData data, String currentUserId) {
        MeetupVO vo = MAPPER.toMeetupVO(data);

        // 计算每人费用
        if (data.getCostItems() != null && !data.getCostItems().isEmpty() && data.getMaxPlayers() != null) {
            int totalAmount = data.getCostItems().stream()
                    .mapToInt(item -> item.getTotalAmount() != null ? item.getTotalAmount() : 0)
                    .sum();
            vo.setPerPersonCost((int) Math.ceil((double) totalAmount / data.getMaxPlayers()));
        }

        // 计算 actionState
        MeetupStatusEnum realStatus = lazyStatus(data);
        boolean isCreator = currentUserId.equals(data.getCreatorId());
        vo.setActionState(calculateActionState(realStatus, data, currentUserId, isCreator));

        // 计算 quitWillPenalize
        if (!isCreator) {
            long hoursUntilStart = java.time.Duration.between(LocalDateTime.now(), data.getStartTime()).toHours();
            vo.setQuitWillPenalize(hoursUntilStart < 6);
        }

        return vo;
    }

    /**
     * 计算 actionState
     */
    private com.rally.domain.meetup.enums.ActionStateEnum calculateActionState(
            MeetupStatusEnum realStatus, MeetupData data, String currentUserId, boolean isCreator) {

        // 终态判断
        if (realStatus == MeetupStatusEnum.finished || realStatus == MeetupStatusEnum.closed) {
            return isCreator ? com.rally.domain.meetup.enums.ActionStateEnum.OWNER_DISABLED
                    : com.rally.domain.meetup.enums.ActionStateEnum.DISABLED;
        }

        // 创建人视角
        if (isCreator) {
            int lockMinutes = configGateway.getInt("meetup.edit_lock_minutes_before_start", 60);
            boolean locked = LocalDateTime.now().isAfter(data.getStartTime().minusMinutes(lockMinutes));
            return locked ? com.rally.domain.meetup.enums.ActionStateEnum.OWNER_EDIT_LOCKED
                    : com.rally.domain.meetup.enums.ActionStateEnum.OWNER_EDITABLE;
        }

        // 访客视角：需要查询报名状态
        // TODO: 查询 waitlist 状态
        // 暂时返回默认值
        if (realStatus == MeetupStatusEnum.full) {
            return com.rally.domain.meetup.enums.ActionStateEnum.FULL;
        }
        return data.getJoinMode() == com.rally.domain.meetup.enums.JoinModeEnum.direct
                ? com.rally.domain.meetup.enums.ActionStateEnum.JOIN_DIRECT
                : com.rally.domain.meetup.enums.ActionStateEnum.APPLY_APPROVAL;
    }
}
