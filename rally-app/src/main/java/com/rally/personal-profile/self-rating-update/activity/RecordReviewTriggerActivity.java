package com.rally.personalprofile.selfratingupdate.activity;

import com.rally.domain.log.ProfileLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 record-review-trigger：记录自评上调触发的核查期日志。
 */
@Component
@RequiredArgsConstructor
public class RecordReviewTriggerActivity {

    private final ProfileLogService profileLogService;

    public void execute(String userId, int requiredMatches) {
        // A1：仅正数所需场次表示本次真正触发核查；0 和 -1 不构造日志。
        if (requiredMatches <= 0) {
            return;
        }

        // A2：沿用 main 的核查触发日志链路，保留类型、原因、备注、bizId/时间填充及异常传播语义。
        profileLogService.saveReviewTriggerLog(userId, requiredMatches);
    }
}
