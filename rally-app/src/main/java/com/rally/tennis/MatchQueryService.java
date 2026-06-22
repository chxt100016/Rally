package com.rally.tennis;

import com.rally.domain.tennis.gateway.MatchQueryGateway;
import com.rally.domain.tennis.gateway.TennisTournamentGateway;
import com.rally.domain.tennis.model.*;
import com.rally.domain.translation.model.TranslationLanguageEnum;
import com.rally.tennis.convert.MatchConvertMapper;
import com.rally.translation.TennisTranslationService;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MatchQueryService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Resource
    private MatchQueryGateway matchQueryGateway;

    @Resource
    private TennisTournamentGateway tennisTournamentGateway;

    @Resource
    private TennisTranslationService tennisTranslationService;

    public MatchQueryResponse queryMatches(String tournamentIdStr) {
        List<String> tournamentIds = parseTournamentIds(tournamentIdStr);
        if (CollectionUtils.isEmpty(tournamentIds)) {
            return emptyMatchResponse();
        }

        MatchLoadResult loaded = loadAndSplit(tournamentIds);
        if (loaded.empty) {
            return emptyMatchResponse();
        }

        Map<String, String> eliminatedRoundMap = new HashMap<>();
        for (MatchQueryVO m : loaded.finishedMatches) {
            if (m.getWinnerId() == null) continue;
            if (m.getPlayer1() != null && !m.getWinnerId().equals(m.getPlayer1().getId())) {
                eliminatedRoundMap.put(m.getPlayer1().getId(), m.getRound());
            }
            if (m.getPlayer2() != null && !m.getWinnerId().equals(m.getPlayer2().getId())) {
                eliminatedRoundMap.put(m.getPlayer2().getId(), m.getRound());
            }
        }

        List<SeedVO> seedVOList = loaded.seeds.stream()
                .filter(s -> s.getSeed() != null && s.getSeed() != 0)
                .map(s -> {
                    SeedVO seedVO = new SeedVO();
                    seedVO.setPlayerId(s.getPlayerId());
                    seedVO.setSeed(s.getSeed());
                    seedVO.setTournamentId(s.getTournamentId());
                    seedVO.setTour(loaded.tournamentTourMap.getOrDefault(s.getTournamentId(), ""));
                    PlayerData player = loaded.playerMap.get(s.getPlayerId());
                    if (player != null) {
                        String name = StringUtils.isNotBlank(player.getLastName()) ? player.getLastName() : player.getFirstName();
                        seedVO.setName(name);
                        seedVO.setCountry(CountryEnum.getCountry(player.getNationality()));
                    }
                    String eliminatedRound = eliminatedRoundMap.get(s.getPlayerId());
                    if (eliminatedRound != null) {
                        seedVO.setStatus(SeedStatusEnum.ELIMINATED);
                        seedVO.setLabel(TennisRoundEnum.labelOf(eliminatedRound));
                    } else {
                        seedVO.setStatus(SeedStatusEnum.ACTIVE);
                    }
                    return seedVO;
                })
                .sorted(Comparator.comparing(SeedVO::getSeed, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        loaded.upcomingMatches.sort(Comparator.comparing(MatchQueryVO::getScheduledAt, Comparator.nullsLast(Comparator.naturalOrder())));
        loaded.finishedMatches.sort(Comparator.comparing(MatchQueryVO::getStartedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        tennisTranslationService.matches(loaded.upcomingMatches, TranslationLanguageEnum.ZH_CN);
        tennisTranslationService.matches(loaded.finishedMatches, TranslationLanguageEnum.ZH_CN);
        tennisTranslationService.seeds(seedVOList, TranslationLanguageEnum.ZH_CN);

        Map<String, List<MatchQueryVO>> matchMap = new LinkedHashMap<>();
        matchMap.put("upcomingMatches", loaded.upcomingMatches);
        matchMap.put("finishedMatches", loaded.finishedMatches);

        MatchQueryResponse response = new MatchQueryResponse();
        response.setSeeds(seedVOList);
        response.setMatches(matchMap);
        return response;
    }

    public List<CourtMatchDTO> queryUpcomingByCourt(String tournamentIdStr) {
        List<String> tournamentIds = parseTournamentIds(tournamentIdStr);
        if (CollectionUtils.isEmpty(tournamentIds)) return List.of();

        MatchLoadResult loaded = loadAndSplit(tournamentIds);
        if (loaded.empty) return List.of();

        List<MatchQueryVO> upcomingMatches = loaded.upcomingMatches;
        upcomingMatches.sort(Comparator.comparing(MatchQueryVO::getScheduledAt, Comparator.nullsLast(Comparator.naturalOrder())));
        tennisTranslationService.matches(upcomingMatches, TranslationLanguageEnum.ZH_CN);

        Map<String, List<MatchQueryVO>> courtMap = new LinkedHashMap<>();
        for (MatchQueryVO m : upcomingMatches) {
            String key = m.getCourt() != null ? m.getCourt() : "";
            courtMap.computeIfAbsent(key, k -> new ArrayList<>()).add(m);
        }

        List<CourtMatchDTO> courts = new ArrayList<>();
        for (Map.Entry<String, List<MatchQueryVO>> entry : courtMap.entrySet()) {
            List<MatchQueryVO> courtMatches = entry.getValue();
            CourtMatchDTO dto = new CourtMatchDTO();
            dto.setCourt(courtMatches.get(0).getCourt());
            boolean hasAtp = courtMatches.stream().anyMatch(m -> "ATP".equalsIgnoreCase(loaded.tournamentTourMap.getOrDefault(m.getTournamentId(), "")));
            dto.setTour(hasAtp ? "ATP" : "WTA");
            dto.setMatches(courtMatches);
            courts.add(dto);
        }

        courts.sort((a, b) -> {
            Integer seedA = getMinSeed(a.getMatches());
            Integer seedB = getMinSeed(b.getMatches());
            if (!Objects.equals(seedA, seedB)) {
                if (seedA == null) return 1;
                if (seedB == null) return -1;
                return seedA - seedB;
            }
            return tourOrder(a.getTour()) - tourOrder(b.getTour());
        });

        return courts;
    }

    public List<MatchQueryVO> queryFinishedList(String tournamentIdStr) {
        List<String> tournamentIds = parseTournamentIds(tournamentIdStr);
        if (CollectionUtils.isEmpty(tournamentIds)) return List.of();

        MatchLoadResult loaded = loadAndSplit(tournamentIds);
        if (loaded.empty) return List.of();

        List<MatchQueryVO> finishedMatches = loaded.finishedMatches;
        finishedMatches.sort(Comparator.comparing(MatchQueryVO::getStartedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        tennisTranslationService.matches(finishedMatches, TranslationLanguageEnum.ZH_CN);
        return finishedMatches;
    }

    private MatchLoadResult loadAndSplit(List<String> tournamentIds) {
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

    private Integer getMinSeed(List<MatchQueryVO> matches) {
        Integer min = null;
        for (MatchQueryVO m : matches) {
            if (m.getPlayer1() != null && m.getPlayer1().getSeed() != null) {
                if (min == null || m.getPlayer1().getSeed() < min) min = m.getPlayer1().getSeed();
            }
            if (m.getPlayer2() != null && m.getPlayer2().getSeed() != null) {
                if (min == null || m.getPlayer2().getSeed() < min) min = m.getPlayer2().getSeed();
            }
        }
        return min;
    }

    private int tourOrder(String tour) {
        if ("ATP".equalsIgnoreCase(tour)) return 0;
        if ("WTA".equalsIgnoreCase(tour)) return 1;
        return 2;
    }

    private MatchQueryResponse emptyMatchResponse() {
        MatchQueryResponse response = new MatchQueryResponse();
        response.setSeeds(List.of());
        Map<String, List<MatchQueryVO>> emptyMatch = new LinkedHashMap<>();
        emptyMatch.put("upcomingMatches", List.of());
        emptyMatch.put("finishedMatches", List.of());
        response.setMatches(emptyMatch);
        return response;
    }

    private List<String> parseTournamentIds(String tournamentIdStr) {
        if (tournamentIdStr == null || tournamentIdStr.isBlank()) return List.of();
        return Arrays.stream(tournamentIdStr.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
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
        vo.setCurrentSet(calculateCurrentSet(sets));
        vo.setCurrentSetScore(calculateCurrentSetScore(sets));
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
        String name = StringUtils.isNotBlank(player.getLastName()) ? player.getLastName() : player.getFirstName();
        vo.setName(name);
        vo.setCountry(CountryEnum.getCountry(player.getNationality()));
        vo.setSeed(seedMap.getOrDefault(tournamentId + ":" + playerId, null));
        return vo;
    }

    private Integer calculateCurrentSet(List<SetScoreVO> sets) {
        return CollectionUtils.isEmpty(sets) ? null : sets.size();
    }

    private String calculateCurrentSetScore(List<SetScoreVO> sets) {
        if (CollectionUtils.isEmpty(sets)) return null;
        SetScoreVO lastSet = sets.get(sets.size() - 1);
        if (lastSet.getPlayer1() == null || lastSet.getPlayer2() == null) return null;
        return lastSet.getPlayer1() + "-" + lastSet.getPlayer2();
    }

    private String calculateDuration(MatchData match) {
        if (match.getDurationMinutes() != null) {
            int minutes = match.getDurationMinutes();
            int hours = minutes / 60;
            int mins = minutes % 60;
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

    private static class MatchLoadResult {
        boolean empty;
        List<MatchQueryVO> upcomingMatches = new ArrayList<>();
        List<MatchQueryVO> finishedMatches = new ArrayList<>();
        List<PlayerSeedData> seeds = new ArrayList<>();
        Map<String, String> tournamentTourMap = new HashMap<>();
        Map<String, PlayerData> playerMap = new HashMap<>();
    }
}
