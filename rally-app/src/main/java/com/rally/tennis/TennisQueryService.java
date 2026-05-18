package com.rally.tennis;

import com.rally.db.tennis.entity.TennisTournamentPO;
import com.rally.db.tennis.repository.TennisTournamentRepository;
import com.rally.domain.tennis.gateway.MatchQueryGateway;
import com.rally.domain.tennis.model.*;
import com.rally.domain.translation.model.TranslationLanguageEnum;
import com.rally.translation.TennisTranslationService;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TennisQueryService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Resource
    private TennisTournamentRepository tennisTournamentRepository;

    @Resource
    private MatchQueryGateway matchQueryGateway;

    @Resource
    private com.rally.db.tennis.repository.TennisPlayerRepository tennisPlayerRepository;

    @Resource
    private TennisTranslationService tennisTranslationService;

    /**
     * 查询赛事列表，支持按状态、类型和时间范围筛选
     * @param range recent=最近一个月, 其他或null=全部
     */
    public List<TournamentDTO> queryTournaments(String status, String type, String range) {
        String dbStatus = resolveDbStatus(status);

        // 时间范围：recent=最近一个月，其他=不筛选
        LocalDate dateFrom = null;
        LocalDate dateTo = null;
        if ("recent".equalsIgnoreCase(range)) {
            LocalDate today = LocalDate.now();
            dateFrom = today.minusMonths(1);
            dateTo = today.plusMonths(1);
        }

        List<TennisTournamentPO> list = tennisTournamentRepository.listByCondition(dbStatus, type, dateFrom, dateTo);
        if (CollectionUtils.isEmpty(list)) {
            return List.of();
        }

        list = list.stream()
                .filter(po -> isCategoryKept(po.getCategory()))
                .toList();
        if (CollectionUtils.isEmpty(list)) {
            return List.of();
        }

        // 按 (city, name) 分组，计算 groupId
        List<TournamentDTO> result = new ArrayList<>();
        List<List<TennisTournamentPO>> groups = groupByCityAndName(list);

        for (List<TennisTournamentPO> group : groups) {
            String groupId = "g" + (result.size() + 1);
            for (TennisTournamentPO po : group) {
                result.add(toVO(po, groupId));
            }
        }

        tennisTranslationService.tournaments(result, TranslationLanguageEnum.ZH_CN);
        return result;
    }

    /**
     * 根据前端 status 参数映射到数据库 status
     */
    private String resolveDbStatus(String status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case "FINISHED" -> "completed";
            case "ONGOING", "UPCOMING" -> "active";
            default -> null;
        };
    }

    /**
     * category 过滤：非数字或数字 >= 500 才保留
     */
    private boolean isCategoryKept(String category) {
        if (category == null || category.isBlank()) {
            return true;
        }
        try {
            return Integer.parseInt(category.trim()) >= 500;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    /**
     * 按城市和时间分组：city 不区分大小写相同 且 startDate 和 endDate 时间重合算作同一分组
     */
    private List<List<TennisTournamentPO>> groupByCityAndName(List<TennisTournamentPO> list) {
        // 使用列表存储分组，每个分组是一个 Map 的 entry: key 是组索引，value 是该组的赛事列表
        List<List<TennisTournamentPO>> groups = new ArrayList<>();

        for (TennisTournamentPO po : list) {
            boolean added = false;
            for (List<TennisTournamentPO> group : groups) {
                TennisTournamentPO first = group.get(0);
                if (isSameGroup(first, po)) {
                    group.add(po);
                    added = true;
                    break;
                }
            }
            if (!added) {
                List<TennisTournamentPO> newGroup = new ArrayList<>();
                newGroup.add(po);
                groups.add(newGroup);
            }
        }

        // 组内按 startDate 排序
        for (List<TennisTournamentPO> group : groups) {
            group.sort(Comparator.comparing(TennisTournamentPO::getStartDate,
                    Comparator.nullsLast(Comparator.naturalOrder())));
        }

        return groups;
    }

    /**
     * 判断两个赛事是否属于同一分组：city 不区分大小写相同 且 startDate 和 endDate 时间重合
     */
    private boolean isSameGroup(TennisTournamentPO a, TennisTournamentPO b) {
        // city 不区分大小写比较
        String cityA = a.getCity() != null ? a.getCity().toLowerCase() : "";
        String cityB = b.getCity() != null ? b.getCity().toLowerCase() : "";
        if (!cityA.equals(cityB)) {
            return false;
        }

        // startDate 和 endDate 时间重合
        if (a.getStartDate() == null || b.getStartDate() == null ||
            a.getEndDate() == null || b.getEndDate() == null) {
            return false;
        }

        // 时间重合判断：a.start <= b.end && b.start <= a.end
        return !a.getStartDate().isAfter(b.getEndDate()) && !b.getStartDate().isAfter(a.getEndDate());
    }

    /**
     * PO → VO 转换，包含状态派生逻辑
     */
    private TournamentDTO toVO(TennisTournamentPO po, String groupId) {
        TournamentDTO vo = new TournamentDTO();
        vo.setId(po.getTournamentId());
        vo.setName(po.getName());
        vo.setType(po.getTour());
        vo.setTypeLabel(resolveTypeLabel(po.getTour()));
        vo.setCategory(po.getCategory());
        vo.setSurface(po.getSurface() != null ? po.getSurface().toUpperCase() : null);
        vo.setSurfaceLabel(po.getSurface());
        vo.setCity(po.getCity());
        vo.setStartDate(po.getStartDate() != null ? po.getStartDate().format(DATE_FMT) : null);
        vo.setEndDate(po.getEndDate() != null ? po.getEndDate().format(DATE_FMT) : null);

        // 派生展示状态
        String displayStatus = deriveStatus(po);
        vo.setStatus(displayStatus);
        vo.setStatusLabel(resolveStatusLabel(displayStatus));
        vo.setGroupId(groupId);

        return vo;
    }

    /**
     * 根据数据库 status 和日期派生前端展示状态
     */
    private String deriveStatus(TennisTournamentPO po) {
        LocalDate today = LocalDate.now();
        if ("completed".equals(po.getStatus())) {
            return "FINISHED";
        }
        if (po.getStartDate() != null && today.isBefore(po.getStartDate())) {
            return "UPCOMING";
        }
        return "ONGOING";
    }

    private String resolveTypeLabel(String type) {
        if (type == null) return "";
        return switch (type) {
            case "ATP" -> "ATP";
            case "WTA" -> "WTA";
            default -> type;
        };
    }

    private String resolveStatusLabel(String status) {
        if (status == null) return "";
        return switch (status) {
            case "ONGOING" -> "进行中";
            case "UPCOMING" -> "即将开始";
            case "FINISHED" -> "已结束";
            default -> status;
        };
    }

    private String safeStr(String s) {
        return s != null ? s : "";
    }

    /**
     * 查询比赛列表，返回按状态分类的比赛
     * @param tournamentIdStr tournamentId，多个用逗号分隔
     */
    public Map<String, List<MatchQueryVO>> queryMatches(String tournamentIdStr) {
        // 解析 tournamentId 列表
        List<String> tournamentIds = parseTournamentIds(tournamentIdStr);
        if (CollectionUtils.isEmpty(tournamentIds)) {
            return Map.of("upcomingMatches", List.of(), "finishedMatches", List.of());
        }

        // 查询比赛数据
        List<MatchData> matches = matchQueryGateway.listByTournamentIds(tournamentIds);
        if (CollectionUtils.isEmpty(matches)) {
            return Map.of("upcomingMatches", List.of(), "finishedMatches", List.of());
        }

        // 过滤掉 player1_id 和 player2_id 都为空的比赛
        matches = matches.stream()
                .filter(m -> m.getPlayer1Id() != null || m.getPlayer2Id() != null)
                .toList();
        if (CollectionUtils.isEmpty(matches)) {
            return Map.of("upcomingMatches", List.of(), "finishedMatches", List.of());
        }

        // 查询所有相关球员
        Set<String> playerIds = new HashSet<>();
        for (MatchData match : matches) {
            if (match.getPlayer1Id() != null) playerIds.add(match.getPlayer1Id());
            if (match.getPlayer2Id() != null) playerIds.add(match.getPlayer2Id());
        }
        List<PlayerData> players = matchQueryGateway.listPlayersByPlayerIds(new ArrayList<>(playerIds));
        Map<String, PlayerData> playerMap = players.stream()
                .collect(Collectors.toMap(PlayerData::getPlayerId, p -> p, (a, b) -> a));

        // 查询所有相关盘分
        List<Long> tennisMatchIds = matches.stream().map(MatchData::getTennisMatchId).filter(Objects::nonNull).toList();
        List<SetScoreData> setScores = matchQueryGateway.listSetScoresByTennisMatchIds(tennisMatchIds);
        Map<Long, List<SetScoreData>> setScoreMap = setScores.stream()
                .collect(Collectors.groupingBy(SetScoreData::getTennisMatchId));

        // 查询球员种子信息，构建 tournamentId:playerId -> seed 映射
        List<PlayerSeedData> seeds = matchQueryGateway.listSeedsByTournamentIds(tournamentIds);
        Map<String, Integer> seedMap = seeds.stream()
                .collect(Collectors.toMap(
                        s -> s.getTournamentId() + ":" + s.getPlayerId(),
                        PlayerSeedData::getSeed,
                        (a, b) -> a));

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

        // upcomingMatches 按 matchDate 正序，finishedMatches 按日期倒序
        upcomingMatches.sort(Comparator.comparing(MatchQueryVO::getScheduledAt, Comparator.nullsLast(Comparator.naturalOrder())));
        finishedMatches.sort(Comparator.comparing(MatchQueryVO::getStartedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        tennisTranslationService.matches(upcomingMatches, TranslationLanguageEnum.ZH_CN);
        tennisTranslationService.matches(finishedMatches, TranslationLanguageEnum.ZH_CN);

        Map<String, List<MatchQueryVO>> result = new LinkedHashMap<>();
        result.put("upcomingMatches", upcomingMatches);
        result.put("finishedMatches", finishedMatches);
        return result;
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
        List<SetScoreVO> sets = setScores.stream().map(this::toSetScoreVO).toList();
        vo.setSets(sets);

        // 派生状态
        vo.setStatus(match.getStatus());
//        vo.setStatusLabel(resolveMatchStatusLabel(displayStatus));

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
     * SetScoreData → SetScoreVO 转换
     */
    private SetScoreVO toSetScoreVO(SetScoreData data) {
        SetScoreVO vo = new SetScoreVO();
        vo.setNumber(data.getSetNumber());
        vo.setPlayer1(data.getP1Games());
        vo.setPlayer2(data.getP2Games());
        vo.setTiebreak1(data.getP1Tiebreak());
        vo.setTiebreak2(data.getP2Tiebreak());
        return vo;
    }

    /**
     * 根据数据库状态和日期派生前端展示状态
     */
    private String deriveMatchStatus(MatchData match) {
        String dbStatus = match.getStatus();
        if ("finished".equals(dbStatus) || "completed".equals(dbStatus)) {
            return "FINISHED";
        }
        if ("live".equals(dbStatus)) {
            return "LIVE";
        }
        // scheduled 或其他状态
        if (match.getScheduledAt() != null) {
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(match.getScheduledAt())) {
                return "WAITING";
            } else {
                // 已过预定时间但状态还是 scheduled，说明是 NOT_EARLIER_THAN
                return "NOT_EARLIER_THAN";
            }
        }
        return "WAITING";
    }

    /**
     * 解析比赛状态标签
     */
    private String resolveMatchStatusLabel(String status) {
        if (status == null) return "";
        return switch (status) {
            case "LIVE" -> "直播中";
            case "WAITING" -> "候场中";
            case "NOT_EARLIER_THAN" -> "不早于";
            case "FINISHED" -> "已结束";
            default -> status;
        };
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

    public List<PlayerQueryVO> queryPlayers(String tour) {
        if (tour == null || tour.isBlank()) return List.of();
        List<com.rally.db.tennis.entity.TennisPlayerPO> players =
                tennisPlayerRepository.listByTourOrderByRank(tour.toUpperCase());
        LocalDate today = LocalDate.now();
        return players.stream()
                .map(po -> toPlayerQueryVO(po, today))
                .toList();
    }

    private PlayerQueryVO toPlayerQueryVO(com.rally.db.tennis.entity.TennisPlayerPO po, LocalDate today) {
        PlayerQueryVO vo = new PlayerQueryVO();
        vo.setId(po.getPlayerId());
        vo.setRank(po.getRank());
        String first = po.getFirstName() != null ? po.getFirstName() : "";
        String last  = po.getLastName()  != null ? po.getLastName()  : "";
        vo.setName((first + " " + last).trim());
        vo.setCountry(CountryEnum.getCountry(po.getNationality()));
        vo.setPoints(po.getPoints());
        if (po.getBirthDate() != null) {
            vo.setAge(Period.between(po.getBirthDate(), today).getYears());
            vo.setBirthDate(po.getBirthDate().format(DATE_FMT));
        }
        return vo;
    }
}
