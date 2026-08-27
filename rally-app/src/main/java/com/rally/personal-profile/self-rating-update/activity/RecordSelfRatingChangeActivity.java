package com.rally.personalprofile.selfratingupdate.activity;

import com.rally.domain.log.ProfileLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 业务活动 record-self-rating-change：记录本次 NTRP 自评修改。
 */
@Component
@RequiredArgsConstructor
public class RecordSelfRatingChangeActivity {

    private final ProfileLogService profileLogService;

    public void execute(String userId, BigDecimal oldNtrp, BigDecimal newNtrp) {
        // A1-A2：沿用 main 的日志建立与持久化链路。该命令会计算 new-old，
        // 首次填写取 0，因此同值与下降都会如实记录；插入异常不在活动内吞掉。
        profileLogService.saveNtrpChangeLog(userId, oldNtrp, newNtrp);
    }
}
