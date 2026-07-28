package com.rally.domain.tour;

import com.rally.domain.tour.model.TournamentData;
import com.rally.domain.tour.model.TournamentGroupData;
import com.rally.domain.tour.repository.TourTournamentRepository;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class TourTournamentQueryDomainService {

    private static final Map<String, Integer> CATEGORY_ORDER = Map.of("GS", 0, "1000", 1, "500", 2, "250", 3);
    private static final Comparator<TournamentData> TOURNAMENT_COMPARATOR = Comparator.comparingInt((TournamentData data) -> categoryOrder(data.getCategory()))
            .thenComparing(data -> normalizeCategory(data.getCategory()))
            .thenComparing(TournamentData::getStartDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(TournamentData::getEndDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(TournamentData::getTournamentId, Comparator.nullsLast(Comparator.naturalOrder()));

    @Resource
    private TourTournamentRepository tourTournamentRepository;

    public TournamentData findByTournamentId(String tournamentId) {
        return tourTournamentRepository.findByTournamentId(tournamentId);
    }

    public List<TournamentData> findValidCurrentTournaments(LocalDate date) {
        List<TournamentData> tournaments = tourTournamentRepository.findCurrentTournaments(date);
        return tournaments.stream().filter(data -> isCategoryKept(data.getCategory())).toList();
    }

    public List<TournamentGroupData> findValidCurrentTournamentGroups(LocalDate date) {
        return groupAndSortTournaments(findValidCurrentTournaments(date));
    }

    public List<TournamentGroupData> groupAndSortTournaments(List<TournamentData> tournaments) {
        if (tournaments == null || tournaments.isEmpty()) {
            return List.of();
        }
        List<TournamentData> validTournaments = tournaments.stream().filter(Objects::nonNull).toList();
        if (validTournaments.isEmpty()) {
            return List.of();
        }

        int[] parent = new int[validTournaments.size()];
        for (int i = 0; i < parent.length; i++) {
            parent[i] = i;
        }
        for (int i = 0; i < validTournaments.size(); i++) {
            for (int j = i + 1; j < validTournaments.size(); j++) {
                if (shouldMerge(validTournaments.get(i), validTournaments.get(j))) {
                    union(parent, i, j);
                }
            }
        }

        Map<Integer, List<TournamentData>> groupMap = new LinkedHashMap<>();
        for (int i = 0; i < validTournaments.size(); i++) {
            groupMap.computeIfAbsent(find(parent, i), key -> new ArrayList<>()).add(validTournaments.get(i));
        }

        List<TournamentGroupData> groups = groupMap.values().stream().map(group -> {
            List<TournamentData> sortedTournaments = group.stream().sorted(TOURNAMENT_COMPARATOR).toList();
            return new TournamentGroupData(sortedTournaments.get(0), sortedTournaments);
        }).sorted(Comparator.comparing(TournamentGroupData::getRepresentative, TOURNAMENT_COMPARATOR)).toList();
        return groups;
    }

    private boolean isCategoryKept(String category) {
        if (category == null || category.isBlank()) return true;
        try {
            return Integer.parseInt(category.trim()) >= 250;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private boolean shouldMerge(TournamentData first, TournamentData second) {
        if (first.getStartDate() == null || first.getEndDate() == null || second.getStartDate() == null || second.getEndDate() == null) {
            return false;
        }
        if (first.getCity() == null || first.getCity().isBlank() || second.getCity() == null || second.getCity().isBlank()) {
            return false;
        }
        boolean overlap = !first.getEndDate().isBefore(second.getStartDate()) && !second.getEndDate().isBefore(first.getStartDate());
        return overlap && first.getCity().trim().equalsIgnoreCase(second.getCity().trim());
    }

    private int find(int[] parent, int index) {
        while (parent[index] != index) {
            parent[index] = parent[parent[index]];
            index = parent[index];
        }
        return index;
    }

    private void union(int[] parent, int first, int second) {
        parent[find(parent, first)] = find(parent, second);
    }

    private static int categoryOrder(String category) {
        return CATEGORY_ORDER.getOrDefault(normalizeCategory(category), Integer.MAX_VALUE);
    }

    private static String normalizeCategory(String category) {
        return category == null ? "" : category.trim().toUpperCase(Locale.ROOT);
    }
}
