package com.rally.protourdata.tournamentdrawcollect.activity;

import com.alibaba.fastjson2.JSON;
import com.rally.domain.tour.match.RefreshTourMatchCommand;
import com.rally.domain.tour.match.TourMatch;
import com.rally.domain.tour.match.TourMatchIdentity;
import com.rally.domain.tour.match.TourMatchInsertResult;
import com.rally.domain.tour.match.TourMatchPersistence;
import com.rally.domain.tour.match.TourMatchState;
import com.rally.domain.tour.model.MatchData;
import com.rally.domain.tour.model.SetScore;
import com.rally.domain.tour.repository.MatchQueryRepository;
import com.rally.domain.tour.repository.TourMatchCollectRepository;
import com.rally.tour.convert.MatchAppConvertMapper;
import com.rally.tour.model.Match;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务活动 upsert-draw-matches：把一份来源签表中的比赛快照关联到已保存签表。
 */
@Component
@RequiredArgsConstructor
public class UpsertDrawMatchesActivity {

    private static final MatchAppConvertMapper MATCH_MAPPER = MatchAppConvertMapper.INSTANCE;

    private final MatchQueryRepository matchQueryRepository;
    private final TourMatchCollectRepository matchCollectRepository;

    /**
     * 比赛批次使用独立事务；失败只回滚本批次，不补偿此前已提交的签表。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void execute(Long drawId, List<Match> sourceMatches) {
        if (sourceMatches == null || sourceMatches.isEmpty()) {
            return;
        }
        if (drawId == null || drawId <= 0) {
            throw new IllegalArgumentException("保存比赛必须提供有效 drawId");
        }

        // A1：沿用 main 的 Match -> MatchData 转换，随后把每一场绑定到前一步
        // 已保存的原始 drawId。关键赛事身份必须逐项完整，不能因存量存在而省略。
        List<MatchData> matches = MATCH_MAPPER.toMatchDataList(sourceMatches);
        for (MatchData match : matches) {
            validateSourceIdentity(match);
            match.setDrawId(drawId);
        }

        // A2-A3：同批重复身份按到达顺序通过 C1 合并；来源 null/空字段
        // 不清除存量，状态允许前进或回退，来源遗漏的比赛不参与本次写入。
        MatchRepositoryPersistence persistence = new MatchRepositoryPersistence(drawId);
        for (MatchData match : matches) {
            TourMatch.saveOrRefresh(toCommand(match), persistence);
        }
    }

    private static void validateSourceIdentity(MatchData match) {
        if (match == null
                || match.getTournamentId() == null || match.getTournamentId().isBlank()
                || match.getYear() == null
                || match.getMatchId() == null || match.getMatchId().isBlank()) {
            throw new IllegalArgumentException(
                    "保存比赛必须提供 tournamentId、year、matchId");
        }
    }

    private static RefreshTourMatchCommand toCommand(MatchData match) {
        return new RefreshTourMatchCommand(
                match.getDrawId(),
                match.getMatchId(),
                match.getTournamentId(),
                match.getYear(),
                match.getMatchIndex(),
                match.getRoundNumber(),
                match.getRoundName(),
                match.getPlayer1Id(),
                match.getPlayer2Id(),
                match.getWinnerId(),
                match.getScheduledAt(),
                match.getScheduledAtText(),
                match.getStartedAt(),
                match.getEndedAt(),
                match.getCourt(),
                match.getCourtSeq(),
                match.getStatus(),
                match.getDurationMinutes(),
                match.getDescription(),
                match.getMatchDate(),
                setsToJson(match.getSets()));
    }

    private static String setsToJson(List<SetScore> sets) {
        return sets == null || sets.isEmpty() ? null : JSON.toJSONString(sets);
    }

    /**
     * 把既有查询/批量 upsert 仓储适配为 @tour.match 的 C1 持久化端口。
     * 存量只在首次定位时读取一次；每次写入后同步缓存，保证同批重复键按顺序合并。
     */
    private final class MatchRepositoryPersistence implements TourMatchPersistence {

        private final Long drawId;
        private Map<TourMatchIdentity, TourMatchState> states;

        private MatchRepositoryPersistence(Long drawId) {
            this.drawId = drawId;
        }

        @Override
        public TourMatchState findByIdentity(TourMatchIdentity identity) {
            ensureLoaded();
            return states.get(identity);
        }

        @Override
        public TourMatchInsertResult insert(TourMatchState state) {
            MatchData saved = save(state);
            TourMatchState savedState = toState(saved);
            states.put(savedStateIdentity(savedState), savedState);
            return TourMatchInsertResult.created(savedState.id());
        }

        @Override
        public boolean replaceSnapshot(TourMatchState state) {
            MatchData saved = save(state);
            TourMatchState savedState = toState(saved);
            states.put(savedStateIdentity(savedState), savedState);
            return true;
        }

        private void ensureLoaded() {
            if (states != null) {
                return;
            }
            states = new LinkedHashMap<>();
            List<MatchData> existing = matchQueryRepository.listByDrawId(drawId);
            if (existing == null) {
                return;
            }
            for (MatchData match : existing) {
                if (match == null) {
                    continue;
                }
                TourMatchState state = toState(match);
                states.put(savedStateIdentity(state), state);
            }
        }

        private MatchData save(TourMatchState state) {
            List<MatchData> saved = matchCollectRepository.saveOrUpdateBatch(
                    List.of(toMatchData(state)));
            if (saved == null || saved.size() != 1 || saved.get(0) == null) {
                throw new IllegalStateException("比赛保存未返回唯一结果");
            }
            return saved.get(0);
        }
    }

    private static TourMatchIdentity savedStateIdentity(TourMatchState state) {
        return TourMatchIdentity.fromSource(state.drawId(), state.matchId());
    }

    private static TourMatchState toState(MatchData match) {
        if (match.getDrawId() == null || match.getYear() == null) {
            throw new IllegalStateException("存量比赛缺少 drawId 或 year");
        }
        return new TourMatchState(
                match.getTourMatchId(),
                match.getMatchId(),
                match.getMatchIndex(),
                match.getDrawId(),
                match.getTournamentId(),
                match.getYear(),
                match.getRoundNumber(),
                match.getRoundName(),
                match.getPlayer1Id(),
                match.getPlayer2Id(),
                match.getWinnerId(),
                match.getScheduledAt(),
                match.getScheduledAtText(),
                match.getStartedAt(),
                match.getEndedAt(),
                match.getCourt(),
                match.getCourtSeq(),
                match.getStatus(),
                match.getDurationMinutes(),
                match.getDescription(),
                match.getMatchDate(),
                setsToJson(match.getSets()),
                null,
                null);
    }

    private static MatchData toMatchData(TourMatchState state) {
        MatchData match = new MatchData();
        match.setTourMatchId(state.id());
        match.setMatchId(state.matchId());
        match.setMatchIndex(state.matchIndex());
        match.setDrawId(state.drawId());
        match.setTournamentId(state.tournamentId());
        match.setYear(state.year());
        match.setRoundNumber(state.roundNumber());
        match.setRoundName(state.roundName());
        match.setPlayer1Id(state.player1Id());
        match.setPlayer2Id(state.player2Id());
        match.setWinnerId(state.winnerId());
        match.setScheduledAt(state.scheduledAt());
        match.setScheduledAtText(state.scheduledAtText());
        match.setStartedAt(state.startedAt());
        match.setEndedAt(state.endedAt());
        match.setCourt(state.court());
        match.setCourtSeq(state.courtSeq());
        match.setStatus(state.status());
        match.setDurationMinutes(state.durationMinutes());
        match.setDescription(state.description());
        match.setMatchDate(state.matchDate());
        match.setSets(state.setsJson() == null || state.setsJson().isBlank()
                ? null
                : JSON.parseArray(state.setsJson(), SetScore.class));
        return match;
    }
}
