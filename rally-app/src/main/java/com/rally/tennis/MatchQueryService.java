package com.rally.tennis;

import com.rally.db.tennis.entity.TennisTournamentPO;
import com.rally.db.tennis.repository.TennisTournamentRepository;
import com.rally.domain.tennis.gateway.MatchQueryGateway;
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
    private TennisTournamentRepository tennisTournamentRepository;

    @Resource
    private TennisTranslationService tennisTranslationService;

    /**
     * 查询比赛列表，返回按状态分类的比赛及种子信息
     * @param tournamentIdStr tournamentId，多个用逗号分隔
     */
    public MatchQueryResponse queryMatches(String tournamentIdStr) {
        // 解析 tournamentId 列表
        List<String> tournamentIds = parseTournamentIds(tournamentIdStr);
        if (CollectionUtils.isEmpty(tournamentIds)) {
            return emptyMatchResponse();
        }

        // 查询比赛数据
        List<MatchData> matches = matchQueryGateway.listByTournamentIds(tournamentIds);
        if (CollectionUtils.isEmpty(matches)) {
            return emptyMatchResponse();
        }

        // 过滤掉 player1_id 和 player2_id 都为空的比赛
        matches = matches.stream()
                .filter(m -> m.getPlayer1Id() != null || m.getPlayer2Id() != null)
                .toList();
        if (CollectionUtils.isEmpty(matches)) {
            return emptyMatchResponse();
        }

        // 查询所有相关球员
        Set<String> playerIds = new HashSet<>();
        for (MatchData match : matches) {
            if (match.getPlayer1Id() != null) playerIds.add(match.getPlayer1Id());
            if (match.getPlayer2Id() != null) playerIds.add(match.getPlayer2Id());
        }
        // 查询球员种子信息，构建 tournamentId:playerId -> seed 映射
        List<PlayerSeedData> seeds = matchQueryGateway.listSeedsByTournamentIds(tournamentIds);
        Map<String, Integer> seedMap = seeds.stream()
                .collect(Collectors.toMap(
                        s -> s.getTournamentId() + ":" + s.getPlayerId(),
                        PlayerSeedData::getSeed,
                        (a, b) -> a));

        // 查询赛事 tour 信息，构建 tournamentId -> tour 映射
        Map<String, String> tournamentTourMap = tennisTournamentRepository.listByTournamentIds(tournamentIds)
                .stream()
                .collect(Collectors.toMap(
                        TennisTournamentPO::getTournamentId,
                        po -> po.getTour() != null ? po.getTour() : "",
                        (a, b) -> a));

        // 将种子球员 ID 补充进查询集合，确保没有比赛记录的种子球员信息也能获取到
        seeds.stream().map(PlayerSeedData::getPlayerId).forEach(playerIds::add);
        List<PlayerData> players = matchQueryGateway.listPlayersByPlayerIds(new ArrayList<>(playerIds));
        Map<String, PlayerData> playerMap = players.stream()
                .collect(Collectors.toMap(PlayerData::getPlayerId, p -> p, (a, b) -> a));

        // 查询所有相关盘分
        List<Long> tennisMatchIds = matches.stream().map(MatchData::getTennisMatchId).filter(Objects::nonNull).toList();
        List<SetScoreData> setScores = matchQueryGateway.listSetScoresByTennisMatchIds(tennisMatchIds);
        Map<Long, List<SetScoreData>> setScoreMap = setScores.stream()
                .collect(Collectors.groupingBy(SetScoreData::getTennisMatchId));

        // 转换并按状态分组
        List<MatchQueryVO> upcomingMatches = new ArrayList<>();
        List<MatchQueryVO> finishedMatches = new ArrayList<>();

        for (MatchData match : matches) {
            MatchQueryVO vo = toMatchVO(match, playerMap, setScoreMap, seedMap);
            String status = vo.getStatus();
            if ("FINISHED".equals(status)) {
                finishedMatches.add(vo);
            } else {
                upcomingMatches.add(vo);
            }
        }

        // 若 upcomingMatches 中存在某日期，则将 finishedMatches 中同日期的比赛移入 upcomingMatches
        Set<String> upcomingDates = new HashSet<>();
        for (MatchQueryVO m : upcomingMatches) {
            if (m.getDate() != null) upcomingDates.add(m.getDate());
        }
        if (!upcomingDates.isEmpty()) {
            Iterator<MatchQueryVO> it = finishedMatches.iterator();
            while (it.hasNext()) {
                MatchQueryVO m = it.next();
                if (upcomingDates.contains(m.getDate())) {
                    it.remove();
                    upcomingMatches.add(m);
                }
            }
        }

        // 构建种子列表，按 seed 升序排列
        // 被淘汰判断：在 finishedMatches 中参与过但不是 winnerId 的球员；同时记录淘汰轮次
        Map<String, String> eliminatedRoundMap = new HashMap<>();
        for (MatchQueryVO m : finishedMatches) {
            if (m.getWinnerId() == null) continue;
            if (m.getPlayer1() != null && !m.getWinnerId().equals(m.getPlayer1().getId())) {
                // 同一球员可能出现多次（理论上不会），保留最后一条即可
                eliminatedRoundMap.put(m.getPlayer1().getId(), m.getRound());
            }
            if (m.getPlayer2() != null && !m.getWinnerId().equals(m.getPlayer2().getId())) {
                eliminatedRoundMap.put(m.getPlayer2().getId(), m.getRound());
            }
        }

        List<SeedVO> seedVOList = seeds.stream()
                .filter(s -> s.getSeed() != null && s.getSeed() != 0)
                .map(s -> {
                    SeedVO seedVO = new SeedVO();
                    seedVO.setPlayerId(s.getPlayerId());
                    seedVO.setSeed(s.getSeed());
                    seedVO.setTournamentId(s.getTournamentId());
                    seedVO.setTour(tournamentTourMap.getOrDefault(s.getTournamentId(), ""));
                    PlayerData player = playerMap.get(s.getPlayerId());
                    if (player != null) {
                        String name = StringUtils.isNotBlank(player.getLastName())
                                ? player.getLastName() : player.getFirstName();
                        seedVO.setName(name);
                        seedVO.setCountry(CountryEnum.getCountry(player.getNationality()));
                    }
                    String eliminatedRound = eliminatedRoundMap.get(s.getPlayerId());
                    if (eliminatedRound != null) {
                        seedVO.setStatus(SeedStatusEnum.ELIMINATED);
                        // 将 roundName（如 "QF"）转为中文展示名（如 "8强"）
                        seedVO.setLabel(TennisRoundEnum.labelOf(eliminatedRound));
                    } else {
                        seedVO.setStatus(SeedStatusEnum.ACTIVE);
                    }
                    return seedVO;
                })
                .sorted(Comparator.comparing(SeedVO::getSeed, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        // upcomingMatches 按 matchDate 正序，finishedMatches 按日期倒序
        upcomingMatches.sort(Comparator.comparing(MatchQueryVO::getScheduledAt, Comparator.nullsLast(Comparator.naturalOrder())));
        finishedMatches.sort(Comparator.comparing(MatchQueryVO::getStartedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        tennisTranslationService.matches(upcomingMatches, TranslationLanguageEnum.ZH_CN);
        tennisTranslationService.matches(finishedMatches, TranslationLanguageEnum.ZH_CN);
        tennisTranslationService.seeds(seedVOList, TranslationLanguageEnum.ZH_CN);

        Map<String, List<MatchQueryVO>> matchMap = new LinkedHashMap<>();
        matchMap.put("upcomingMatches", upcomingMatches);
        matchMap.put("finishedMatches", finishedMatches);

        MatchQueryResponse response = new MatchQueryResponse();
        response.setSeeds(seedVOList);
        response.setMatches(matchMap);
        return response;
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

    /**
     * 解析 tournamentId 字符串为列表
     */
    private List<String> parseTournamentIds(String tournamentIdStr) {
        if (tournamentIdStr == null || tournamentIdStr.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tournamentIdStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * MatchData → MatchQueryVO 转换
     */
    private MatchQueryVO toMatchVO(MatchData match, Map<String, PlayerData> playerMap,
                                   Map<Long, List<SetScoreData>> setScoreMap,
                                   Map<String, Integer> seedMap) {
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

        // 计算 scheduledTime
        if (match.getScheduledAt() != null) {
            vo.setScheduledTime(match.getScheduledAt().format(DateTimeFormatter.ofPattern("HH:mm")));
        } else {
            vo.setScheduledTime(null);
        }

        // 设置球员信息（包含种子）
        vo.setPlayer1(buildPlayerVO(match.getPlayer1Id(), match.getTournamentId(), playerMap, seedMap));
        vo.setPlayer2(buildPlayerVO(match.getPlayer2Id(), match.getTournamentId(), playerMap, seedMap));

        // 设置盘分
        List<SetScoreData> setScores = setScoreMap.getOrDefault(match.getTennisMatchId(), List.of());
        List<SetScoreVO> sets = setScores.stream().map(MatchConvertMapper.INSTANCE::toSetScoreVO).toList();
        vo.setSets(sets);

        // 派生状态
        vo.setStatus(match.getStatus());

        // 计算当前盘和当前盘比分
        vo.setCurrentSet(calculateCurrentSet(sets));
        vo.setCurrentSetScore(calculateCurrentSetScore(sets));

        // 设置 winnerId
        vo.setWinnerId(match.getWinnerId());

        // 计算时长
        vo.setDuration(calculateDuration(match));

        return vo;
    }

    /**
     * 构建球员 VO
     */
    private PlayerVO buildPlayerVO(String playerId, String tournamentId,
                                   Map<String, PlayerData> playerMap,
                                   Map<String, Integer> seedMap) {
        if (playerId == null) {
            return null;
        }
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

    /**
     * 计算当前盘数
     */
    private Integer calculateCurrentSet(List<SetScoreVO> sets) {
        if (CollectionUtils.isEmpty(sets)) {
            return null;
        }
        return sets.size();
    }

    /**
     * 计算当前盘比分
     */
    private String calculateCurrentSetScore(List<SetScoreVO> sets) {
        if (CollectionUtils.isEmpty(sets)) {
            return null;
        }
        SetScoreVO lastSet = sets.get(sets.size() - 1);
        if (lastSet.getPlayer1() == null || lastSet.getPlayer2() == null) {
            return null;
        }
        return lastSet.getPlayer1() + "-" + lastSet.getPlayer2();
    }

    /**
     * 计算比赛时长
     */
    private String calculateDuration(MatchData match) {
        if (match.getDurationMinutes() != null) {
            int minutes = match.getDurationMinutes();
            int hours = minutes / 60;
            int mins = minutes % 60;
            if (hours > 0) {
                return hours + "h" + (mins > 0 ? mins + "m" : "");
            }
            return mins + "m";
        }
        // 如果比赛正在进行中，计算从开始到现在的时间
        if ("live".equals(match.getStatus()) && match.getStartedAt() != null) {
            long minutes = java.time.Duration.between(match.getStartedAt(), LocalDateTime.now()).toMinutes();
            if (minutes >= 0) {
                int hours = (int) (minutes / 60);
                int mins = (int) (minutes % 60);
                if (hours > 0) {
                    return "已开始 " + hours + "h" + (mins > 0 ? mins + "m" : "");
                }
                return "已开始 " + mins + "m";
            }
        }
        return null;
    }
}
