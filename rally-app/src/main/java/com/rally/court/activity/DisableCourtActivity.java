package com.rally.court.activity;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.court.gateway.CourtRepository;
import com.rally.domain.court.model.Court;
import com.rally.domain.court.model.CourtData;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 disable-court：把指定球场置为已停用并保存，已停用的不再改写。
 */
@Component
@RequiredArgsConstructor
public class DisableCourtActivity {

    private final CourtRepository courtRepository;

    public void execute(String courtId) {
        // A1 按球场业务编号取出球场，取不到则报 COURT_NOT_FOUND
        CourtData data = courtRepository.findByBizId(courtId);
        Assert.notNull(data, BizErrorCode.COURT_NOT_FOUND);
        Court court = Court.of(data);
        // A2 球场已是停用状态时直接结束，不再改写，按幂等处理
        if (!court.disable()) {
            return;
        }
        // A3 把球场状态置为已停用并保存，其余字段保持取出时的原值
        courtRepository.save(court.state());
    }
}
