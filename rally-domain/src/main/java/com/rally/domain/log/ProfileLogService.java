package com.rally.domain.log;

import com.rally.domain.user.enums.ChangeLogTypeEnum;
import com.rally.domain.user.enums.ChangeReasonEnum;
import com.rally.domain.log.gateway.ProfileChangeLogGateway;
import com.rally.domain.log.model.ProfileChangeLogData;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 变更日志领域服务
 * 负责构建和保存各类变更日志
 */
@Service
public class ProfileLogService {

    @Resource
    private ProfileChangeLogGateway profileChangeLogGateway;

    /**
     * 构建并保存 NTRP 变更日志
     */
    public void saveNtrpChangeLog(String userId, BigDecimal oldNtrp, BigDecimal newNtrp) {
        BigDecimal delta = oldNtrp != null ? newNtrp.subtract(oldNtrp) : BigDecimal.ZERO;
        ProfileChangeLogData log = new ProfileChangeLogData();
        log.setUserId(userId);
        log.setType(ChangeLogTypeEnum.NTRP);
        log.setBeforeValue(oldNtrp);
        log.setAfterValue(newNtrp);
        log.setValue(delta);
        log.setReason(ChangeReasonEnum.USER);
        profileChangeLogGateway.save(log);
    }

    /**
     * 构建并保存核查期触发日志
     */
    public void saveReviewTriggerLog(String userId, int requiredMatches) {
        ProfileChangeLogData log = new ProfileChangeLogData();
        log.setUserId(userId);
        log.setType(ChangeLogTypeEnum.UNDER_REVIEW);
        log.setBeforeValue(new BigDecimal(requiredMatches));
        log.setAfterValue(new BigDecimal(requiredMatches));
        log.setReason(ChangeReasonEnum.USER);
        log.setRemark("自评向上修改触发核查期");
        profileChangeLogGateway.save(log);
    }

    /**
     * 构建并保存遇差票重置日志
     */
    public void saveReviewResetLog(String userId, BigDecimal beforeRemaining, int requiredMatches, String meetupId) {
        ProfileChangeLogData log = new ProfileChangeLogData();
        log.setUserId(userId);
        log.setType(ChangeLogTypeEnum.UNDER_REVIEW);
        log.setBeforeValue(beforeRemaining);
        log.setAfterValue(new BigDecimal(requiredMatches));
        log.setReason(ChangeReasonEnum.REVIEW_BAD);
        log.setRefId(meetupId);
        log.setRemark("遇差票重置核查期进度");
        profileChangeLogGateway.save(log);
    }

    /**
     * 构建并保存核查期推进日志
     */
    public void saveReviewAdvanceLog(String userId, BigDecimal beforeRemaining, BigDecimal afterRemaining, String meetupId) {
        ProfileChangeLogData log = new ProfileChangeLogData();
        log.setUserId(userId);
        log.setType(ChangeLogTypeEnum.UNDER_REVIEW);
        log.setBeforeValue(beforeRemaining);
        log.setAfterValue(afterRemaining);
        log.setReason(ChangeReasonEnum.SYSTEM);
        log.setRefId(meetupId);
        profileChangeLogGateway.save(log);
    }
}
