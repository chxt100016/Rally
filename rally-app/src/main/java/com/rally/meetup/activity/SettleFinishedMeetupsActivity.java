package com.rally.meetup.activity;

import com.rally.domain.meetup.finishsettlement.FinishSettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 settle-finished-meetups：批量结算已过结束时间的指定状态约球。
 */
@Component
@RequiredArgsConstructor
public class SettleFinishedMeetupsActivity {

    private final FinishSettlementService finishSettlementService;

    /**
     * 由领域服务在构造单条批量更新时取当前时间，完成筛选、状态迁移并返回影响行数。
     *
     * @return 本次由单条更新置为 FINISHED 的记录数，无命中时为 0
     */
    public Integer execute() {
        // A1-A3 当前时间基准、精确状态筛选和单条批量更新均由已确认的领域服务保证。
        return finishSettlementService.settle();
    }
}
