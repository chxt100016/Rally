package com.rally.recap;

import com.rally.utils.UserContext;
import com.rally.domain.recap.enums.RecapOverallStatus;
import com.rally.domain.recap.model.Recap;
import com.rally.domain.recap.model.RecapSubmitCmd;
import com.rally.domain.recap.service.RecapDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 赛后收集应用服务（薄编排层）
 * <p>
 * 职责：
 * 1. 获取当前用户上下文
 * 2. 委托领域服务执行业务逻辑
 * 3. 捕获比分冲突异常，转换为状态码
 * 4. VO -> DTO 转换
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecapAppService {

    private final RecapDomainService recapDomainService;


    /**
     * 提交赛后收集
     *
     * @return 整体状态枚举
     */
    public RecapOverallStatus submit(RecapSubmitCmd cmd) {
        String userId = UserContext.get();

        // 1. 加载聚合根（含业务校验）
        Recap recap = recapDomainService.get(userId, cmd.getMeetupId());

        // 2. 提交评价（独立事务）
        recapDomainService.submitReviews(recap, cmd.getReviews());

        // 3. 提交比分（独立事务，冲突返回 false）
        boolean scoreSuccess = recapDomainService.submitScores(recap, cmd.getScores(), cmd.getScoreVersion());

        return scoreSuccess ? RecapOverallStatus.ALL_SUCCESS : RecapOverallStatus.PARTIAL;
    }



}
