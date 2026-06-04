package com.rally.meetup;

import com.rally.cache.UserContext;
import com.rally.client.geo.CityLocator;
import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.meetup.enums.MeetupStatusEnum;
import com.rally.domain.meetup.gateway.MeetupGateway;
import com.rally.domain.meetup.gateway.NearbyGateway;
import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.model.MeetupVO;
import com.rally.domain.meetup.model.PublishCmd;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.meetup.convert.MeetupAppConvertMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 约球写流程编排：发布、编辑、关闭
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeetupAppService {

    private final MeetupGateway meetupGateway;
    private final NearbyGateway nearbyGateway;
    private final CityLocator cityLocator;
    private final MeetupDomainService meetupDomainService;



    /**
     * 发布约球
     */
    @Transactional
    public MeetupVO publish(PublishCmd cmd) {
        String userId = UserContext.get();

        // 1. 当日发布上限校验
        int publishLimit = SystemConfig.getInt("anti_abuse.publish_per_day_limit", 5);
        long todayCount = meetupGateway.countTodayActive(userId);
        if (todayCount >= publishLimit) {
            throw new BusinessException(BizErrorCode.PUBLISH_LIMIT_EXCEEDED);
        }

        // 2. 城市开通校验
        String cityCode = cityLocator.validateCityCode(cmd.getCityCode());
        if (cityCode == null) {
            throw new BusinessException(BizErrorCode.CITY_NOT_OPENED);
        }

        // 3. 字段校验（domain）
        meetupDomainService.validatePublish(cmd);

        // 4. 构建 MeetupData（domain）
        MeetupData data = meetupDomainService.buildMeetupData(cmd, userId, cityCode);

        // 5. 落库
        meetupGateway.save(data);

        // 6. GEO 双写
        try {
            nearbyGateway.add(cityCode, data.getBizId(), cmd.getLng(), cmd.getLat());
        } catch (Exception e) {
            log.warn("GEO 写入失败，不影响主流程: {}", e.getMessage());
        }

        // 7. 返回详情
        return MeetupAppConvertMapper.INSTANCE.toMeetupVO(data);
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

        // 2. 权限和状态校验（domain）
        Meetup meetup = new Meetup(data);
        int lockMinutes = SystemConfig.getInt("meetup.edit_lock_minutes_before_start", 60);
        if (!meetup.canEdit(userId, lockMinutes)) {
            throw new BusinessException(BizErrorCode.MEETUP_STATUS_ILLEGAL);
        }

        // 3. 城市开通校验（如果改了场地）
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

        // 4. 更新字段（domain）
        meetupDomainService.updateMeetupData(data, cmd, newCityCode);

        // 5. 落库
        meetupGateway.save(data);

        // 6. GEO 更新（如果场地变了）
        if (locationChanged) {
            try {
                nearbyGateway.remove(data.getCityCode(), meetupId);
                nearbyGateway.add(newCityCode, meetupId, cmd.getLng(), cmd.getLat());
            } catch (Exception e) {
                log.warn("GEO 更新失败，不影响主流程: {}", e.getMessage());
            }
        }

        // 7. 返回详情
        return MeetupAppConvertMapper.INSTANCE.toMeetupVO(data);
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

        // 2. 权限和状态校验（domain）
        Meetup meetup = new Meetup(data);
        if (!meetup.canClose(userId)) {
            throw new BusinessException(BizErrorCode.MEETUP_STATUS_ILLEGAL);
        }

        // 3. 阶梯扣分（如果有人报名）
        if (data.getCurrentPlayers() > 1) {
            int penalty24h = SystemConfig.getInt("meetup.cancel.penalty_24h_out", 5);
            int penalty12h = SystemConfig.getInt("meetup.cancel.penalty_12_24h", 10);
            int penalty6h = SystemConfig.getInt("meetup.cancel.penalty_6_12h", 15);
            int penaltyUnder6h = SystemConfig.getInt("meetup.cancel.penalty_under_6h", 25);
            int penalty = meetupDomainService.calculateCancelPenalty(
                    data.getStartTime(), penalty24h, penalty12h, penalty6h, penaltyUnder6h);
            if (penalty > 0) {
                // TODO: 调用评分域扣分（交叉引用 04）
                log.info("发布者关闭约球扣分: userId={}, meetupId={}, penalty={}", userId, meetupId, penalty);
            }
        }

        // 4. 更新状态
        meetupGateway.updateStatus(meetupId, MeetupStatusEnum.CLOSED.name());

        // 5. GEO 清理
        try {
            nearbyGateway.remove(data.getCityCode(), meetupId);
        } catch (Exception e) {
            log.warn("GEO 清理失败: {}", e.getMessage());
        }

        // 6. 发送取消通知（交叉引用 05）
        // TODO: 调用通知域发送取消通知
        log.info("约球已关闭: meetupId={}", meetupId);
    }
}
