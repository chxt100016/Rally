package com.rally.tournament.courtbookerselect.activity;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.tournament.gateway.TournamentMatchRepository;
import com.rally.domain.tournament.model.TournamentMatch;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 业务活动 select-court-booker：比赛参与者认领订场职责。
 */
@Component
@RequiredArgsConstructor
public class SelectCourtBookerActivity {

    private final TournamentMatchRepository matchRepository;

    /**
     * 保持既有认领链路的聚合校验、当前时间写入与乐观锁事务语义。
     */
    @Transactional(rollbackFor = Exception.class)
    public void execute(String matchId, String participantUserId) {
        TournamentMatch match = matchRepository.findByBizIdWithParticipants(matchId);
        Assert.notNull(match, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);
        match.selectCourtBooker(participantUserId);
        if (!matchRepository.updateWithVersion(match.getData())) {
            throw new BusinessException(BizErrorCode.TOURNAMENT_MATCH_VERSION_CONFLICT);
        }
    }
}
