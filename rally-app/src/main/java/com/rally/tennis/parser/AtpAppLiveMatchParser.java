package com.rally.tennis.parser;

import com.rally.client.atp.AtpClient;
import com.rally.client.atp.model.AtpAppLiveResponse;
import com.rally.tennis.model.Discipline;
import com.rally.tennis.model.Match;
import com.rally.tennis.model.MatchStatus;
import com.rally.tennis.model.Player;
import com.rally.tennis.model.SetScore;
import com.rally.tennis.model.TournamentEntry;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * ATP App 实时比赛解析器（男子单打 MS）
 * 数据源：https://app.atptour.com/api/v2/gateway/livematches
 * 每次请求对应一个赛事，按 eventId + eventYear 拉取
 */
@Component
public class AtpAppLiveMatchParser extends MatchParser<AtpAppLiveResponse, List<AtpAppLiveResponse.LiveMatch>> {

    @Resource
    private AtpClient atpClient;

    /**
     * 调用 AtpClient 获取指定赛事的实时比赛数据
     * DrawParams 中 tournamentId 对应 eventId，year 对应 eventYear
     */
    @Override
    protected AtpAppLiveResponse request(DrawParams params) {
        return atpClient.getLiveMatches(params.getTournamentId(), params.getYear());
    }

    /**
     * 从响应中提取男子单打（MS）比赛列表，过滤 MatchId 不以 "MS" 开头的条目
     */
    @Override
    protected List<DrawResult<List<AtpAppLiveResponse.LiveMatch>>> ms(AtpAppLiveResponse data, DrawParams params) {
        return buildDrawResult(data, params, "MS", Discipline.SINGLES);
    }

    /**
     * 构建 DrawResult 的通用方法，供子类复用
     * @param data       原始响应
     * @param params     请求参数（tournamentId / year）
     * @param prefix     MatchId 前缀过滤，如 "MS"、"LS"
     * @param discipline 赛事类型
     */
    protected List<DrawResult<List<AtpAppLiveResponse.LiveMatch>>> buildDrawResult(
            AtpAppLiveResponse data, DrawParams params, String prefix, Discipline discipline) {
        if (data == null || data.getData() == null
                || CollectionUtils.isEmpty(data.getData().getLiveMatches())) {
            return List.of();
        }
        // 按 MatchId 前缀过滤，只保留指定类型的比赛
        List<AtpAppLiveResponse.LiveMatch> filtered = data.getData().getLiveMatches().stream()
                .filter(m -> m.getMatchId() != null && m.getMatchId().startsWith(prefix))
                .toList();
        if (filtered.isEmpty()) return List.of();

        return List.of(new DrawResult<>(
                filtered,
                discipline,
                prefix,
                new DrawMeta(null, null),
                params.getTournamentId(),
                params.getYear()
        ));
    }

    /**
     * 将 LiveMatch 列表转换为 Match 领域对象
     * SetNumber=0 的盘数据为无效数据，在 buildSets 中过滤
     */
    @Override
    public List<Match> getMatches(DrawResult<List<AtpAppLiveResponse.LiveMatch>> draw,
                                  String tournamentId, Long drawId) {
        List<Match> matches = new ArrayList<>();
        for (AtpAppLiveResponse.LiveMatch lm : draw.getSlice()) {
            Match match = new Match();
            match.setMatchId(lm.getMatchId());
            match.setTournamentId(tournamentId);
            match.setYear(draw.getYear());
            match.setDrawId(drawId);
            // PlayerTeam 对应 player1，OpponentTeam 对应 player2
            match.setPlayer1Id(lm.getPlayerTeam() != null && lm.getPlayerTeam().getPlayer() != null
                    ? lm.getPlayerTeam().getPlayer().getPlayerId() : null);
            match.setPlayer2Id(lm.getOpponentTeam() != null && lm.getOpponentTeam().getPlayer() != null
                    ? lm.getOpponentTeam().getPlayer().getPlayerId() : null);
            match.setStatus(MatchStatus.toStatus(lm.getMatchStatus()));
            match.setCourt(lm.getCourtName());
            match.setWinnerId(lm.getWinningPlayerId());
            match.setSets(buildSets(lm));
            matches.add(match);
        }
        return matches;
    }

    @Override
    public List<Player> getPlayers(DrawResult<List<AtpAppLiveResponse.LiveMatch>> draw) {
        return List.of();
    }

    @Override
    public List<TournamentEntry> getEntries(DrawResult<List<AtpAppLiveResponse.LiveMatch>> draw, Long drawId) {
        return List.of();
    }

    /**
     * 解析比分：将 PlayerTeam 和 OpponentTeam 的 SetScores 合并为 SetScore 列表
     * 过滤 SetNumber=0 的无效盘数据，以 PlayerTeam 的盘数为基准进行配对
     */
    private List<SetScore> buildSets(AtpAppLiveResponse.LiveMatch lm) {
        if (lm.getPlayerTeam() == null || lm.getOpponentTeam() == null) return null;
        List<AtpAppLiveResponse.SetScoreInfo> sets1 = lm.getPlayerTeam().getSetScores();
        List<AtpAppLiveResponse.SetScoreInfo> sets2 = lm.getOpponentTeam().getSetScores();
        if (CollectionUtils.isEmpty(sets1)) return null;

        List<SetScore> result = new ArrayList<>();
        for (AtpAppLiveResponse.SetScoreInfo s1 : sets1) {
            // SetNumber=0 是占位数据，跳过
            if (s1.getSetNumber() == null || s1.getSetNumber() == 0) continue;
            // SetScore 为 null 表示该盘尚未开始，跳过
            if (s1.getSetScore() == null) continue;

            SetScore ss = new SetScore();
            ss.setSetNumber(s1.getSetNumber());
            ss.setP1Games(s1.getSetScore());
            ss.setP1Tiebreak(s1.getTieBreakScore());

            // 从 OpponentTeam 中找到对应盘号的比分
            if (CollectionUtils.isNotEmpty(sets2)) {
                sets2.stream()
                        .filter(s2 -> s2.getSetNumber() != null
                                && s2.getSetNumber().equals(s1.getSetNumber())
                                && s2.getSetScore() != null)
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

    @Override
    public CollectType collectType() {
        return CollectType.ATP_APP_LIVE;
    }
}
