package com.rally.protourdata.playertournamentpathquery.activity;

import com.rally.domain.tour.model.CountryEnum;
import com.rally.domain.tour.model.CountryVO;
import com.rally.domain.tour.model.MatchData;
import com.rally.domain.tour.model.MatchProgressVO;
import com.rally.domain.tour.model.PlayerData;
import com.rally.domain.tour.model.PlayerDetailData;
import com.rally.domain.tour.model.PlayerSeedData;
import com.rally.domain.tour.model.PlayerTournamentDetailVO;
import com.rally.domain.tour.model.PlayerTournamentVO;
import com.rally.domain.tour.model.SetScore;
import com.rally.domain.tour.model.TourDrawData;
import com.rally.domain.tour.model.TourRoundEnum;
import com.rally.domain.tour.repository.MatchQueryRepository;
import com.rally.domain.translation.cache.TranslationCache;
import com.rally.domain.translation.model.TranslationEntityTypeEnum;
import com.rally.domain.translation.model.TranslationKey;
import com.rally.domain.translation.model.TranslationLanguageEnum;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 业务活动 query-player-tournament-path：组装球员在指定签表中的已走与潜在路径。
 */
@Component
@RequiredArgsConstructor
public class QueryPlayerTournamentPathActivity {

    private static final TranslationLanguageEnum TARGET_LANGUAGE = TranslationLanguageEnum.ZH_CN;

    private final MatchQueryRepository matchQueryRepository;
    private final TranslationCache translationCache;

    public Result execute(String tournamentId, Integer year, String playerId, String drawType) {
        // A1：保留 main 的精确查询口径；签表或球员不存在时成功返回 null。
        TourDrawData draw = matchQueryRepository.getDrawByTournamentIdAndType(tournamentId, year, drawType);
        if (draw == null) {
            return Result.notFound();
        }
        PlayerDetailData playerDetail = matchQueryRepository.getPlayerById(playerId);
        if (playerDetail == null) {
            return Result.notFound();
        }

        PlayerSeedData seedData = matchQueryRepository.getSeedByDrawIdAndPlayerId(draw.getId(), playerId);
        Integer seed = seedData == null ? null : seedData.getSeed();
        List<MatchData> playerMatches = matchQueryRepository.listByDrawIdAndPlayerId(draw.getId(), playerId);
        List<MatchData> allMatches = matchQueryRepository.listByDrawId(draw.getId());

        Map<Integer, MatchData> indexToMatch = allMatches.stream()
                .filter(match -> match.getMatchIndex() != null)
                .collect(Collectors.toMap(MatchData::getMatchIndex, match -> match, (first, ignored) -> first));

        Set<String> playerIds = new HashSet<>();
        for (MatchData match : allMatches) {
            if (match.getPlayer1Id() != null) {
                playerIds.add(match.getPlayer1Id());
            }
            if (match.getPlayer2Id() != null) {
                playerIds.add(match.getPlayer2Id());
            }
            if (match.getWinnerId() != null) {
                playerIds.add(match.getWinnerId());
            }
        }
        playerIds.add(playerId);

        List<PlayerData> players = matchQueryRepository.listPlayersByPlayerIds(new ArrayList<>(playerIds));
        Map<String, String> playerNameMap = players.stream()
                .collect(Collectors.toMap(
                        PlayerData::getPlayerId,
                        player -> player.getLastName() == null ? "" : player.getLastName(),
                        (first, ignored) -> first));
        Map<String, String> playerNationalityMap = players.stream()
                .filter(player -> player.getNationality() != null)
                .collect(Collectors.toMap(
                        PlayerData::getPlayerId,
                        PlayerData::getNationality,
                        (first, ignored) -> first));

        // 种子继续按外部赛事编号合并，不额外限制年份或签表。
        Map<String, Integer> playerSeedMap = matchQueryRepository
                .listSeedsByTournamentIds(List.of(tournamentId))
                .stream()
                .collect(Collectors.toMap(
                        PlayerSeedData::getPlayerId,
                        PlayerSeedData::getSeed,
                        (first, ignored) -> first));

        Set<String> eliminatedPlayers = allMatches.stream()
                .filter(match -> "FINISHED".equals(match.getStatus()) && match.getWinnerId() != null)
                .flatMap(match -> {
                    Set<String> losers = new HashSet<>();
                    if (match.getPlayer1Id() != null && !match.getPlayer1Id().equals(match.getWinnerId())) {
                        losers.add(match.getPlayer1Id());
                    }
                    if (match.getPlayer2Id() != null && !match.getPlayer2Id().equals(match.getWinnerId())) {
                        losers.add(match.getPlayer2Id());
                    }
                    return losers.stream();
                })
                .collect(Collectors.toSet());

        List<MatchData> sortedPlayerMatches = playerMatches.stream()
                .sorted(Comparator.comparingInt(match -> Optional.ofNullable(match.getRoundNumber()).orElse(0)))
                .toList();

        boolean eliminated = sortedPlayerMatches.stream()
                .anyMatch(match -> "FINISHED".equals(match.getStatus())
                        && match.getWinnerId() != null
                        && !match.getWinnerId().equals(playerId));

        // A2：按轮次顺序保留所有胜局，最后遇到的败局作为出局信息。
        List<MatchProgressVO> progressPath = new ArrayList<>();
        MatchProgressVO eliminationInfo = null;
        for (MatchData match : sortedPlayerMatches) {
            if (!"FINISHED".equals(match.getStatus())) {
                continue;
            }
            String opponentId = getOpponentId(match, playerId);
            String opponentName = opponentId == null
                    ? null
                    : playerNameMap.getOrDefault(opponentId, opponentId);
            Integer opponentSeed = opponentId == null ? null : playerSeedMap.get(opponentId);
            CountryVO opponentCountry = opponentId == null
                    ? null
                    : CountryEnum.getCountry(playerNationalityMap.get(opponentId));
            boolean won = playerId.equals(match.getWinnerId());
            MatchProgressVO progress = buildProgress(
                    match,
                    opponentId,
                    opponentName,
                    opponentCountry,
                    opponentSeed,
                    formatScore(match, playerId),
                    won ? "WIN" : "LOSS");
            if (won) {
                progressPath.add(progress);
            } else {
                eliminationInfo = progress;
            }
        }

        // A3：未出局才寻找当前场次，并沿 matchIndex 的二叉树继续推算。
        MatchProgressVO next = null;
        List<MatchProgressVO> upcomingOpponents = new ArrayList<>();
        if (!eliminated) {
            MatchData currentMatch = findCurrentMatch(sortedPlayerMatches, playerId);
            if (currentMatch != null && currentMatch.getMatchIndex() != null) {
                next = buildNext(
                        currentMatch,
                        playerId,
                        indexToMatch,
                        playerNameMap,
                        playerNationalityMap,
                        playerSeedMap,
                        eliminatedPlayers);
                upcomingOpponents = buildUpcomingOpponents(
                        currentMatch,
                        indexToMatch,
                        playerNameMap,
                        playerNationalityMap,
                        playerSeedMap,
                        eliminatedPlayers);
            }
        }

        PlayerTournamentVO data = new PlayerTournamentVO();
        data.setPlayer(buildPlayerDetail(playerDetail, seed));
        data.setProgressPath(progressPath);
        data.setEliminationInfo(eliminationInfo);
        data.setNext(next);
        data.setUpcomingOpponents(upcomingOpponents);

        // A4：只读取已有简中译文。未命中保留原文并交付给后续登记活动。
        Set<TranslationKey> missingTranslationKeys = applyTranslations(data);
        return new Result(data, missingTranslationKeys);
    }

    private MatchProgressVO buildNext(
            MatchData currentMatch,
            String playerId,
            Map<Integer, MatchData> indexToMatch,
            Map<String, String> playerNameMap,
            Map<String, String> playerNationalityMap,
            Map<String, Integer> playerSeedMap,
            Set<String> eliminatedPlayers) {
        if (!"FINISHED".equals(currentMatch.getStatus())) {
            String opponentId = getOpponentId(currentMatch, playerId);
            if (opponentId == null) {
                return null;
            }
            return buildPending(
                    currentMatch.getRoundName() == null
                            ? roundNameFromMatchIndex(currentMatch.getMatchIndex())
                            : currentMatch.getRoundName(),
                    opponentId,
                    playerNameMap,
                    playerNationalityMap,
                    playerSeedMap,
                    formatScheduleInfo(currentMatch),
                    currentMatch.getCourt());
        }

        int nextMatchIndex = currentMatch.getMatchIndex() / 2;
        if (nextMatchIndex < 1) {
            return null;
        }
        int opponentSubtreeRoot = currentMatch.getMatchIndex() % 2 == 0
                ? currentMatch.getMatchIndex() + 1
                : currentMatch.getMatchIndex() - 1;
        String opponentId = findTopSeedInSubtree(
                opponentSubtreeRoot, indexToMatch, playerSeedMap, eliminatedPlayers);
        if (opponentId == null) {
            return null;
        }

        MatchData nextMatch = indexToMatch.get(nextMatchIndex);
        String round = nextMatch != null && nextMatch.getRoundName() != null
                ? nextMatch.getRoundName()
                : roundNameFromMatchIndex(nextMatchIndex);
        return buildPending(
                round,
                opponentId,
                playerNameMap,
                playerNationalityMap,
                playerSeedMap,
                nextMatch == null ? null : formatScheduleInfo(nextMatch),
                nextMatch == null ? null : nextMatch.getCourt());
    }

    private List<MatchProgressVO> buildUpcomingOpponents(
            MatchData currentMatch,
            Map<Integer, MatchData> indexToMatch,
            Map<String, String> playerNameMap,
            Map<String, String> playerNationalityMap,
            Map<String, Integer> playerSeedMap,
            Set<String> eliminatedPlayers) {
        List<MatchProgressVO> result = new ArrayList<>();
        int playerPathIndex = currentMatch.getMatchIndex() / 2;
        while (playerPathIndex / 2 >= 1) {
            int parentIndex = playerPathIndex / 2;
            MatchData nextMatch = indexToMatch.get(parentIndex);
            int opponentSubtreeRoot = playerPathIndex % 2 == 0
                    ? playerPathIndex + 1
                    : playerPathIndex - 1;
            String opponentId = findTopSeedInSubtree(
                    opponentSubtreeRoot, indexToMatch, playerSeedMap, eliminatedPlayers);
            if (opponentId != null) {
                String round = nextMatch != null && nextMatch.getRoundName() != null
                        ? nextMatch.getRoundName()
                        : roundNameFromMatchIndex(parentIndex);
                result.add(buildPending(
                        round,
                        opponentId,
                        playerNameMap,
                        playerNationalityMap,
                        playerSeedMap,
                        null,
                        null));
            }
            playerPathIndex = parentIndex;
        }
        return result;
    }

    private MatchProgressVO buildPending(
            String round,
            String opponentId,
            Map<String, String> playerNameMap,
            Map<String, String> playerNationalityMap,
            Map<String, Integer> playerSeedMap,
            String schedule,
            String court) {
        MatchProgressVO result = new MatchProgressVO();
        result.setRound(round);
        result.setRoundLabel(TourRoundEnum.labelOf(round));
        result.setOpponentId(opponentId);
        result.setOpponentName(playerNameMap.getOrDefault(opponentId, opponentId));
        result.setOpponentCountry(CountryEnum.getCountry(playerNationalityMap.get(opponentId)));
        result.setOpponentSeed(playerSeedMap.get(opponentId));
        result.setResult("PENDING");
        result.setScore(schedule);
        result.setCourt(court);
        return result;
    }

    private MatchData findCurrentMatch(List<MatchData> playerMatches, String playerId) {
        for (MatchData match : playerMatches) {
            if (!"FINISHED".equals(match.getStatus()) && match.getMatchIndex() != null) {
                return match;
            }
        }
        MatchData lastWon = null;
        for (MatchData match : playerMatches) {
            if ("FINISHED".equals(match.getStatus()) && playerId.equals(match.getWinnerId())) {
                lastWon = match;
            }
        }
        return lastWon;
    }

    private String findTopSeedInSubtree(
            int subtreeRoot,
            Map<Integer, MatchData> indexToMatch,
            Map<String, Integer> playerSeedMap,
            Set<String> eliminatedPlayers) {
        Set<String> players = new HashSet<>();
        collectPlayersInSubtree(subtreeRoot, indexToMatch, players);
        return players.stream()
                .filter(playerSeedMap::containsKey)
                .filter(player -> !eliminatedPlayers.contains(player))
                .min(Comparator.comparingInt(playerSeedMap::get))
                .orElse(null);
    }

    private void collectPlayersInSubtree(
            int index,
            Map<Integer, MatchData> indexToMatch,
            Set<String> players) {
        MatchData match = indexToMatch.get(index);
        if (match == null) {
            return;
        }
        if (match.getPlayer1Id() != null) {
            players.add(match.getPlayer1Id());
        }
        if (match.getPlayer2Id() != null) {
            players.add(match.getPlayer2Id());
        }
        collectPlayersInSubtree(index * 2, indexToMatch, players);
        collectPlayersInSubtree(index * 2 + 1, indexToMatch, players);
    }

    private String roundNameFromMatchIndex(int matchIndex) {
        int level = 31 - Integer.numberOfLeadingZeros(matchIndex);
        int remaining = 1 << (level + 1);
        return switch (remaining) {
            case 2 -> "F";
            case 4 -> "SF";
            case 8 -> "QF";
            default -> "R" + remaining;
        };
    }

    private String formatScore(MatchData match, String playerId) {
        if (!"FINISHED".equals(match.getStatus())) {
            return "待定";
        }
        List<SetScore> sets = Optional.ofNullable(match.getSets()).orElseGet(List::of);
        if (CollectionUtils.isEmpty(sets)) {
            return "已完成";
        }
        boolean playerOne = playerId.equals(match.getPlayer1Id());
        StringBuilder score = new StringBuilder();
        for (SetScore set : sets) {
            if (!score.isEmpty()) {
                score.append(' ');
            }
            int myGames = playerOne ? set.getP1Games() : set.getP2Games();
            int opponentGames = playerOne ? set.getP2Games() : set.getP1Games();
            score.append(myGames).append('-').append(opponentGames);
        }
        return score.toString();
    }

    private String formatScheduleInfo(MatchData match) {
        StringBuilder schedule = new StringBuilder();
        if (match.getScheduledAt() != null) {
            schedule.append(match.getScheduledAt().getMonthValue())
                    .append("月")
                    .append(match.getScheduledAt().getDayOfMonth())
                    .append("日 ")
                    .append(match.getScheduledAt().toLocalTime());
        } else if (match.getMatchDate() != null) {
            schedule.append(match.getMatchDate().getMonthValue())
                    .append("月")
                    .append(match.getMatchDate().getDayOfMonth())
                    .append("日");
        } else if (match.getScheduledAtText() != null) {
            schedule.append(match.getScheduledAtText());
        }
        if (match.getCourt() != null) {
            if (!schedule.isEmpty()) {
                schedule.append(' ');
            }
            schedule.append(match.getCourt());
            if (match.getCourtSeq() != null) {
                schedule.append(match.getCourtSeq());
            }
        }
        return schedule.isEmpty() ? "待定" : schedule.toString();
    }

    private String getOpponentId(MatchData match, String playerId) {
        if (playerId.equals(match.getPlayer1Id())) {
            return match.getPlayer2Id();
        }
        if (playerId.equals(match.getPlayer2Id())) {
            return match.getPlayer1Id();
        }
        return null;
    }

    private MatchProgressVO buildProgress(
            MatchData match,
            String opponentId,
            String opponentName,
            CountryVO opponentCountry,
            Integer opponentSeed,
            String score,
            String matchResult) {
        MatchProgressVO result = new MatchProgressVO();
        result.setRound(match.getRoundName());
        result.setRoundLabel(TourRoundEnum.labelOf(match.getRoundName()));
        result.setOpponentId(opponentId);
        result.setOpponentName(opponentName);
        result.setOpponentCountry(opponentCountry);
        result.setOpponentSeed(opponentSeed);
        result.setScore(score);
        result.setResult(matchResult);
        return result;
    }

    private PlayerTournamentDetailVO buildPlayerDetail(PlayerDetailData data, Integer seed) {
        PlayerTournamentDetailVO result = new PlayerTournamentDetailVO();
        result.setId(data.getPlayerId());
        result.setAvatarUrl(null);
        result.setName(buildName(data.getFirstName(), data.getLastName()));
        result.setCountry(CountryEnum.getCountry(data.getNationality()));
        result.setRank(data.getRank());
        result.setPoints(data.getPoints());
        if (data.getBirthDate() != null) {
            result.setAge(Period.between(data.getBirthDate(), LocalDate.now()).getYears());
        }
        result.setSeed(seed);
        return result;
    }

    private String buildName(String firstName, String lastName) {
        return ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
    }

    private Set<TranslationKey> applyTranslations(PlayerTournamentVO data) {
        Map<TranslationKey, List<MatchProgressVO>> opponentKeys = new LinkedHashMap<>();
        List<MatchProgressVO> allProgress = new ArrayList<>();
        allProgress.addAll(data.getProgressPath());
        allProgress.addAll(data.getUpcomingOpponents());
        if (data.getEliminationInfo() != null) {
            allProgress.add(data.getEliminationInfo());
        }
        if (data.getNext() != null) {
            allProgress.add(data.getNext());
        }
        for (MatchProgressVO progress : allProgress) {
            if (progress.getOpponentName() != null) {
                TranslationKey key = new TranslationKey(
                        TranslationEntityTypeEnum.PLAYER,
                        progress.getOpponentName(),
                        TARGET_LANGUAGE);
                opponentKeys.computeIfAbsent(key, ignored -> new ArrayList<>()).add(progress);
            }
        }

        TranslationKey playerKey = null;
        if (data.getPlayer() != null && data.getPlayer().getName() != null) {
            playerKey = new TranslationKey(
                    TranslationEntityTypeEnum.PLAYER,
                    data.getPlayer().getName(),
                    TARGET_LANGUAGE);
        }
        TranslationKey courtKey = null;
        if (data.getNext() != null && data.getNext().getCourt() != null) {
            courtKey = new TranslationKey(
                    TranslationEntityTypeEnum.COURT,
                    data.getNext().getCourt(),
                    TARGET_LANGUAGE);
        }

        Set<TranslationKey> requestedKeys = new LinkedHashSet<>(opponentKeys.keySet());
        if (playerKey != null) {
            requestedKeys.add(playerKey);
        }
        if (courtKey != null) {
            requestedKeys.add(courtKey);
        }

        Map<TranslationKey, String> translations = new HashMap<>();
        Set<TranslationKey> missingKeys = new LinkedHashSet<>();
        for (TranslationKey key : requestedKeys) {
            String translated = translationCache.get(key);
            if (StringUtils.isNotBlank(translated)) {
                translations.put(key, translated);
            } else {
                missingKeys.add(key);
            }
        }

        if (playerKey != null && translations.containsKey(playerKey)) {
            data.getPlayer().setName(translations.get(playerKey));
        }
        for (Map.Entry<TranslationKey, List<MatchProgressVO>> entry : opponentKeys.entrySet()) {
            String translated = translations.get(entry.getKey());
            if (translated != null) {
                entry.getValue().forEach(progress -> progress.setOpponentName(translated));
            }
        }
        if (courtKey != null && translations.containsKey(courtKey) && data.getNext().getScore() != null) {
            data.getNext().setScore(data.getNext().getScore()
                    .replace(data.getNext().getCourt(), translations.get(courtKey)));
        }
        return missingKeys;
    }

    /** 旧接口 DTO，以及交给后续活动的去重缺译键。 */
    public record Result(PlayerTournamentVO data, Set<TranslationKey> missingTranslationKeys) {
        public Result {
            missingTranslationKeys = missingTranslationKeys == null
                    ? Set.of()
                    : Collections.unmodifiableSet(new LinkedHashSet<>(missingTranslationKeys));
        }

        public static Result notFound() {
            return new Result(null, Set.of());
        }
    }
}
