package com.rally.domain.tennis;

import com.rally.domain.tennis.convert.MatchConvertMapper;
import com.rally.domain.tennis.gateway.MatchQueryGateway;
import com.rally.domain.tennis.gateway.TennisTournamentGateway;
import com.rally.domain.tennis.model.*;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class MatchDataLoader {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Resource
    private MatchQueryGateway matchQueryGateway;

    @Resource
    private TennisTournamentGateway tennisTournamentGateway;

    public static List<String> parseTournamentIds(String tournamentIdStr) {
        if (tournamentIdStr == null || tournamentIdStr.isBlank()) return List.of();
        return Arrays.stream(tournamentIdStr.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public MatchLoadResult loadAndSplit(List<String> tournamentIds) {
        MatchLoadResult result = new MatchLoadResult();

        List<MatchData> matches = matchQueryGateway.listByTournamentIds(tournamentIds);
        if (CollectionUtils.isEmpty(matches)) {
            result.empty = true;
            return result;
        }

        matches = matches.stream().filter(m -> m.getPlayer1Id() != null || m.getPlayer2Id() != null).toList();
        if (CollectionUtils.isEmpty(matches)) {
            result.empty = true;
            return result;
        }

        Set<String> playerIds = new HashSet<>();
        for (MatchData match : matches) {
            if (match.getPlayer1Id() != null) playerIds.add(match.getPlayer1Id());
            if (match.getPlayer2Id() != null) playerIds.add(match.getPlayer2Id());
        }

        result.seeds = matchQueryGateway.listSeedsByTournamentIds(tournamentIds);
        Map<String, Integer> seedMap = result.seeds.stream().collect(Collectors.toMap(s -> s.getTournamentId() + ":" + s.getPlayerId(), PlayerSeedData::getSeed, (a, b) -> a));

        result.tournamentTourMap = tennisTournamentGateway.listByTournamentIds(tournamentIds).stream().collect(Collectors.toMap(TournamentData::getTournamentId, data -> data.getTour() != null ? data.getTour() : "", (a, b) -> a));

        result.seeds.stream().map(PlayerSeedData::getPlayerId).forEach(playerIds::add);
        List<PlayerData> players = matchQueryGateway.listPlayersByPlayerIds(new ArrayList<>(playerIds));
        result.playerMap = players.stream().collect(Collectors.toMap(PlayerData::getPlayerId, p -> p, (a, b) -> a));

        List<Long> tennisMatchIds = matches.stream().map(MatchData::getTennisMatchId).filter(Objects::nonNull).toList();
        List<SetScoreData> setScores = matchQueryGateway.listSetScoresByTennisMatchIds(tennisMatchIds);
        Map<Long, List<SetScoreData>> setScoreMap = setScores.stream().collect(Collectors.groupingBy(SetScoreData::getTennisMatchId));

        for (MatchData match : matches) {
            MatchQueryVO vo = toMatchVO(match, result.playerMap, setScoreMap, seedMap);
            if ("FINISHED".equals(vo.getStatus())) {
                result.finishedMatches.add(vo);
            } else {
                result.upcomingMatches.add(vo);
            }
        }

        Set<String> upcomingDates = new HashSet<>();
        for (MatchQueryVO m : result.upcomingMatches) {
            if (m.getDate() != null) upcomingDates.add(m.getDate());
        }
        if (!upcomingDates.isEmpty()) {
            Iterator<MatchQueryVO> it = result.finishedMatches.iterator();
            while (it.hasNext()) {
                MatchQueryVO m = it.next();
                if (upcomingDates.contains(m.getDate())) {
                    it.remove();
                    result.upcomingMatches.add(m);
                }
            }
        }

        return result;
    }

    private MatchQueryVO toMatchVO(MatchData match, Map<String, PlayerData> playerMap, Map<Long, List<SetScoreData>> setScoreMap, Map<String, Integer> seedMap) {
        MatchQueryVO vo = new MatchQueryVO();
        vo.setId(match.getMatchId());
        vo.setTournamentId(match.getTournamentId());
        vo.setCourt(match.getCourt());
        vo.setCourtSeq(match.getCourtSeq());
        vo.setRound(match.getRoundName());
        vo.setRoundLabel(TennisRoundEnum.labelOf(match.getRoundName()));
        vo.setSchedulingType(match.getScheduledAtText());
        vo.setDate(match.getMatchDate() != null ? match.getMatchDate().format(DATE_FMT) : null);
        vo.setStartedAt(match.getStartedAt());
        vo.setScheduledAt(match.getScheduledAt());
        if (match.getScheduledAt() != null) {
            vo.setScheduledTime(match.getScheduledAt().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        vo.setPlayer1(buildPlayerVO(match.getPlayer1Id(), match.getTournamentId(), playerMap, seedMap));
        vo.setPlayer2(buildPlayerVO(match.getPlayer2Id(), match.getTournamentId(), playerMap, seedMap));
        List<SetScoreData> setScoreList = setScoreMap.getOrDefault(match.getTennisMatchId(), List.of());
        List<SetScoreVO> sets = setScoreList.stream().map(MatchConvertMapper.INSTANCE::toSetScoreVO).toList();
        vo.setSets(sets);
        vo.setStatus(match.getStatus());
        vo.setCurrentSet(CollectionUtils.isEmpty(sets) ? null : sets.size());
        vo.setCurrentSetScore(buildCurrentSetScore(sets));
        vo.setWinnerId(match.getWinnerId());
        vo.setDuration(calculateDuration(match));
        return vo;
    }

    private PlayerVO buildPlayerVO(String playerId, String tournamentId, Map<String, PlayerData> playerMap, Map<String, Integer> seedMap) {
        if (playerId == null) return null;
        PlayerData player = playerMap.get(playerId);
        if (player == null) {
            PlayerVO vo = new PlayerVO();
            vo.setId(playerId);
            vo.setName(playerId);
            return vo;
        }
        PlayerVO vo = new PlayerVO();
        vo.setId(player.getPlayerId());
        vo.setName(StringUtils.isNotBlank(player.getLastName()) ? player.getLastName() : player.getFirstName());
        vo.setCountry(CountryEnum.getCountry(player.getNationality()));
        vo.setSeed(seedMap.getOrDefault(tournamentId + ":" + playerId, null));
        return vo;
    }

    private String buildCurrentSetScore(List<SetScoreVO> sets) {
        if (CollectionUtils.isEmpty(sets)) return null;
        SetScoreVO lastSet = sets.get(sets.size() - 1);
        if (lastSet.getPlayer1() == null || lastSet.getPlayer2() == null) return null;
        return lastSet.getPlayer1() + "-" + lastSet.getPlayer2();
    }

    private String calculateDuration(MatchData match) {
        if (match.getDurationMinutes() != null) {
            int hours = match.getDurationMinutes() / 60;
            int mins = match.getDurationMinutes() % 60;
            if (hours > 0) return hours + "h" + (mins > 0 ? mins + "m" : "");
            return mins + "m";
        }
        if ("live".equals(match.getStatus()) && match.getStartedAt() != null) {
            long minutes = java.time.Duration.between(match.getStartedAt(), LocalDateTime.now()).toMinutes();
            if (minutes >= 0) {
                int hours = (int) (minutes / 60);
                int mins = (int) (minutes % 60);
                if (hours > 0) return "已开始 " + hours + "h" + (mins > 0 ? mins + "m" : "");
                return "已开始 " + mins + "m";
            }
        }
        return null;
    }

    public static class MatchLoadResult {
        public boolean empty;
        public List<MatchQueryVO> upcomingMatches = new ArrayList<>();
        public List<MatchQueryVO> finishedMatches = new ArrayList<>();
        public List<PlayerSeedData> seeds = new ArrayList<>();
        public Map<String, String> tournamentTourMap = new HashMap<>();
        public Map<String, PlayerData> playerMap = new HashMap<>();
    }
}
