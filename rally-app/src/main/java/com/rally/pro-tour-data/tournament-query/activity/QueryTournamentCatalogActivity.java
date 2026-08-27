package com.rally.protourdata.tournamentquery.activity;

import com.rally.client.qiniu.QiniuClient;
import com.rally.domain.tour.model.TournamentData;
import com.rally.domain.tour.model.TournamentDTO;
import com.rally.domain.tour.repository.TourTournamentRepository;
import com.rally.domain.translation.model.TranslationEntityTypeEnum;
import com.rally.domain.translation.model.TranslationKey;
import com.rally.domain.translation.model.TranslationLanguageEnum;
import com.rally.tour.convert.TournamentConvertMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 业务活动 query-tournament-catalog：筛选、分组并组装职业赛事展示清单。 */
@Component
@RequiredArgsConstructor
public class QueryTournamentCatalogActivity {

    private static final TranslationLanguageEnum TARGET_LANGUAGE = TranslationLanguageEnum.ZH_CN;

    private final TourTournamentRepository tourTournamentRepository;
    private final QiniuClient qiniuClient;

    public Result execute(String status, String type, String range) {
        // A1：保持 main 的状态解释、type 原样精确匹配、日期窗口及 start_date 升序查询语义。
        String dbStatus = resolveDbStatus(status);
        LocalDate dateFrom = null;
        LocalDate dateTo = null;
        if ("recent".equalsIgnoreCase(range)) {
            LocalDate today = LocalDate.now();
            dateFrom = today.minusMonths(1);
            dateTo = today.plusMonths(1);
        } else if ("live".equalsIgnoreCase(range)) {
            LocalDate today = LocalDate.now();
            dbStatus = "active";
            dateFrom = today;
            dateTo = today;
        }

        List<TournamentData> tournaments = tourTournamentRepository
                .listByCondition(dbStatus, type, dateFrom, dateTo);
        if (CollectionUtils.isEmpty(tournaments)) {
            return Result.empty();
        }

        // A2：仅排除可解析为整数且小于 250 的类别，空白和非数字均继续展示。
        tournaments = tournaments.stream()
                .filter(tournament -> isCategoryKept(tournament.getCategory()))
                .toList();
        if (CollectionUtils.isEmpty(tournaments)) {
            return Result.empty();
        }

        // A3：新赛事只和每组首项比较；同城且赛期相交才合组，组内空开始日晚排。
        List<List<TournamentData>> groups = groupByCityAndPeriod(tournaments);

        // A4：沿用既有 DTO 映射、日期状态推导及 3600 秒背景图签名行为。
        List<TournamentDTO> result = new ArrayList<>();
        for (List<TournamentData> group : groups) {
            String groupId = "g" + (result.size() + 1);
            for (TournamentData tournament : group) {
                result.add(TournamentConvertMapper.INSTANCE.toDTO(tournament, groupId, qiniuClient));
            }
        }

        return new Result(result, collectTranslationKeys(result));
    }

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

    private boolean isCategoryKept(String category) {
        if (category == null || category.isBlank()) {
            return true;
        }
        try {
            return Integer.parseInt(category.trim()) >= 250;
        } catch (NumberFormatException ignored) {
            return true;
        }
    }

    private List<List<TournamentData>> groupByCityAndPeriod(List<TournamentData> tournaments) {
        List<List<TournamentData>> groups = new ArrayList<>();
        for (TournamentData tournament : tournaments) {
            boolean added = false;
            for (List<TournamentData> group : groups) {
                if (isSameGroup(group.get(0), tournament)) {
                    group.add(tournament);
                    added = true;
                    break;
                }
            }
            if (!added) {
                List<TournamentData> group = new ArrayList<>();
                group.add(tournament);
                groups.add(group);
            }
        }
        for (List<TournamentData> group : groups) {
            group.sort(Comparator.comparing(
                    TournamentData::getStartDate,
                    Comparator.nullsLast(Comparator.naturalOrder())));
        }
        return groups;
    }

    private boolean isSameGroup(TournamentData first, TournamentData candidate) {
        String firstCity = first.getCity() == null ? "" : first.getCity().toLowerCase();
        String candidateCity = candidate.getCity() == null ? "" : candidate.getCity().toLowerCase();
        if (!firstCity.equals(candidateCity)) {
            return false;
        }
        if (first.getStartDate() == null || first.getEndDate() == null
                || candidate.getStartDate() == null || candidate.getEndDate() == null) {
            return false;
        }
        return !first.getStartDate().isAfter(candidate.getEndDate())
                && !candidate.getStartDate().isAfter(first.getEndDate());
    }

    private Set<TranslationKey> collectTranslationKeys(List<TournamentDTO> tournaments) {
        Set<TranslationKey> keys = new LinkedHashSet<>();
        for (TournamentDTO tournament : tournaments) {
            keys.add(new TranslationKey(
                    TranslationEntityTypeEnum.TOURNAMENT, tournament.getName(), TARGET_LANGUAGE));
            keys.add(new TranslationKey(
                    TranslationEntityTypeEnum.CITY, tournament.getCity(), TARGET_LANGUAGE));
            keys.add(new TranslationKey(
                    TranslationEntityTypeEnum.SURFACE, tournament.getSurfaceLabel(), TARGET_LANGUAGE));
        }
        return keys;
    }

    /** 展示列表及交给后续翻译活动的去重简中查询键。 */
    public record Result(List<TournamentDTO> tournaments, Set<TranslationKey> translationKeys) {
        public Result {
            tournaments = tournaments == null ? List.of() : List.copyOf(tournaments);
            translationKeys = translationKeys == null
                    ? Set.of()
                    : Collections.unmodifiableSet(new LinkedHashSet<>(translationKeys));
        }

        public static Result empty() {
            return new Result(List.of(), Set.of());
        }
    }
}
