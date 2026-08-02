package com.rally.domain.tournament.model;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.domain.system.CityConfig;
import com.rally.domain.tournament.convert.TournamentDomainConvertMapper;
import com.rally.domain.tournament.enums.TournamentStatusEnum;
import com.rally.domain.tournament.enums.TournamentRoundEnum;

/**
 * 赛事聚合根工厂
 */
public class TournamentFactory {

    private TournamentFactory() {
    }

    /**
     * 创建赛事草稿
     */
    public static Tournament create(TournamentCreateCmd cmd) {
        TournamentData data = TournamentDomainConvertMapper.INSTANCE.toTournamentData(cmd);
        data.setBizId(IdWorker.getIdStr());
        data.setCityName(CityConfig.getCityName(data.getCityCode()));
        data.setStatus(TournamentStatusEnum.DRAFT);
        data.setCurrentFilledSlots(0);
        // 赛事从资格赛开始；后续只能由轮次推进服务切换到正赛轮次。
        data.setCurrentRound(TournamentRoundEnum.QUALIFIER);
        return new Tournament(data);
    }
}
