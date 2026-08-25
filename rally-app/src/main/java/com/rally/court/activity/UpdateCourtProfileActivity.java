package com.rally.court.activity;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.court.gateway.CourtRepository;
import com.rally.domain.court.model.Court;
import com.rally.domain.court.model.CourtData;
import com.rally.domain.court.model.CourtUpdateCmd;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 业务活动 update-court-profile：取出球场，把运营填了的字段逐项改写并保存。
 */
@Component
@RequiredArgsConstructor
public class UpdateCourtProfileActivity {

    private final CourtRepository courtRepository;

    @Transactional(rollbackFor = Exception.class)
    public void execute(CourtUpdateCmd cmd) {
        // A1 按球场业务编号取出球场，取不到则报 COURT_NOT_FOUND
        CourtData data = courtRepository.findByBizId(cmd.getCourtId());
        Assert.notNull(data, BizErrorCode.COURT_NOT_FOUND);
        Court court = Court.of(data);
        // A2 逐项改写球场字段（别名与标签空列表清空、不传不改，改名连带重算拼音）
        // A3 归属信息中给了的城市与区域连带改写名称
        // A4 展示资料中给了的展示项逐项改写，其余项保持原值
        court.updateProfile(cmd);
        // A5 保存球场，A2 到 A5 在同一个事务内，失败时全部改写不生效
        courtRepository.save(court.state());
    }
}
