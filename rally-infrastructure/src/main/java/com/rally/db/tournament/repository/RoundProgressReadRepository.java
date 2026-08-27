package com.rally.db.tournament.repository;

import com.rally.db.tournament.entity.TournamentMatchPO;
import com.rally.db.tournament.entity.TournamentPO;
import com.rally.db.tournament.service.TournamentMatchMybatisService;
import com.rally.db.tournament.service.TournamentService;
import com.rally.domain.tournament.enums.TournamentRoundEnum;
import com.rally.domain.tournament.roundprogress.RoundProgressMatchSnapshot;
import com.rally.domain.tournament.roundprogress.RoundProgressReader;
import com.rally.domain.tournament.roundprogress.RoundProgressSnapshot;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 轮次进度只读端口实现：在同一只读事务里读取赛事行与其比赛投影，保证快照一致。
 */
@Component
@RequiredArgsConstructor
public class RoundProgressReadRepository implements RoundProgressReader {

    private final TournamentService tournamentService;
    private final TournamentMatchMybatisService tournamentMatchMybatisService;

    @Override
    @Transactional(readOnly = true)
    public RoundProgressSnapshot loadSnapshot(String tournamentId) {
        TournamentPO tournament = tournamentService.lambdaQuery()
                .select(TournamentPO::getBizId, TournamentPO::getTotalSlots,
                        TournamentPO::getCurrentFilledSlots, TournamentPO::getCurrentRound)
                .eq(TournamentPO::getBizId, tournamentId)
                .one();
        if (tournament == null) {
            return null;
        }
        List<TournamentMatchPO> matchPOs = tournamentMatchMybatisService.lambdaQuery()
                .select(TournamentMatchPO::getTournamentId, TournamentMatchPO::getRound, TournamentMatchPO::getStatus)
                .eq(TournamentMatchPO::getTournamentId, tournamentId)
                .list();
        List<RoundProgressMatchSnapshot> matches = matchPOs.stream().map(po -> new RoundProgressMatchSnapshot(po.getTournamentId(), parseRound(po.getRound()), po.getStatus())).toList();
        return new RoundProgressSnapshot(tournament.getBizId(), tournament.getTotalSlots(), tournament.getCurrentFilledSlots(), parseRound(tournament.getCurrentRound()), matches);
    }

    /** 历史脏数据可能写入未知轮次，这里按未知处理而不是让整次判定失败。 */
    private TournamentRoundEnum parseRound(String round) {
        if (StringUtils.isBlank(round)) {
            return null;
        }
        return EnumUtils.getEnum(TournamentRoundEnum.class, round);
    }
}
