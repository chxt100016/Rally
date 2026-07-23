package com.rally.db.tournament.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rally.db.tournament.convert.TournamentConvertMapper;
import com.rally.db.tournament.entity.TournamentPO;
import com.rally.db.tournament.service.TournamentService;
import com.rally.domain.meetup.model.PageDTO;
import com.rally.domain.tournament.enums.TournamentStatusEnum;
import com.rally.domain.tournament.gateway.TournamentRepository;
import com.rally.domain.tournament.model.TournamentAdminListCmd;
import com.rally.domain.tournament.model.TournamentData;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 赛事主表 Repository 实现
 */
@Component
@RequiredArgsConstructor
public class TournamentRepositoryImpl implements TournamentRepository {

    private final TournamentService tournamentService;
    private static final TournamentConvertMapper MAPPER = TournamentConvertMapper.INSTANCE;

    @Override
    public void save(TournamentData data) {
        TournamentPO po = MAPPER.toTournamentPO(data);
        boolean updated = po.getBizId() != null && tournamentService.lambdaUpdate().eq(TournamentPO::getBizId, po.getBizId()).update(po);
        if (!updated) {
            tournamentService.save(po);
        }
    }

    @Override
    public TournamentData findByBizId(String bizId) {
        TournamentPO po = tournamentService.lambdaQuery()
                .eq(TournamentPO::getBizId, bizId)
                .one();
        return MAPPER.toTournamentData(po);
    }

    @Override
    public PageDTO<TournamentData> pageList(TournamentAdminListCmd cmd) {
        LambdaQueryWrapper<TournamentPO> wrapper = new LambdaQueryWrapper<TournamentPO>()
                .eq(StringUtils.isNotBlank(cmd.getCityCode()), TournamentPO::getCityCode, cmd.getCityCode())
                .eq(cmd.getStatus() != null, TournamentPO::getStatus, cmd.getStatus() == null ? null : cmd.getStatus().name())
                .eq(StringUtils.isNotBlank(cmd.getNtrpLevel()), TournamentPO::getNtrpLevel, cmd.getNtrpLevel())
                .orderByDesc(TournamentPO::getCreateTime);
        Page<TournamentPO> page = tournamentService.page(new Page<>(cmd.getPageNum(), cmd.getPageSize()), wrapper);
        return new PageDTO<>(MAPPER.toTournamentDataList(page.getRecords()), page.getTotal(), page.getCurrent() * page.getSize() < page.getTotal());
    }

    @Override
    public List<TournamentData> findActiveWithQualifierStarted(LocalDateTime now) {
        List<TournamentPO> pos = tournamentService.lambdaQuery()
                .eq(TournamentPO::getStatus, TournamentStatusEnum.ACTIVE.name())
                .le(TournamentPO::getQualifierStartTime, now)
                .list();
        return pos.stream().map(MAPPER::toTournamentData).collect(Collectors.toList());
    }

    @Override
    public boolean incrementFilledSlots(String tournamentId) {
        return tournamentService.lambdaUpdate()
                .eq(TournamentPO::getBizId, tournamentId)
                .apply("current_filled_slots < total_slots")
                .setSql("current_filled_slots = current_filled_slots + 1")
                .update();
    }
}
