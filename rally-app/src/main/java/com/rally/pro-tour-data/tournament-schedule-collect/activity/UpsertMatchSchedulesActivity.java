package com.rally.protourdata.tournamentschedulecollect.activity;

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
 * 业务活动 upsert-match-schedules：保存一份已解析的比赛排期快照。
 */
@Component
@RequiredArgsConstructor
public class UpsertMatchSchedulesActivity {

    private static final MatchAppConvertMapper MATCH_MAPPER = MatchAppConvertMapper.INSTANCE;

    private final MatchQueryRepository matchQueryRepository;
    private final TourMatchCollectRepository matchCollectRepository;

    /**
     * 比赛批次单独提交；任一转换、身份或写入失败都回滚整批，
     * 不补偿前置已提交的签表。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void execute(Long drawId, List<Match> sourceMatches) {
        // 沿用 main 的空批语义：无比赛时不校验 drawId，也不读写数据。
        if (sourceMatches == null || sourceMatches.isEmpty()) {
            return;
        }
        if (drawId == null || drawId <= 0) {
            throw new IllegalArgumentException("保存比赛排期必须提供有效 drawId");
        }

        // A1-A2：来源 client 保持 main 的日期/时间解析、Asia/Shanghai
        // 转换及“随后”场次推算；此处保留完整 Match 字段映射。
        List<MatchData> matches = MATCH_MAPPER.toMatchDataList(sourceMatches);
        for (MatchData match : matches) {
            validateSourceIdentity(match);
            match.setDrawId(drawId);
        }

        // A3：同批重复 drawId+matchId 按到达顺序逐次合并；
        // 仅非空快照字段覆盖存量，状态允许回退，来源遗漏不删除。
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
                    "保存比赛排期必须提供 tournamentId、year、matchId");
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
     * 把既有查询/批量 upsert 仓储适配为 @tour.match C1 写端口。
     * 存量只在首次定位时读取；每次写入后同步缓存，保证同批重复键按顺序合并。
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
                throw new IllegalStateException("比赛排期保存未返回唯一结果");
            }
            return saved.get(0);
        }
    }

    private static TourMatchIdentity savedStateIdentity(TourMatchState state) {
        return TourMatchIdentity.fromSource(state.drawId(), state.matchId());
    }

    private static TourMatchState toState(MatchData match) {
        if (match.getDrawId() == null || match.getYear() == null) {
            throw new IllegalStateException("存量比赛排期缺少 drawId 或 year");
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
