package com.rally.tour;

import com.rally.domain.tour.repository.MatchQueryRepository;
import com.rally.domain.tour.model.*;
import com.rally.domain.translation.model.TranslationLanguageEnum;
import com.rally.translation.TourTranslationService;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PlayerTournamentQueryService {

    @Resource
    private MatchQueryRepository matchQueryRepository;

    @Resource
    private TourTranslationService tourTranslationService;

    public PlayerTournamentVO query(String tournamentId, Integer year, String playerId, String drawType) {
        // 1. 查询签表
        TourDrawData draw = matchQueryRepository.getDrawByTournamentIdAndType(tournamentId, year, drawType);
        if (draw == null) {
            return null;
        }

        // 2. 查询球员基本信息
        PlayerDetailData playerDetail = matchQueryRepository.getPlayerById(playerId);
        if (playerDetail == null) {
            return null;
        }

        // 3. 查询球员种子
        PlayerSeedData seedData = matchQueryRepository.getSeedByDrawIdAndPlayerId(draw.getId(), playerId);
        Integer seed = seedData != null ? seedData.getSeed() : null;

        // 4. 查询该签表下球员参与的所有比赛
        List<MatchData> playerMatches = matchQueryRepository.listByDrawIdAndPlayerId(draw.getId(), playerId);

        // 5. 查询该签表下所有比赛（用于前方对手推算）
        List<MatchData> allMatches = matchQueryRepository.listByDrawId(draw.getId());
        // matchIndex → match 的映射
        Map<Integer, MatchData> indexToMatch = allMatches.stream()
                .filter(m -> m.getMatchIndex() != null)
                .collect(Collectors.toMap(MatchData::getMatchIndex, m -> m, (a, b) -> a));

        // 6. 批量查询涉及的球员名字
        Set<String> playerIds = new HashSet<>();
        for (MatchData m : allMatches) {
            if (m.getPlayer1Id() != null) playerIds.add(m.getPlayer1Id());
            if (m.getPlayer2Id() != null) playerIds.add(m.getPlayer2Id());
            if (m.getWinnerId() != null) playerIds.add(m.getWinnerId());
        }
        playerIds.add(playerId);
        List<PlayerData> playerDataList = matchQueryRepository.listPlayersByPlayerIds(new ArrayList<>(playerIds));
        // 对手名字只取 lastName
        Map<String, String> playerNameMap = playerDataList.stream()
                .collect(Collectors.toMap(PlayerData::getPlayerId,
                        p -> p.getLastName() != null ? p.getLastName() : "", (a,b) -> a));
        // 对手国籍映射
        Map<String, String> playerNationalityMap = playerDataList.stream()
                .filter(p -> p.getNationality() != null)
                .collect(Collectors.toMap(PlayerData::getPlayerId, PlayerData::getNationality, (a, b) -> a));

        // 7. 查询球员参与比赛的盘分数据
        List<Long> tourMatchIds = playerMatches.stream()
                .filter(m -> m.getTourMatchId() != null)
                .map(MatchData::getTourMatchId)
                .toList();
        List<SetScoreData> setScores = CollectionUtils.isEmpty(tourMatchIds)
                ? List.of()
                : matchQueryRepository.listSetScoresByTourMatchIds(tourMatchIds);
        Map<Long, List<SetScoreData>> setScoreMap = setScores.stream()
                .collect(Collectors.groupingBy(SetScoreData::getTourMatchId));

        // 8. 查询所有种子信息（用于前方对手种子推算）
        List<PlayerSeedData> allSeeds = matchQueryRepository.listSeedsByTournamentIds(List.of(tournamentId));
        Map<String, Integer> playerSeedMap = allSeeds.stream()
                .collect(Collectors.toMap(PlayerSeedData::getPlayerId, PlayerSeedData::getSeed, (a, b) -> a));

        // 构建已淘汰球员集合：在 FINISHED 比赛中出现但不是 winnerId 的球员
        Set<String> eliminatedPlayers = allMatches.stream()
                .filter(m -> "FINISHED".equals(m.getStatus()) && m.getWinnerId() != null)
                .flatMap(m -> {
                    Set<String> losers = new HashSet<>();
                    if (m.getPlayer1Id() != null && !m.getPlayer1Id().equals(m.getWinnerId())) losers.add(m.getPlayer1Id());
                    if (m.getPlayer2Id() != null && !m.getPlayer2Id().equals(m.getWinnerId())) losers.add(m.getPlayer2Id());
                    return losers.stream();
                })
                .collect(Collectors.toSet());

        // 9. 按 roundNumber 排序球员比赛
        playerMatches = playerMatches.stream()
                .sorted(Comparator.comparingInt(m ->
                        Optional.ofNullable(m.getRoundNumber()).orElse(0)))
                .toList(); // 注意：Java 16+ 的 toList() 也是不可变的，用 collect(Collectors.toList()) 更安全

        // 10. 判断球员是否已出局
        boolean eliminated = playerMatches.stream()
                .anyMatch(m -> "FINISHED".equals(m.getStatus())
                        && m.getWinnerId() != null
                        && !m.getWinnerId().equals(playerId));

        // 11. 构建晋级路线（已完成且赢了的比赛）
        List<MatchProgressVO> progressPath = new ArrayList<>();
        MatchProgressVO eliminationInfo = null;

        for (MatchData m : playerMatches) {
            String opponentId = getOpponentId(m, playerId);
            String opponentName = opponentId != null ? playerNameMap.getOrDefault(opponentId, opponentId) : null;
            Integer opponentSeed = opponentId != null ? playerSeedMap.get(opponentId) : null;
            CountryVO opponentCountry = opponentId != null
                    ? CountryEnum.getCountry(playerNationalityMap.get(opponentId))
                    : null;
            String score = formatScore(m, playerId, setScoreMap);

            if ("FINISHED".equals(m.getStatus())) {
                boolean won = playerId.equals(m.getWinnerId());
                MatchProgressVO vo = buildProgressVO(m, opponentId, opponentName, opponentCountry, opponentSeed, score, won ? "WIN" : "LOSS");
                if (won) {
                    progressPath.add(vo);
                } else {
                    eliminationInfo = vo;
                }
            }
        }

        // 12. 构建前方对手（未出局时推算后续轮次）
        List<MatchProgressVO> upcomingOpponents = new ArrayList<>();
        if (!eliminated) {
            MatchData currentMatch = findCurrentMatch(playerMatches, playerId);
            if (currentMatch != null && currentMatch.getMatchIndex() != null) {
                upcomingOpponents = buildUpcomingOpponents(currentMatch, playerId, indexToMatch, playerNameMap, playerNationalityMap, playerSeedMap, eliminatedPlayers);
            }
        }

        // 12. 组装响应
        PlayerTournamentVO result = new PlayerTournamentVO();
        result.setPlayer(buildPlayerDetail(playerDetail, seed));
        result.setProgressPath(progressPath);
        result.setUpcomingOpponents(upcomingOpponents);
        result.setEliminationInfo(eliminationInfo);

        // 13. 翻译球员姓名（主球员 + 所有对手）
        tourTranslationService.playerTournament(result, TranslationLanguageEnum.ZH_CN);

        return result;
    }

    /**
     * 找到球员当前所在的比赛（优先取未完成的，否则取最后一场已完成且赢了的）
     */
    private MatchData findCurrentMatch(List<MatchData> playerMatches, String playerId) {
        // 找未完成的比赛（球员已确认在场）
        for (MatchData m : playerMatches) {
            if (!"FINISHED".equals(m.getStatus()) && m.getMatchIndex() != null) {
                return m;
            }
        }
        // 找最后一场赢了的比赛
        MatchData last = null;
        for (MatchData m : playerMatches) {
            if ("FINISHED".equals(m.getStatus()) && playerId.equals(m.getWinnerId())) {
                last = m;
            }
        }
        return last;
    }

    /**
     * 根据 matchIndex 二叉树推算后续每轮对手
     * 对每轮找对手子树中种子号最小（最高种子）且未被淘汰的球员；找不到则跳过该轮
     */
    private List<MatchProgressVO> buildUpcomingOpponents(
            MatchData currentMatch, String playerId,
            Map<Integer, MatchData> indexToMatch,
            Map<String, String> playerNameMap,
            Map<String, String> playerNationalityMap,
            Map<String, Integer> playerSeedMap,
            Set<String> eliminatedPlayers) {

        List<MatchProgressVO> result = new ArrayList<>();
        int playerPathIdx = currentMatch.getMatchIndex();

        // 当前比赛未完成且对手已确定，先加入
        if (!"FINISHED".equals(currentMatch.getStatus())) {
            String opponentId = getOpponentId(currentMatch, playerId);
            if (opponentId != null) {
                String opponentName = playerNameMap.getOrDefault(opponentId, opponentId);
                Integer opponentSeed = playerSeedMap.get(opponentId);
                CountryVO opponentCountry = CountryEnum.getCountry(playerNationalityMap.get(opponentId));
                String round = currentMatch.getRoundName() != null
                        ? currentMatch.getRoundName()
                        : roundNameFromMatchIndex(playerPathIdx);
                MatchProgressVO vo = new MatchProgressVO();
                vo.setRound(round);
                vo.setRoundLabel(TourRoundEnum.labelOf(round));
                vo.setOpponentId(opponentId);
                vo.setOpponentName(opponentName);
                vo.setOpponentCountry(opponentCountry);
                vo.setOpponentSeed(opponentSeed);
                vo.setResult("PENDING");
                result.add(vo);
            }
        }

        while (playerPathIdx / 2 >= 1) {
            int parentIdx = playerPathIdx / 2;
            MatchData nextMatch = indexToMatch.get(parentIdx);

            // 对手子树根 = playerPathIdx 的兄弟节点
            int opponentSubtreeRoot = (playerPathIdx % 2 == 0) ? playerPathIdx + 1 : playerPathIdx - 1;

            // 从对手子树中找最高种子（未被淘汰），找不到则跳过该轮
            String opponentId = findTopSeedInSubtree(opponentSubtreeRoot, indexToMatch, playerSeedMap, eliminatedPlayers);
            if (opponentId != null) {
                String opponentName = playerNameMap.getOrDefault(opponentId, opponentId);
                Integer opponentSeed = playerSeedMap.get(opponentId);
                CountryVO opponentCountry = CountryEnum.getCountry(playerNationalityMap.get(opponentId));
                // 优先取 match 记录的轮次，match 不存在时从 matchIndex 推算
                String round = (nextMatch != null && nextMatch.getRoundName() != null)
                        ? nextMatch.getRoundName()
                        : roundNameFromMatchIndex(parentIdx);
                MatchProgressVO vo = new MatchProgressVO();
                vo.setRound(round);
                vo.setRoundLabel(TourRoundEnum.labelOf(round));
                vo.setOpponentId(opponentId);
                vo.setOpponentName(opponentName);
                vo.setOpponentCountry(opponentCountry);
                vo.setOpponentSeed(opponentSeed);
//                vo.setScore("待定");
                vo.setResult("PENDING");
                result.add(vo);
            }

            playerPathIdx = parentIdx;
        }
        return result;
    }

    /**
     * 在子树中收集所有球员，返回种子号最小（最高种子）且未被淘汰的球员ID
     */
    private String findTopSeedInSubtree(int subtreeRoot, Map<Integer, MatchData> indexToMatch,
                                         Map<String, Integer> playerSeedMap, Set<String> eliminatedPlayers) {
        Set<String> players = new HashSet<>();
        collectPlayersInSubtree(subtreeRoot, indexToMatch, players);
        return players.stream()
                .filter(playerSeedMap::containsKey)
                .filter(p -> !eliminatedPlayers.contains(p))
                .min(Comparator.comparingInt(playerSeedMap::get))
                .orElse(null);
    }

    /**
     * 递归收集子树内所有比赛的球员ID（子节点 = idx*2 和 idx*2+1）
     */
    private void collectPlayersInSubtree(int idx, Map<Integer, MatchData> indexToMatch, Set<String> players) {
        MatchData match = indexToMatch.get(idx);
        if (match == null) {
            return;
        }
        if (match.getPlayer1Id() != null) players.add(match.getPlayer1Id());
        if (match.getPlayer2Id() != null) players.add(match.getPlayer2Id());
        collectPlayersInSubtree(idx * 2, indexToMatch, players);
        collectPlayersInSubtree(idx * 2 + 1, indexToMatch, players);
    }

    /**
     * 根据 matchIndex 推算轮次名称
     * matchIndex=1 → F，每往上一层人数翻倍：SF/QF/R16/R32/R64/R128
     */
    private String roundNameFromMatchIndex(int matchIndex) {
        // floor(log2(matchIndex)) 得到层级，层级0=决赛
        int level = 31 - Integer.numberOfLeadingZeros(matchIndex);
        int remaining = 1 << (level + 1);
        return switch (remaining) {
            case 2 -> "F";
            case 4 -> "SF";
            case 8 -> "QF";
            default -> "R" + remaining;
        };
    }

    /**
     * 格式化比分字符串，如 "6-3 6-4 7-6(5)"
     * player1 视角：若 playerId 是 player1，直接用 p1Games-p2Games；否则反转
     */
    private String formatScore(MatchData match, String playerId, Map<Long, List<SetScoreData>> setScoreMap) {
        if (!"FINISHED".equals(match.getStatus()) || match.getTourMatchId() == null) {
            return "待定";
        }
        List<SetScoreData> sets = setScoreMap.getOrDefault(match.getTourMatchId(), List.of());
        if (CollectionUtils.isEmpty(sets)) {
            return "已完成";
        }
        boolean isPlayer1 = playerId.equals(match.getPlayer1Id());
        StringBuilder sb = new StringBuilder();
        for (SetScoreData s : sets) {
            if (sb.length() > 0) sb.append(" ");
            int myGames = isPlayer1 ? s.getP1Games() : s.getP2Games();
            int oppGames = isPlayer1 ? s.getP2Games() : s.getP1Games();
            sb.append(myGames).append("-").append(oppGames);
            // 抢七：显示失分方的抢七分数
            Integer myTb = isPlayer1 ? s.getP1Tiebreak() : s.getP2Tiebreak();
            Integer oppTb = isPlayer1 ? s.getP2Tiebreak() : s.getP1Tiebreak();
            if (myTb != null || oppTb != null) {
                int loserTb = (myGames > oppGames) ? (oppTb != null ? oppTb : 0) : (myTb != null ? myTb : 0);
                sb.append("(").append(loserTb).append(")");
            }
        }
        return sb.toString();
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

    private MatchProgressVO buildProgressVO(MatchData match, String opponentId, String opponentName,
                                             CountryVO opponentCountry, Integer opponentSeed, String score, String result) {
        MatchProgressVO vo = new MatchProgressVO();
        vo.setRound(match.getRoundName());
        vo.setRoundLabel(TourRoundEnum.labelOf(match.getRoundName()));
        vo.setOpponentId(opponentId);
        vo.setOpponentName(opponentName);
        vo.setOpponentCountry(opponentCountry);
        vo.setOpponentSeed(opponentSeed);
        vo.setScore(score);
        vo.setResult(result);
        return vo;
    }

    private PlayerTournamentDetailVO buildPlayerDetail(PlayerDetailData data, Integer seed) {
        PlayerTournamentDetailVO vo = new PlayerTournamentDetailVO();
        vo.setId(data.getPlayerId());
        vo.setAvatarUrl(null);
        vo.setName(buildName(data.getFirstName(), data.getLastName()));
        vo.setCountry(CountryEnum.getCountry(data.getNationality()));
        vo.setRank(data.getRank());
        vo.setPoints(data.getPoints());
        if (data.getBirthDate() != null) {
            vo.setAge(Period.between(data.getBirthDate(), LocalDate.now()).getYears());
        }
        vo.setSeed(seed);
        return vo;
    }

    private String buildName(String firstName, String lastName) {
        String first = firstName != null ? firstName : "";
        String last = lastName != null ? lastName : "";
        return (first + " " + last).trim();
    }
}
