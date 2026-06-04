package com.rally.domain.meetup.service;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.meetup.enums.*;
import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.model.PublishCmd;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * 约球领域服务
 */
@Service
public class MeetupDomainService {

    /**
     * 校验发布参数
     */
    public void validatePublish(PublishCmd cmd) {
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
        if (cmd.getLevelMode() != null && cmd.getLevelMode() == LevelModeEnum.RANGE) {
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
    public MeetupData buildMeetupData(PublishCmd cmd, String userId, String cityCode) {
        MeetupData data = new MeetupData();
        data.setBizId(generateBizId());
        data.setCreatorId(userId);
        data.setTitle(cmd.getTitle() != null ? cmd.getTitle() : generateTitle(cmd));
        data.setMatchType(cmd.getMatchType());
        data.setMaxPlayers(cmd.getMaxPlayers());
        data.setCurrentPlayers(1); // 发布者占位
        data.setCityCode(cityCode);
        data.setStartTime(cmd.getStartTime());
        data.setEndTime(calculateEndTime(cmd.getStartTime(), cmd.getDuration()));
        data.setDuration(cmd.getDuration());
        data.setCourtName(cmd.getCourtName());
        data.setCourtAddress(cmd.getCourtAddress());
        data.setCourtLng(cmd.getLng());
        data.setCourtLat(cmd.getLat());
        data.setCourtGrid(generateCourtGrid(cmd.getCourtName(), cmd.getLng(), cmd.getLat()));
        data.setLevelMode(cmd.getLevelMode());
        data.setLevelValue(buildLevelValue(cmd));
        data.setGenderLimit(cmd.getGenderLimit());
        data.setJoinMode(cmd.getJoinMode());
        data.setCostItems(cmd.getCostItems());
        data.setStatus(MeetupStatusEnum.OPEN);
        return data;
    }

    /**
     * 更新 MeetupData（编辑时）
     */
    public void updateMeetupData(MeetupData data, PublishCmd cmd, String newCityCode) {
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
            data.setEndTime(calculateEndTime(cmd.getStartTime(), cmd.getDuration()));
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
            data.setCourtGrid(generateCourtGrid(
                    cmd.getCourtName() != null ? cmd.getCourtName() : data.getCourtName(),
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
    public String generateTitle(PublishCmd cmd) {
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
    public String generateCourtGrid(String courtName, double lng, double lat) {
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
    public String buildLevelValue(PublishCmd cmd) {
        if (cmd.getLevelMode() == null) {
            return null;
        }
        return switch (cmd.getLevelMode()) {
            case RANGE -> cmd.getLevelMin() + ":" + cmd.getLevelMax();
            case EXACT -> cmd.getLevelMin() != null ? cmd.getLevelMin().toString() : null;
            case ABOVE -> cmd.getLevelMin() != null ? cmd.getLevelMin().toString() : null;
            case BELOW -> cmd.getLevelMax() != null ? cmd.getLevelMax().toString() : null;
        };
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

    /**
     * 生成 bizId
     */
    private String generateBizId() {
        return String.valueOf(System.currentTimeMillis());
    }

    /**
     * 计算结束时间
     */
    private LocalDateTime calculateEndTime(LocalDateTime startTime, BigDecimal duration) {
        return startTime.plusHours(duration.longValue())
                .plusMinutes((duration.remainder(BigDecimal.ONE).multiply(new BigDecimal("60"))).longValue());
    }
}
