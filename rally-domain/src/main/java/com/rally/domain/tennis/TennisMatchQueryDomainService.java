package com.rally.domain.tennis;

import com.rally.domain.tennis.model.*;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TennisMatchQueryDomainService {

    @Resource
    private MatchDataLoader matchDataLoader;

    public TennisMatchDTO getUpcoming(List<String> tournamentIds) {
        TennisMatchDTO dto = emptyDTO();
        if (CollectionUtils.isEmpty(tournamentIds)) return dto;
        MatchDataLoader.MatchLoadResult loaded = matchDataLoader.loadAndSplit(tournamentIds);
        if (loaded.empty) return dto;
        dto.setSeed(buildSeedGroups(loaded));
        dto.setMatch(buildCourtGroups(loaded));
        return dto;
    }

    public TennisMatchDTO getFinished(List<String> tournamentIds) {
        TennisMatchDTO dto = emptyDTO();
        if (CollectionUtils.isEmpty(tournamentIds)) return dto;
        MatchDataLoader.MatchLoadResult loaded = matchDataLoader.loadAndSplit(tournamentIds);
        if (loaded.empty) return dto;
        dto.setSeed(buildSeedGroups(loaded));
        dto.setMatch(buildRoundGroups(loaded));
        return dto;
    }

    private TennisMatchDTO emptyDTO() {
        TennisMatchDTO dto = new TennisMatchDTO();
        dto.setSeed(List.of());
        dto.setMatch(List.of());
        return dto;
    }

    private List<MatchGroupDTO> buildCourtGroups(MatchDataLoader.MatchLoadResult loaded) {
        List<MatchQueryVO> upcomingMatches = loaded.upcomingMatches;
        upcomingMatches.sort(Comparator.comparing(MatchQueryVO::getScheduledAt, Comparator.nullsLast(Comparator.naturalOrder())));

        Map<String, List<MatchQueryVO>> courtMap = new LinkedHashMap<>();
        for (MatchQueryVO m : upcomingMatches) {
            courtMap.computeIfAbsent(m.getCourt() != null ? m.getCourt() : "", k -> new ArrayList<>()).add(m);
        }

        List<MatchGroupDTO> courts = new ArrayList<>();
        Map<String, Integer> courtTourOrderMap = new HashMap<>();
        for (Map.Entry<String, List<MatchQueryVO>> entry : courtMap.entrySet()) {
            List<MatchQueryVO> courtMatches = entry.getValue();
            MatchGroupDTO dto = new MatchGroupDTO();
            dto.setName(courtMatches.get(0).getCourt());
            dto.setData(courtMatches);
            courts.add(dto);
            boolean hasAtp = courtMatches.stream().anyMatch(m -> "ATP".equalsIgnoreCase(loaded.tournamentTourMap.getOrDefault(m.getTournamentId(), "")));
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
            String keyA = a.getName() != null ? a.getName() : "";
            String keyB = b.getName() != null ? b.getName() : "";
            return courtTourOrderMap.getOrDefault(keyA, 2) - courtTourOrderMap.getOrDefault(keyB, 2);
        });

        return courts;
    }

    private List<MatchGroupDTO> buildRoundGroups(MatchDataLoader.MatchLoadResult loaded) {
        List<MatchQueryVO> finishedMatches = loaded.finishedMatches;
        finishedMatches.sort(Comparator.comparing(MatchQueryVO::getStartedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        Map<String, List<MatchQueryVO>> roundMap = new LinkedHashMap<>();
        for (MatchQueryVO m : finishedMatches) {
            roundMap.computeIfAbsent(m.getRound() != null ? m.getRound() : "", k -> new ArrayList<>()).add(m);
        }

        List<MatchGroupDTO> rounds = new ArrayList<>();
        for (Map.Entry<String, List<MatchQueryVO>> entry : roundMap.entrySet()) {
            MatchGroupDTO dto = new MatchGroupDTO();
            dto.setName(TennisRoundEnum.labelOf(entry.getKey()));
            dto.setData(entry.getValue());
            rounds.add(dto);
        }

        rounds.sort(Comparator.comparingInt(g -> roundOrder(g.getData().get(0).getRound())));
        return rounds;
    }

    private List<SeedGroupDTO> buildSeedGroups(MatchDataLoader.MatchLoadResult loaded) {
        Map<String, String> eliminatedRoundMap = new HashMap<>();
        for (MatchQueryVO m : loaded.finishedMatches) {
            if (m.getWinnerId() == null) continue;
            if (m.getPlayer1() != null && !m.getWinnerId().equals(m.getPlayer1().getId())) eliminatedRoundMap.put(m.getPlayer1().getId(), m.getRound());
            if (m.getPlayer2() != null && !m.getWinnerId().equals(m.getPlayer2().getId())) eliminatedRoundMap.put(m.getPlayer2().getId(), m.getRound());
        }

        List<SeedVO> allSeeds = loaded.seeds.stream()
                .filter(s -> s.getSeed() != null && s.getSeed() != 0)
                .map(s -> {
                    SeedVO seedVO = new SeedVO();
                    seedVO.setPlayerId(s.getPlayerId());
                    seedVO.setSeed(s.getSeed());
                    seedVO.setTournamentId(s.getTournamentId());
                    seedVO.setTour(loaded.tournamentTourMap.getOrDefault(s.getTournamentId(), ""));
                    PlayerData player = loaded.playerMap.get(s.getPlayerId());
                    if (player != null) {
                        seedVO.setName(StringUtils.isNotBlank(player.getLastName()) ? player.getLastName() : player.getFirstName());
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

        Map<String, List<SeedVO>> grouped = new LinkedHashMap<>();
        grouped.put("ATP", new ArrayList<>());
        grouped.put("WTA", new ArrayList<>());
        grouped.put("OUT", new ArrayList<>());

        for (SeedVO seed : allSeeds) {
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
        TennisRoundEnum[] values = TennisRoundEnum.values();
        for (int i = 0; i < values.length; i++) {
            if (values[i].getRoundName().equals(roundName)) return i;
        }
        return values.length;
    }
}
