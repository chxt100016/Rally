package com.rally.domain.tour;

import com.rally.domain.tour.convert.MatchConvertMapper;
import com.rally.domain.tour.gateway.MatchQueryGateway;
import com.rally.domain.tour.gateway.TourTournamentGateway;
import com.rally.domain.tour.model.*;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TourMatchQueryDomainService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Resource
    private MatchQueryGateway matchQueryGateway;

    @Resource
    private TourTournamentGateway tourTournamentGateway;

    // ==================== 扁平查询：每个方法各自独立查询所需数据 ====================

    /**
     * 种子球员列表。淘汰状态用全部已结束比赛计算（今天输球立即标记淘汰）。
     */
    public List<SeedVO> seeds(List<String> tournamentIds) {
        if (CollectionUtils.isEmpty(tournamentIds)) return List.of();
        List<PlayerSeedData> seedData = matchQueryGateway.listSeedsByTournamentIds(tournamentIds);
        if (CollectionUtils.isEmpty(seedData)) return List.of();

        Map<String, String> eliminatedRoundMap = buildEliminatedRoundMap(matchQueryGateway.listFinishedByTournamentIds(tournamentIds));

        List<String> seedPlayerIds = seedData.stream().map(PlayerSeedData::getPlayerId).distinct().toList();
        Map<String, PlayerData> playerMap = matchQueryGateway.listPlayersByPlayerIds(seedPlayerIds).stream().collect(Collectors.toMap(PlayerData::getPlayerId, p -> p, (a, b) -> a));
        Map<String, String> tourMap = tournamentTourMap(tournamentIds);

        return seedData.stream()
                .filter(s -> s.getSeed() != null && s.getSeed() != 0)
                .map(s -> toSeedVO(s, playerMap, tourMap, eliminatedRoundMap))
                .sorted(Comparator.comparing(SeedVO::getSeed, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /**
     * 未结束比赛 + 今天已结束的比赛（与某场未结束比赛同一天），按计划开始时间升序。
     */
    public List<MatchQueryVO> upcomingMatches(List<String> tournamentIds) {
        if (CollectionUtils.isEmpty(tournamentIds)) return List.of();
        List<MatchData> unfinished = matchQueryGateway.listUnfinishedByTournamentIds(tournamentIds);
        Set<LocalDate> upcomingDates = unfinished.stream().map(MatchData::getMatchDate).filter(Objects::nonNull).collect(Collectors.toSet());

        List<MatchData> matches = new ArrayList<>(unfinished);
        if (!upcomingDates.isEmpty()) {
            matchQueryGateway.listFinishedByTournamentIds(tournamentIds).stream()
                    .filter(m -> m.getMatchDate() != null && upcomingDates.contains(m.getMatchDate()))
                    .forEach(matches::add);
        }

        List<MatchQueryVO> vos = toMatchVOs(matches, tournamentIds);
        vos.sort(Comparator.comparing(MatchQueryVO::getScheduledAt, Comparator.nullsLast(Comparator.naturalOrder())));
        return vos;
    }

    /**
     * 全部已结束比赛，按实际开始时间倒序。
     */
    public List<MatchQueryVO> finishedMatches(List<String> tournamentIds) {
        if (CollectionUtils.isEmpty(tournamentIds)) return List.of();
        List<MatchQueryVO> vos = toMatchVOs(matchQueryGateway.listFinishedByTournamentIds(tournamentIds), tournamentIds);
        vos.sort(Comparator.comparing(MatchQueryVO::getStartedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return vos;
    }

    // ==================== 分组视图：在扁平结果上做分组 ====================

    public List<SeedGroupDTO> seedGroups(List<String> tournamentIds) {
        List<SeedVO> seeds = seeds(tournamentIds);

        Map<String, List<SeedVO>> grouped = new LinkedHashMap<>();
        grouped.put("ATP", new ArrayList<>());
        grouped.put("WTA", new ArrayList<>());
        grouped.put("OUT", new ArrayList<>());
        for (SeedVO seed : seeds) {
            if (seed.getStatus() == SeedStatusEnum.ELIMINATED) {
                grouped.get("OUT").add(seed);
            } else if ("ATP".equalsIgnoreCase(seed.getTour())) {
                grouped.get("ATP").add(seed);
            } else {
                grouped.get("WTA").add(seed);
            }
        }

        List<SeedGroupDTO> result = new ArrayList<>();
        for (Map.Entry<String, List<SeedVO>> entry : grouped.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            SeedGroupDTO dto = new SeedGroupDTO();
            dto.setType(entry.getKey());
            dto.setData(entry.getValue());
            result.add(dto);
        }
        return result;
    }

    public List<MatchGroupDTO> upcomingDateGroups(List<String> tournamentIds) {
        List<MatchQueryVO> matches = upcomingMatches(tournamentIds);
        if (matches.isEmpty()) return List.of();
        Map<String, String> tourMap = tournamentTourMap(tournamentIds);

        // 先按日期分组（matches 已按 scheduledAt 升序，日期插入顺序天然升序）
        Map<String, List<MatchQueryVO>> dateMap = new LinkedHashMap<>();
        for (MatchQueryVO m : matches) {
            dateMap.computeIfAbsent(m.getDate() != null ? m.getDate() : "", k -> new ArrayList<>()).add(m);
        }

        LocalDate base = baseDate(dateMap.keySet());
        List<MatchGroupDTO> dateGroups = new ArrayList<>();
        for (Map.Entry<String, List<MatchQueryVO>> entry : dateMap.entrySet()) {
            MatchGroupDTO dateGroup = new MatchGroupDTO();
            dateGroup.setKey(entry.getKey());
            dateGroup.setName(dateLabel(entry.getKey(), base));
            dateGroup.setChildren(buildCourtGroups(entry.getValue(), tourMap));
            dateGroups.add(dateGroup);
        }
        dateGroups.sort(Comparator.comparing(g -> StringUtils.isNotBlank(g.getKey()) ? g.getKey() : "9999-99-99"));
        return dateGroups;
    }

    /** 锚点日期：数据中最早日期与今天取较早者，跨天比赛的过去日期据此显示为「今天」 */
    private LocalDate baseDate(Set<String> dates) {
        LocalDate base = LocalDate.now();
        for (String d : dates) {
            if (StringUtils.isBlank(d)) continue;
            LocalDate parsed = LocalDate.parse(d, DATE_FMT);
            if (parsed.isBefore(base)) base = parsed;
        }
        return base;
    }

    private String dateLabel(String date, LocalDate base) {
        if (StringUtils.isBlank(date)) return date;
        if (date.equals(base.format(DATE_FMT))) return "今天";
        if (date.equals(base.plusDays(1).format(DATE_FMT))) return "明天";
        return date;
    }

    private List<MatchGroupDTO> buildCourtGroups(List<MatchQueryVO> matches, Map<String, String> tourMap) {
        Map<String, List<MatchQueryVO>> courtMap = new LinkedHashMap<>();
        for (MatchQueryVO m : matches) {
            courtMap.computeIfAbsent(m.getCourt() != null ? m.getCourt() : "", k -> new ArrayList<>()).add(m);
        }

        List<MatchGroupDTO> courts = new ArrayList<>();
        Map<String, Integer> courtTourOrderMap = new HashMap<>();
        for (Map.Entry<String, List<MatchQueryVO>> entry : courtMap.entrySet()) {
            List<MatchQueryVO> courtMatches = entry.getValue();
            MatchGroupDTO dto = new MatchGroupDTO();
            dto.setKey(courtMatches.get(0).getCourt());
            dto.setName(courtMatches.get(0).getCourt());
            dto.setData(courtMatches);
            courts.add(dto);
            boolean hasAtp = courtMatches.stream().anyMatch(m -> "ATP".equalsIgnoreCase(tourMap.getOrDefault(m.getTournamentId(), "")));
            courtTourOrderMap.put(entry.getKey(), hasAtp ? 0 : 1);
        }

        courts.sort((a, b) -> {
            Integer seedA = getMinSeed(a.getData());
            Integer seedB = getMinSeed(b.getData());
            if (!Objects.equals(seedA, seedB)) {
                if (seedA == null) return 1;
                if (seedB == null) return -1;
                return seedA - seedB;
            }
            String keyA = a.getKey() != null ? a.getKey() : "";
            String keyB = b.getKey() != null ? b.getKey() : "";
            return courtTourOrderMap.getOrDefault(keyA, 2) - courtTourOrderMap.getOrDefault(keyB, 2);
        });

        return courts;
    }

    public List<MatchGroupDTO> finishedRoundGroups(List<String> tournamentIds) {
        List<MatchQueryVO> matches = finishedMatches(tournamentIds);
        if (matches.isEmpty()) return List.of();

        Map<String, List<MatchQueryVO>> roundMap = new LinkedHashMap<>();
        for (MatchQueryVO m : matches) {
            roundMap.computeIfAbsent(m.getRound() != null ? m.getRound() : "", k -> new ArrayList<>()).add(m);
        }

        List<MatchGroupDTO> rounds = new ArrayList<>();
        for (Map.Entry<String, List<MatchQueryVO>> entry : roundMap.entrySet()) {
            MatchGroupDTO dto = new MatchGroupDTO();
            dto.setKey(entry.getKey());
            dto.setName(TourRoundEnum.labelOf(entry.getKey()));
            dto.setData(entry.getValue());
            rounds.add(dto);
        }

        rounds.sort(Comparator.comparingInt(g -> roundOrder(g.getData().get(0).getRound())));
        return rounds;
    }

    // ==================== 私有：数据加载 + VO 构建 ====================

    private Map<String, String> tournamentTourMap(List<String> tournamentIds) {
        return tourTournamentGateway.listByTournamentIds(tournamentIds).stream().collect(Collectors.toMap(TournamentData::getTournamentId, data -> data.getTour() != null ? data.getTour() : "", (a, b) -> a));
    }

    private Map<String, String> buildEliminatedRoundMap(List<MatchData> finishedMatches) {
        Map<String, String> map = new HashMap<>();
        for (MatchData m : finishedMatches) {
            if (m.getWinnerId() == null) continue;
            if (m.getPlayer1Id() != null && !m.getWinnerId().equals(m.getPlayer1Id())) map.put(m.getPlayer1Id(), m.getRoundName());
            if (m.getPlayer2Id() != null && !m.getWinnerId().equals(m.getPlayer2Id())) map.put(m.getPlayer2Id(), m.getRoundName());
        }
        return map;
    }

    private SeedVO toSeedVO(PlayerSeedData s, Map<String, PlayerData> playerMap, Map<String, String> tourMap, Map<String, String> eliminatedRoundMap) {
        SeedVO seedVO = new SeedVO();
        seedVO.setPlayerId(s.getPlayerId());
        seedVO.setSeed(s.getSeed());
        seedVO.setTournamentId(s.getTournamentId());
        seedVO.setTour(tourMap.getOrDefault(s.getTournamentId(), ""));
        PlayerData player = playerMap.get(s.getPlayerId());
        if (player != null) {
            seedVO.setName(StringUtils.isNotBlank(player.getLastName()) ? player.getLastName() : player.getFirstName());
            seedVO.setCountry(CountryEnum.getCountry(player.getNationality()));
        }
        String eliminatedRound = eliminatedRoundMap.get(s.getPlayerId());
        if (eliminatedRound != null) {
            seedVO.setStatus(SeedStatusEnum.ELIMINATED);
            seedVO.setLabel(TourRoundEnum.labelOf(eliminatedRound));
        } else {
            seedVO.setStatus(SeedStatusEnum.ACTIVE);
        }
        return seedVO;
    }

    private List<MatchQueryVO> toMatchVOs(List<MatchData> matches, List<String> tournamentIds) {
        List<MatchData> valid = matches.stream().filter(m -> m.getPlayer1Id() != null || m.getPlayer2Id() != null).toList();
        if (valid.isEmpty()) return new ArrayList<>();

        Set<String> playerIds = new HashSet<>();
        for (MatchData m : valid) {
            if (m.getPlayer1Id() != null) playerIds.add(m.getPlayer1Id());
            if (m.getPlayer2Id() != null) playerIds.add(m.getPlayer2Id());
        }
        Map<String, PlayerData> playerMap = matchQueryGateway.listPlayersByPlayerIds(new ArrayList<>(playerIds)).stream().collect(Collectors.toMap(PlayerData::getPlayerId, p -> p, (a, b) -> a));
        Map<String, Integer> seedMap = matchQueryGateway.listSeedsByTournamentIds(tournamentIds).stream().collect(Collectors.toMap(s -> s.getTournamentId() + ":" + s.getPlayerId(), PlayerSeedData::getSeed, (a, b) -> a));
        List<Long> tourMatchIds = valid.stream().map(MatchData::getTourMatchId).filter(Objects::nonNull).toList();
        Map<Long, List<SetScoreData>> setScoreMap = matchQueryGateway.listSetScoresByTourMatchIds(tourMatchIds).stream().collect(Collectors.groupingBy(SetScoreData::getTourMatchId));

        List<MatchQueryVO> vos = new ArrayList<>();
        for (MatchData m : valid) {
            vos.add(toMatchVO(m, playerMap, setScoreMap, seedMap));
        }
        return vos;
    }

    private MatchQueryVO toMatchVO(MatchData match, Map<String, PlayerData> playerMap, Map<Long, List<SetScoreData>> setScoreMap, Map<String, Integer> seedMap) {
        MatchQueryVO vo = new MatchQueryVO();
        vo.setId(match.getMatchId());
        vo.setTournamentId(match.getTournamentId());
        vo.setCourt(match.getCourt());
        vo.setCourtSeq(match.getCourtSeq());
        vo.setRound(match.getRoundName());
        vo.setRoundShow(TourRoundEnum.labelOf(match.getRoundName()));
        vo.setDate(match.getMatchDate() != null ? match.getMatchDate().format(DATE_FMT) : null);
        vo.setStartedAt(match.getStartedAt());
        vo.setScheduledAt(match.getScheduledAt());
        MatchStatus matchStatus = MatchStatus.fromName(match.getStatus());
        if (matchStatus == MatchStatus.LIVE || matchStatus == MatchStatus.FINISHED) {
            vo.setStatusLabel(matchStatus.getLabel());
            vo.setScheduledShow(matchStatus.getLabel());
        } else {
            vo.setScheduledShow(ScheduledAtTextEnum.toShow(match.getScheduledAtText(), match.getScheduledAt()));
        }
        vo.setPlayer1(buildPlayerVO(match.getPlayer1Id(), match.getTournamentId(), playerMap, seedMap));
        vo.setPlayer2(buildPlayerVO(match.getPlayer2Id(), match.getTournamentId(), playerMap, seedMap));
        List<SetScoreData> setScoreList = setScoreMap.getOrDefault(match.getTourMatchId(), List.of());
        List<SetScoreVO> sets = setScoreList.stream().map(MatchConvertMapper.INSTANCE::toSetScoreVO).toList();
        vo.setSets(sets);
        vo.setStatus(match.getStatus());
        vo.setCurrentSet(CollectionUtils.isEmpty(sets) ? null : sets.size());
        vo.setCurrentSetScore(buildCurrentSetScore(sets));
        vo.setWinnerId(match.getWinnerId());
        vo.setDurationShow(durationShow(match.getDurationMinutes()));
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

    private String durationShow(Integer durationMinutes) {
        if (durationMinutes == null) return null;
        int hours = durationMinutes / 60;
        int mins = durationMinutes % 60;
        if (hours > 0) return hours + "h" + (mins > 0 ? mins + "m" : "");
        return mins + "m";
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

    private int roundOrder(String roundName) {
        TourRoundEnum[] values = TourRoundEnum.values();
        for (int i = 0; i < values.length; i++) {
            if (values[i].getRoundName().equals(roundName)) return i;
        }
        return values.length;
    }
}
