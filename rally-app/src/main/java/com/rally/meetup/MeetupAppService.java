package com.rally.meetup;

import com.rally.utils.UserContext;
import com.rally.domain.court.service.CourtDomainService;
import com.rally.domain.meetup.gateway.NearbyGateway;
import com.rally.domain.meetup.model.*;
import com.rally.domain.meetup.model.MeetupEditCmd;
import com.rally.domain.meetup.service.MeetupPolicy;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.system.enums.SystemConfigKey;
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

    private final NearbyGateway nearbyGateway;

    private final MeetupDomainService meetupDomainService;

    private final MeetupPolicy meetupPolicy;

    private final CourtDomainService courtDomainService;

    /**
     * 发布约球
     */
    @Transactional
    public void publish(MeetupPublishCmd cmd) {
        String userId = UserContext.get();

        // 1. 校验
        meetupPolicy.assertPublish(userId, cmd);

        // 2. 构建 MeetupData 并持久化（含创建者自动报名）
        meetupDomainService.save(userId, cmd);

        // 3. 球场查找或创建（200m 范围内合并）
        courtDomainService.findOrCreate(cmd.getCourtName(), cmd.getCourtAddress(), cmd.getCourtLng(), cmd.getCourtLat(), cmd.getCityCode(), cmd.getDistrictCode(), null);
    }

    /**
     * 编辑约球
     */
    @Transactional
    public MeetupVO edit(MeetupEditCmd cmd) {
        String meetupId = cmd.getMeetupId();
        String userId = UserContext.get();

        // 1. 获取聚合根
        Meetup meetup = meetupDomainService.get(meetupId);
        MeetupData data = meetup.getData();

        // 2. 编辑校验
        meetupPolicy.assertEdit(meetup, cmd);

        // 3. 场地变更检测（更新前检测）
        boolean locationChanged = meetup.isLocationChanged(cmd);

        // 4. 更新字段 + 落库
        meetupDomainService.edit(userId, meetup, cmd);

        // 5. GEO 更新（如果场地变了）
        if (locationChanged) {
            try {
                nearbyGateway.remove(data.getCityCode(), meetupId);
                nearbyGateway.add(data.getCityCode(), meetupId, cmd.getCourtLng(), cmd.getCourtLat());
            } catch (Exception e) {
                log.warn("GEO 更新失败，不影响主流程: {}", e.getMessage());
            }
        }

        // 6. 返回详情
        return MeetupAppConvertMapper.INSTANCE.toMeetupVO(data);
    }

    /**
     * 关闭约球
     */
    @Transactional
    public void close(String meetupId) {
        String userId = UserContext.get();

        // 1. 查询聚合根
        Meetup meetup = meetupDomainService.get(meetupId);
        MeetupData data = meetup.getData();

        // 2. 权限和状态校验 + 更新状态
        meetupDomainService.close(userId, meetup);

        // 3. 阶梯扣分（如果有人报名）
        if (data.getCurrentPlayers() > 1) {
            int penalty24h = SystemConfig.getInt(SystemConfigKey.MEETUP_CANCEL_PENALTY_24H_OUT.getKey());
            int penalty12h = SystemConfig.getInt(SystemConfigKey.MEETUP_CANCEL_PENALTY_12_24H.getKey());
            int penalty6h = SystemConfig.getInt(SystemConfigKey.MEETUP_CANCEL_PENALTY_6_12H.getKey());
            int penaltyUnder6h = SystemConfig.getInt(SystemConfigKey.MEETUP_CANCEL_PENALTY_UNDER_6H.getKey());
            int penalty = meetupDomainService.calculateCancelPenalty(
                    data.getStartTime(), penalty24h, penalty12h, penalty6h, penaltyUnder6h);
            if (penalty > 0) {
                // TODO: 调用评分域扣分（交叉引用 04）
                log.info("发布者关闭约球扣分: userId={}, meetupId={}, penalty={}", userId, meetupId, penalty);
            }
        }

        // 4. GEO 清理
        try {
            nearbyGateway.remove(data.getCityCode(), meetupId);
        } catch (Exception e) {
            log.warn("GEO 清理失败: {}", e.getMessage());
        }

        // 5. 发送取消通知（交叉引用 05）
        // TODO: 调用通知域发送取消通知
        log.info("约球已关闭: meetupId={}", meetupId);
    }
}
