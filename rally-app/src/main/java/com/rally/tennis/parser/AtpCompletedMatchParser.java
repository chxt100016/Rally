package com.rally.tennis.parser;

import com.rally.client.atp.AtpClient;
import com.rally.client.atp.model.AtpAppCompletedResponse;
import com.rally.domain.tennis.model.TennisRoundEnum;
import com.rally.tennis.model.*;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ATP App 已完成比赛解析器（男子单打 MS）
 * 数据源：app.atptour.com/api/v2/gateway/results/completed
 */
@Component
public class AtpCompletedMatchParser extends MatchParser<AtpAppCompletedResponse, AtpAppCompletedResponse> {

    @Resource
    private AtpClient atpClient;

    @Override
    protected AtpAppCompletedResponse request(DrawParams params) {
        return atpClient.getCompleted(params.getTournamentId(), params.getYear());
    }

    @Override
    protected List<DrawResult<AtpAppCompletedResponse>> ms(AtpAppCompletedResponse data, DrawParams params) {
        if (data == null || data.getData() == null) return List.of();
        List<AtpAppCompletedResponse.Match> filtered = filterByPrefix(data.getData().getMatches(), "MS");
        if (filtered.isEmpty()) return List.of();
        return List.of(new DrawResult<>(buildFiltered(data, filtered), Discipline.SINGLES, "MS",
                new DrawMeta(null, null), params.getTournamentId(), params.getYear()));
    }

    @Override
    protected List<DrawResult<AtpAppCompletedResponse>> ls(AtpAppCompletedResponse data, DrawParams params) {
        if (data == null || data.getData() == null) return List.of();
        List<AtpAppCompletedResponse.Match> filtered = filterByPrefix(data.getData().getMatches(), "LS");
        if (filtered.isEmpty()) return List.of();
        return List.of(new DrawResult<>(buildFiltered(data, filtered), Discipline.SINGLES, "LS",
                new DrawMeta(null, null), params.getTournamentId(), params.getYear()));
    }

    @Override
    public List<Match> getMatches(DrawResult<AtpAppCompletedResponse> draw, String tournamentId, Long drawId) {
        AtpAppCompletedResponse data = draw.getSlice();
        if (data == null || data.getData() == null
                || CollectionUtils.isEmpty(data.getData().getMatches())) {
            return List.of();
        }
        // 按 drawTypeCode 前缀过滤，MS 签表只取 matchId 以 MS 开头的，LS 同理
        String prefix = draw.getDrawTypeCode();
        List<Match> result = new ArrayList<>();
        for (AtpAppCompletedResponse.Match m : data.getData().getMatches()) {
            if (m.getMatchId() == null || !m.getMatchId().startsWith(prefix)) continue;

            Match match = new Match();
            match.setMatchId(m.getMatchId());
            match.setTournamentId(tournamentId);
            match.setDrawId(drawId);
            match.setYear(draw.getYear());
            match.setStatus(MatchStatus.toStatus(m.getStatus()));
            match.setWinnerId(m.getWinningPlayerId());
            match.setCourt(m.getCourtName());
            match.setMatchDate(parseDate(m.getMatchDate()));

            if (m.getRound() != null) {
                match.setRoundNumber(parseRoundId(m.getRound().getRoundId()));
                match.setRoundName(TennisRoundEnum.of(m.getRound().getLongName()));
            }

            if (m.getPlayerTeam() != null && m.getPlayerTeam().getPlayer() != null) {
                AtpAppCompletedResponse.PlayerInfo p = m.getPlayerTeam().getPlayer();
                match.setPlayer1Id(p.getPlayerId());
                match.setPlayerName1(buildFullName(p.getPlayerFirstName(), p.getPlayerLastName()));
            }
            if (m.getOpponentTeam() != null && m.getOpponentTeam().getPlayer() != null) {
                AtpAppCompletedResponse.PlayerInfo p = m.getOpponentTeam().getPlayer();
                match.setPlayer2Id(p.getPlayerId());
                match.setPlayerName2(buildFullName(p.getPlayerFirstName(), p.getPlayerLastName()));
            }

            match.setSets(buildSets(m));
            result.add(match);
        }
        return result;
    }

    @Override
    public List<Player> getPlayers(DrawResult<AtpAppCompletedResponse> draw) {
        return List.of();
    }

    @Override
    public List<TournamentEntry> getEntries(DrawResult<AtpAppCompletedResponse> draw, Long drawId) {
        return List.of();
    }

    @Override
    public CollectType collectType() {
        return CollectType.ATP_APP_COMPLETED;
    }

    /** 按 matchId 前缀过滤比赛列表 */
    private List<AtpAppCompletedResponse.Match> filterByPrefix(List<AtpAppCompletedResponse.Match> matches, String prefix) {
        if (CollectionUtils.isEmpty(matches)) return List.of();
        return matches.stream()
                .filter(m -> m.getMatchId() != null && m.getMatchId().startsWith(prefix))
                .collect(Collectors.toList());
    }

    /** 用过滤后的 matches 构造新的 AtpAppCompletedResponse，避免污染原始数据 */
    private AtpAppCompletedResponse buildFiltered(AtpAppCompletedResponse original, List<AtpAppCompletedResponse.Match> matches) {
        AtpAppCompletedResponse.DataWrapper wrapper = new AtpAppCompletedResponse.DataWrapper();
        wrapper.setMatches(matches);
        AtpAppCompletedResponse result = new AtpAppCompletedResponse();
        result.setData(wrapper);
        return result;
    }

    /** 合并 PlayerTeam 和 OpponentTeam 的 SetScores，过滤 SetNumber=0 的无效数据 */
    private List<SetScore> buildSets(AtpAppCompletedResponse.Match m) {
        if (m.getPlayerTeam() == null || m.getOpponentTeam() == null) return null;
        List<AtpAppCompletedResponse.SetScoreInfo> sets1 = m.getPlayerTeam().getSetScores();
        List<AtpAppCompletedResponse.SetScoreInfo> sets2 = m.getOpponentTeam().getSetScores();
        if (CollectionUtils.isEmpty(sets1)) return null;

        List<SetScore> result = new ArrayList<>();
        for (AtpAppCompletedResponse.SetScoreInfo s1 : sets1) {
            // SetNumber=0 是占位数据，跳过；SetScore 为 null 表示该盘未进行
            if (s1.getSetNumber() == null || s1.getSetNumber() == 0 || s1.getSetScore() == null) continue;

            SetScore ss = new SetScore();
            ss.setSetNumber(s1.getSetNumber());
            ss.setP1Games(s1.getSetScore());
            ss.setP1Tiebreak(s1.getTieBreakScore());

            // 从 OpponentTeam 中找对应盘号的比分
            if (CollectionUtils.isNotEmpty(sets2)) {
                sets2.stream()
                        .filter(s2 -> s2.getSetNumber() != null && s2.getSetNumber().equals(s1.getSetNumber()))
                        .findFirst()
                        .ifPresent(s2 -> {
                            ss.setP2Games(s2.getSetScore());
                            ss.setP2Tiebreak(s2.getTieBreakScore());
                        });
            }
            result.add(ss);
        }
        return result.isEmpty() ? null : result;
    }

    private void addPlayer(Map<String, Player> map, AtpAppCompletedResponse.TeamInfo team) {
        if (team == null || team.getPlayer() == null || team.getPlayer().getPlayerId() == null) return;
        AtpAppCompletedResponse.PlayerInfo pi = team.getPlayer();
        map.computeIfAbsent(pi.getPlayerId(), id -> {
            Player p = new Player();
            p.setPlayerId(id);
            p.setFirstName(pi.getPlayerFirstName());
            p.setLastName(pi.getPlayerLastName());
            return p;
        });
    }

    private String buildFullName(String firstName, String lastName) {
        if (firstName == null && lastName == null) return null;
        StringBuilder sb = new StringBuilder();
        if (firstName != null) sb.append(firstName);
        if (lastName != null) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(lastName);
        }
        return sb.toString();
    }

    /** 解析 "2026-05-24T00:00:00" 格式的日期 */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.length() < 10) return null;
        try {
            return LocalDate.parse(dateStr.substring(0, 10));
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseRoundId(String roundId) {
        if (roundId == null) return null;
        try {
            return Integer.parseInt(roundId);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
