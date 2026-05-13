package com.rally.tennis;

import com.rally.client.tennistv.TennisTvClient;
import com.rally.client.tennistv.model.AtpDrawsResponse;
import com.rally.client.tennistv.model.AtpOopResponse;
import com.rally.client.tennistv.model.MatchesResponse;
import com.rally.client.wta.WtaClient;
import com.rally.client.wta.model.WtaDrawsResponse;
import com.rally.client.wta.model.WtaMatchesResponse;
import com.rally.db.tennis.entity.TennisMatchPO;
import com.rally.db.tennis.entity.TennisSetScorePO;
import com.rally.db.tennis.repository.TennisMatchRepository;
import com.rally.db.tennis.repository.TennisSetScoreRepository;
import com.rally.domain.tennis.model.TennisRoundEnum;
import com.rally.tennis.convert.DrawMatchAppConvertMapper;
import com.rally.tennis.convert.MatchAppConvertMapper;
import com.rally.tennis.convert.OopMatchAppConvertMapper;
import com.rally.tennis.model.Match;
import com.rally.tennis.model.MatchStatus;
import com.rally.tennis.model.SetScore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MatchCollectService {

    @Resource
    private TennisMatchRepository tennisMatchRepository;

    @Resource
    private TennisSetScoreRepository tennisSetScoreRepository;

    @Resource
    private TennisTvClient tennisTvClient;

    @Resource
    private WtaClient wtaClient;

    public int collect(List<MatchesResponse.MatchInfo> matches) {
        if (CollectionUtils.isEmpty(matches)) {
            return 0;
        }
        List<Match> data = matches.stream()
                .map(MatchAppConvertMapper.INSTANCE::toMatch)
                .toList();
        this.saveMatches(data);
        return data.size();
    }

    public void atpFromDraw(AtpDrawsResponse response, String tournamentId, Long drawId, Integer year) {
        AtpDrawsResponse.Draw draw = response.getMS();
        List<Match> allMatches = new ArrayList<>();
        if (draw == null || CollectionUtils.isEmpty(draw.getRounds())) {
            return;
        }

        for (AtpDrawsResponse.Round round : draw.getRounds()) {
            if (CollectionUtils.isEmpty(round.getFixtures())) {
                continue;
            }
            for (AtpDrawsResponse.Fixture fixture : round.getFixtures()) {
                Match match = DrawMatchAppConvertMapper.INSTANCE.toMatch(fixture);
                match.setTournamentId(tournamentId);
                match.setDrawId(drawId);
                match.setYear(year);
                match.setRoundNumber(round.getRoundId());
                match.setRoundName(TennisRoundEnum.toShortName(round.getRoundName()));

                // 如果没有有效的 matchId，生成一个唯一 ID
                if (match.getMatchId() == null || match.getMatchId().isEmpty()) {
                    String generatedId = generateMatchId(tournamentId, drawId, round.getRoundId(),
                            match.getPlayer1Id(), match.getPlayer2Id());
                    match.setMatchId(generatedId);
                }

                allMatches.add(match);
            }
        }

        this.saveMatches(allMatches);
    }

    public void wtaFromDraw(WtaDrawsResponse response, String tournamentId, Long drawId, int year) {
        if (response == null || response.getData() == null
                || CollectionUtils.isEmpty(response.getData().getResults())) {
            return;
        }
        List<Match> allMatches = new ArrayList<>();
        for (WtaDrawsResponse.RoundResult roundResult : response.getData().getResults()) {
            if (roundResult.getRound() == null || CollectionUtils.isEmpty(roundResult.getMatches())) {
                continue;
            }
            WtaDrawsResponse.RoundInfo round = roundResult.getRound();
            Integer roundNumber = parseRoundId(round.getId());
            String roundName = round.getShortName();

            for (WtaDrawsResponse.MatchResult m : roundResult.getMatches()) {
                Match match = new Match();
                match.setMatchId(m.getMatchId());
                match.setTournamentId(tournamentId);
                match.setDrawId(drawId);
                match.setYear(year);
                match.setRoundNumber(roundNumber);
                match.setRoundName(roundName);
                // "0" 表示 BYE，转为 null
                match.setPlayer1Id("0".equals(m.getPlayerId()) ? null : m.getPlayerId());
                match.setPlayer2Id("0".equals(m.getOpponentId()) ? null : m.getOpponentId());
                match.setWinnerId("0".equals(m.getWinningPlayerId()) ? null : m.getWinningPlayerId());
                match.setStatus(MatchStatus.toStatus(m.getMState()));
                match.setSets(parseWtaDrawSets(m));
                allMatches.add(match);
            }
        }
        this.saveMatches(allMatches);
    }

    private Integer parseRoundId(String id) {
        if (id == null) return null;
        try {
            return Integer.parseInt(id);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<SetScore> parseWtaDrawSets(WtaDrawsResponse.MatchResult m) {
        List<SetScore> sets = new ArrayList<>();
        Integer[][] setData = {
            {m.getSet1Player(), m.getSet1Opponent(), m.getSet1Tie()},
            {m.getSet2Player(), m.getSet2Opponent(), m.getSet2Tie()},
            {m.getSet3Player(), m.getSet3Opponent(), m.getSet3Tie()},
            {m.getSet4Player(), m.getSet4Opponent(), m.getSet4Tie()},
            {m.getSet5Player(), m.getSet5Opponent(), m.getSet5Tie()},
        };
        for (int i = 0; i < setData.length; i++) {
            if (setData[i][0] == null) break;
            SetScore s = new SetScore();
            s.setSetNumber(i + 1);
            s.setP1Games(setData[i][0]);
            s.setP2Games(setData[i][1]);
            s.setP1Tiebreak(setData[i][2]);
            sets.add(s);
        }
        return sets.isEmpty() ? null : sets;
    }

    public void atpFromOop() {
        List<AtpOopResponse> oop = tennisTvClient.getOop();
        if (CollectionUtils.isEmpty(oop)) {
            return;
        }

        List<Match> allMatches = new ArrayList<>();
        for (AtpOopResponse tournament : oop) {
            if (CollectionUtils.isEmpty(tournament.getOop())) {
                continue;
            }
            for (AtpOopResponse.OopDay day : tournament.getOop()) {
                if (day.getCourts() == null) {
                    continue;
                }
                for (AtpOopResponse.CourtDetail court : day.getCourts().values()) {
                    if (CollectionUtils.isEmpty(court.getMatches())) {
                        continue;
                    }

                    java.time.LocalDateTime lastMatchScheduledAt = null;

                    for (AtpOopResponse.MatchDetail detail : court.getMatches()) {
                        if (!detail.getAssociationCode().equals("ATP")) {
                            continue;
                        }
                        Match match = OopMatchAppConvertMapper.INSTANCE.toMatch(detail);

                        // 处理 "Followed By" 的情况：使用上一场比赛时间 + 70分钟
                        if ("Followed By".equals(detail.getNotBeforeText()) && match.getScheduledAt() == null) {
                            if (lastMatchScheduledAt != null) {
                                match.setScheduledAt(lastMatchScheduledAt.plusMinutes(70));
                            }
                        }

                        // 更新上一场比赛的时间
                        if (match.getScheduledAt() != null) {
                            lastMatchScheduledAt = match.getScheduledAt();
                        }

                        allMatches.add(match);
                    }
                }
            }
        }

        if (CollectionUtils.isEmpty(allMatches)) {
            log.info("OOP中无比赛数据");
            return;
        }

        this.saveMatches(allMatches);
    }

    public void saveMatches(List<Match> matches) {
        if (CollectionUtils.isEmpty(matches)) {
            return;
        }
        List<TennisMatchPO> matchPOs = MatchAppConvertMapper.INSTANCE.toMatchPOList(matches);
        tennisMatchRepository.saveOrUpdateBatch(matchPOs);

        // 保存 SetScore 数据
        saveSetScores(matches);
    }

    /**
     * 更新进行中的比赛：时长、状态、盘分、场地、球员
     */
    public void updateLiveMatches(List<MatchesResponse.MatchInfo> matches) {
        if (CollectionUtils.isEmpty(matches)) {
            return;
        }

        List<Match> allMatches = new ArrayList<>();
        List<TennisSetScorePO> allSetScores = new ArrayList<>();

        for (MatchesResponse.MatchInfo info : matches) {
            Match match = new Match();
            match.setMatchId(info.getMatchId());
            match.setTournamentId(info.getTournamentId() != null ? String.valueOf(info.getTournamentId()) : null);
            match.setYear(info.getTournamentYear());
            match.setPlayer1Id(info.getPlayerTeam1() != null ? info.getPlayerTeam1().getPlayerId() : null);
            match.setPlayer2Id(info.getPlayerTeam2() != null ? info.getPlayerTeam2().getPlayerId() : null);
            match.setStatus(MatchStatus.toStatus(info.getStatus()));
            match.setCourt(info.getCourtName());
            match.setDurationMinutes(parseDuration(info.getMatchTime()));
            match.setCourtSeq(info.getCourtSeq());
            allMatches.add(match);

            // 提取盘分：合并两个 PlayerTeam 同一 SetNumber 的数据
            allSetScores.addAll(buildSetScores(info, match.getTournamentId(), match.getYear()));
        }

        // 更新比赛记录
        List<TennisMatchPO> matchPOs = MatchAppConvertMapper.INSTANCE.toMatchPOList(allMatches);
        List<TennisMatchPO> toUpdate = fillIdForUpdate(matchPOs);
        if (CollectionUtils.isNotEmpty(toUpdate)) {
            tennisMatchRepository.updateBatchById(toUpdate);
        }

        // 更新盘分
        if (CollectionUtils.isNotEmpty(allSetScores)) {
            tennisSetScoreRepository.saveOrUpdateBatch(allSetScores);
        }

        log.info("进行中比赛更新完成: 比赛={}, 盘分={}", allMatches.size(), allSetScores.size());
    }

    public void updateMatches(List<Match> matches) {
        if (CollectionUtils.isEmpty(matches)) {
            return;
        }
        List<TennisMatchPO> matchPOs = MatchAppConvertMapper.INSTANCE.toMatchPOList(matches);
        List<TennisMatchPO> toUpdate = fillIdForUpdate(matchPOs);
        if (CollectionUtils.isNotEmpty(toUpdate)) {
            tennisMatchRepository.updateBatchById(toUpdate);
            log.info("更新已有比赛: {}条", toUpdate.size());
        }
    }

    /**
     * 按业务唯一键 (matchId, tournamentId, year) 查出已存在记录，
     * 把数据库主键 id 回填到待更新的 PO 上。
     * updateBatchById 走的是 @TableId 标注的 id 字段，不回填 id 时 SQL 不会命中任何行。
     */
    private List<TennisMatchPO> fillIdForUpdate(List<TennisMatchPO> matchPOs) {
        // 收集所有非空的 matchId 作为查询条件，先用 matchId 粗筛缩小结果集
        List<String> matchIds = matchPOs.stream()
                .map(TennisMatchPO::getMatchId).filter(Objects::nonNull).distinct().toList();
        if (CollectionUtils.isEmpty(matchIds)) {
            return List.of();
        }
        // matchId 在不同赛事/年份下会重复，必须用 (matchId, tournamentId, year) 三元组作为唯一键
        Map<String, Long> keyToId = tennisMatchRepository.lambdaQuery(matchIds)
                .stream()
                .collect(Collectors.toMap(
                        MatchCollectService::uniqueKey,
                        TennisMatchPO::getId,
                        (a, b) -> a));

        // 仅保留库里已存在的记录并回填 id，否则 updateBatchById 因 id=null 而失效
        return matchPOs.stream()
                .filter(m -> keyToId.containsKey(uniqueKey(m)))
                .peek(m -> m.setId(keyToId.get(uniqueKey(m))))
                .toList();
    }

    /**
     * 业务唯一键：matchId + tournamentId + year，与 tennis_match 表的唯一索引保持一致
     */
    private static String uniqueKey(TennisMatchPO po) {
        return po.getMatchId() + "|" + po.getTournamentId() + "|" + po.getYear();
    }

    private void saveSetScores(List<Match> matches) {
        List<TennisSetScorePO> allSetScores = new ArrayList<>();
        for (Match match : matches) {
            if (CollectionUtils.isEmpty(match.getSets()) || match.getMatchId() == null) {
                continue;
            }
            for (SetScore setScore : match.getSets()) {
                TennisSetScorePO po = new TennisSetScorePO();
                po.setMatchId(match.getMatchId());
                po.setTournamentId(match.getTournamentId());
                po.setYear(match.getYear());
                po.setSetNumber(setScore.getSetNumber());
                po.setP1Games(setScore.getP1Games());
                po.setP2Games(setScore.getP2Games());
                po.setP1Tiebreak(setScore.getP1Tiebreak());
                po.setP2Tiebreak(setScore.getP2Tiebreak());
                allSetScores.add(po);
            }
        }
        tennisSetScoreRepository.saveOrUpdateBatch(allSetScores);
    }

    /**
     * 解析 "HH:MM:SS" 格式的比赛时长为分钟数
     */
    private Integer parseDuration(String matchTime) {
        if (matchTime == null || matchTime.isEmpty()) return null;
        try {
            String[] parts = matchTime.split(":");
            if (parts.length == 3) {
                return Integer.parseInt(parts[0].trim()) * 60 + Integer.parseInt(parts[1].trim())
                        + (Integer.parseInt(parts[2].trim()) >= 30 ? 1 : 0);
            }
            // 格式不是 HH:MM:SS（段数不对或含全角冒号）时打日志，避免静默返回 null
            log.warn("解析比赛时长格式异常: value=[{}], len={}, codePoints={}",
                    matchTime, matchTime.length(), matchTime.chars().boxed().toList());
        } catch (NumberFormatException e) {
            log.warn("解析比赛时长失败: value=[{}]", matchTime, e);
        }
        return null;
    }

    /**
     * 合并两个 PlayerTeam 的盘分数据，按 SetNumber 对齐
     */
    private List<TennisSetScorePO> buildSetScores(MatchesResponse.MatchInfo info,
                                                   String tournamentId, Integer year) {
        List<TennisSetScorePO> scores = new ArrayList<>();
        if (info.getPlayerTeam1() == null || info.getPlayerTeam2() == null) return scores;

        List<MatchesResponse.SetInfo> sets1 = info.getPlayerTeam1().getSets();
        List<MatchesResponse.SetInfo> sets2 = info.getPlayerTeam2().getSets();
        if (CollectionUtils.isEmpty(sets1)) return scores;

        for (MatchesResponse.SetInfo s1 : sets1) {
            TennisSetScorePO po = new TennisSetScorePO();
            po.setMatchId(info.getMatchId());
            po.setTournamentId(tournamentId);
            po.setYear(year);
            po.setSetNumber(s1.getSetNumber());
            po.setP1Games(s1.getSetScore() != null ? Integer.parseInt(s1.getSetScore()) : 0);
            po.setP1Tiebreak(s1.getTieBreakScore() != null ? Integer.parseInt(s1.getTieBreakScore()) : null);

            // 查找 PlayerTeam2 同一盘的数据
            if (CollectionUtils.isNotEmpty(sets2)) {
                sets2.stream()
                        .filter(s2 -> s2.getSetNumber() != null && s2.getSetNumber().equals(s1.getSetNumber()))
                        .findFirst()
                        .ifPresent(s2 -> {
                            po.setP2Games(s2.getSetScore() != null ? Integer.parseInt(s2.getSetScore()) : 0);
                            po.setP2Tiebreak(s2.getTieBreakScore() != null ? Integer.parseInt(s2.getTieBreakScore()) : null);
                        });
            }
            scores.add(po);
        }
        return scores;
    }

    private String generateMatchId(String tournamentId, Long drawId, Integer roundNumber,
                                   String Player1Id, String Player2Id) {
        // 使用组合键生成唯一 ID: D_{drawId}_{roundNumber}_{Player1Id}_{Player2Id}
        StringBuilder sb = new StringBuilder();
        sb.append("D");
        if (drawId != null) sb.append(drawId);
        if (roundNumber != null) sb.append("R").append(roundNumber);
        if (Player1Id != null) sb.append("P").append(Player1Id);
        if (Player2Id != null) sb.append("p").append(Player2Id);
        return sb.toString();
    }

    public void atpFromLive() {
        MatchesResponse response = tennisTvClient.getMatchesByStatus("L");
        if (response == null || CollectionUtils.isEmpty(response.getMatches())) {
            log.info("无进行中的比赛");
            return;
        }
        this.updateLiveMatches(response.getMatches());
    }

    public void wtaFromLive(List<WtaMatchesResponse.MatchItem> matches) {
        if (CollectionUtils.isEmpty(matches)) return;

        List<Match> allMatches = new ArrayList<>();
        for (WtaMatchesResponse.MatchItem m : matches) {
            Match match = new Match();
            match.setMatchId(m.getMatchID());
            match.setTournamentId(m.getEventID());
            match.setYear(m.getEventYear());
            match.setStatus(MatchStatus.toStatus(m.getMatchState()));
            match.setDurationMinutes(parseDuration(m.getMatchTimeTotal()));
            match.setWinnerId(resolveWtaWinner(m.getWinner(), m.getPlayerIDA(), m.getPlayerIDB()));
            match.setSets(parseWtaSets(m));
            allMatches.add(match);
        }

        List<TennisMatchPO> matchPOs = MatchAppConvertMapper.INSTANCE.toMatchPOList(allMatches);
        List<TennisMatchPO> toUpdate = fillIdForUpdate(matchPOs);
        if (CollectionUtils.isNotEmpty(toUpdate)) {
            tennisMatchRepository.updateBatchById(toUpdate);
        }

        Set<String> existingMatchIds = toUpdate.stream()
                .map(TennisMatchPO::getMatchId)
                .collect(Collectors.toSet());
        List<Match> existingMatches = allMatches.stream()
                .filter(m -> existingMatchIds.contains(m.getMatchId()))
                .toList();
        saveSetScores(existingMatches);

        log.info("WTA进行中比赛更新完成: 比赛={}", toUpdate.size());
    }



    private String resolveWtaWinner(String winner, String playerIDA, String playerIDB) {
        if (winner == null || winner.isEmpty()) return null;
        try {
            int w = Integer.parseInt(winner);
            if (w == 0) return null;
            return w % 2 == 0 ? playerIDA : playerIDB;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void wtaFromOop(String tournamentId, int year) {
        WtaMatchesResponse response = wtaClient.getMatches(tournamentId, year);

        List<WtaMatchesResponse.MatchItem> matches = response.getMatches().stream()
                .filter(m -> "M".equals(m.getDrawLevelType()) && "S".equals(m.getDrawMatchType()))
                .toList();

        if (CollectionUtils.isEmpty(matches)) return;


        List<Match> allMatches = new ArrayList<>();
        for (WtaMatchesResponse.MatchItem m : matches) {
            Match match = new Match();
            match.setMatchId(m.getMatchID());
            match.setTournamentId(tournamentId);
            match.setYear(year);


            match.setPlayer1Id(m.getPlayerIDA());
            match.setPlayer2Id(m.getPlayerIDB());
            match.setWinnerId(resolveWtaWinner(m.getWinner(), m.getPlayerIDA(), m.getPlayerIDB()));
            match.setStatus(MatchStatus.toStatus(m.getMatchState()));
            match.setDurationMinutes(parseDuration(m.getMatchTimeTotal()));
            match.setCourt(m.getCourtName());
            match.setScheduledAt(OopMatchAppConvertMapper.INSTANCE.parseScheduledAt(m.getMatchTimeStamp(), m.getNotBeforeISOTime()));
            match.setScheduledAtText(com.rally.domain.tennis.model.ScheduledAtTextEnum.fromText(m.getNotBeforeText()));

            if (m.getMatchTimeStamp() != null && !m.getMatchTimeStamp().isEmpty()) {
                try {
                    java.time.LocalDateTime ts = OffsetDateTime.parse(m.getMatchTimeStamp()).toLocalDateTime();
                    match.setStartedAt(ts);
                    if ("F".equals(m.getMatchState())) {
                        match.setEndedAt(ts);
                    }
                    match.setMatchDate(ts.toLocalDate());
                } catch (Exception ignored) {}
            }

            match.setSets(parseWtaSets(m));
            allMatches.add(match);
        }
        updateMatches(allMatches);
    }



    private List<SetScore> parseWtaSets(WtaMatchesResponse.MatchItem m) {
        List<SetScore> sets = new ArrayList<>();
        String[][] setData = {
            {m.getScoreSet1A(), m.getScoreSet1B(), m.getScoreTbSet1()},
            {m.getScoreSet2A(), m.getScoreSet2B(), m.getScoreTbSet2()},
            {m.getScoreSet3A(), m.getScoreSet3B(), m.getScoreTbSet3()},
            {m.getScoreSet4A(), m.getScoreSet4B(), m.getScoreTbSet4()},
            {m.getScoreSet5A(), m.getScoreSet5B(), m.getScoreTbSet5()},
        };
        for (int i = 0; i < setData.length; i++) {
            String a = setData[i][0], b = setData[i][1], tb = setData[i][2];
            if (a == null || a.isEmpty()) break;
            SetScore s = new SetScore();
            s.setSetNumber(i + 1);
            try { s.setP1Games(Integer.parseInt(a)); } catch (NumberFormatException ignored) {}
            try { s.setP2Games(Integer.parseInt(b)); } catch (NumberFormatException ignored) {}
            if (tb != null && !tb.isEmpty()) {
                try { s.setP1Tiebreak(Integer.parseInt(tb)); } catch (NumberFormatException ignored) {}
            }
            sets.add(s);
        }
        return sets.isEmpty() ? null : sets;
    }
}
