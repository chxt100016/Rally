package com.rally.db.tournament.repository;

import com.rally.db.tournament.convert.TournamentEntryConvertMapper;
import com.rally.db.tournament.entity.TournamentEntryPO;
import com.rally.db.tournament.service.TournamentEntryMybatisService;
import com.rally.domain.tournament.enums.TournamentEntryStageEnum;
import com.rally.domain.tournament.enums.TournamentEntryStatusEnum;
import com.rally.domain.tournament.enums.TournamentRoundEnum;
import com.rally.domain.tournament.gateway.TournamentEntryRepository;
import com.rally.domain.tournament.model.TournamentEntryData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 报名表 Repository 实现
 */
@Component
@RequiredArgsConstructor
public class TournamentEntryRepositoryImpl implements TournamentEntryRepository {

    private final TournamentEntryMybatisService tournamentEntryService;
    private static final TournamentEntryConvertMapper MAPPER = TournamentEntryConvertMapper.INSTANCE;

    @Override
    public void save(TournamentEntryData data) {
        TournamentEntryPO po = MAPPER.toTournamentEntryPO(data);
        boolean updated = po.getBizId() != null && tournamentEntryService.lambdaUpdate().eq(TournamentEntryPO::getBizId, po.getBizId()).update(po);
        if (!updated) {
            tournamentEntryService.save(po);
        }
    }

    @Override
    public TournamentEntryData findByBizId(String bizId) {
        TournamentEntryPO po = tournamentEntryService.lambdaQuery()
                .eq(TournamentEntryPO::getBizId, bizId)
                .one();
        return MAPPER.toTournamentEntryData(po);
    }

    @Override
    public TournamentEntryData findByTournamentAndUser(String tournamentId, String userId) {
        TournamentEntryPO po = tournamentEntryService.lambdaQuery()
                .eq(TournamentEntryPO::getTournamentId, tournamentId)
                .eq(TournamentEntryPO::getUserId, userId)
                .one();
        return MAPPER.toTournamentEntryData(po);
    }

    @Override
    public List<TournamentEntryData> findWaitingByTournamentAndStage(String tournamentId, TournamentEntryStageEnum stage, TournamentRoundEnum round) {
        List<TournamentEntryPO> pos = tournamentEntryService.lambdaQuery()
                .eq(TournamentEntryPO::getTournamentId, tournamentId)
                .eq(TournamentEntryPO::getStage, stage.name())
                .eq(TournamentEntryPO::getStatus, TournamentEntryStatusEnum.WAITING.name())
                .eq(TournamentEntryPO::getCurrentRound, round.name())
                .list();
        return pos.stream().map(MAPPER::toTournamentEntryData).collect(Collectors.toList());
    }

    @Override
    public List<TournamentEntryData> findByTournamentId(String tournamentId) {
        List<TournamentEntryPO> pos = tournamentEntryService.lambdaQuery()
                .eq(TournamentEntryPO::getTournamentId, tournamentId)
                .list();
        return pos.stream().map(MAPPER::toTournamentEntryData).collect(Collectors.toList());
    }

    @Override
    public List<TournamentRoundEnum> findDistinctWaitingRounds(String tournamentId, TournamentEntryStageEnum stage) {
        List<TournamentEntryPO> pos = tournamentEntryService.lambdaQuery()
                .select(TournamentEntryPO::getCurrentRound)
                .eq(TournamentEntryPO::getTournamentId, tournamentId)
                .eq(TournamentEntryPO::getStage, stage.name())
                .eq(TournamentEntryPO::getStatus, TournamentEntryStatusEnum.WAITING.name())
                .list();
        return pos.stream()
                .map(TournamentEntryPO::getCurrentRound)
                .distinct()
                .map(TournamentRoundEnum::valueOf)
                .collect(Collectors.toList());
    }
}
