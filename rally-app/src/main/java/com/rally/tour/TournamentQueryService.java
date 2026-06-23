package com.rally.tour;

import com.rally.client.qiniu.QiniuClient;
import com.rally.domain.tour.gateway.TourTournamentGateway;
import com.rally.domain.tour.model.TournamentData;
import com.rally.domain.tour.model.TournamentDTO;
import com.rally.domain.translation.model.TranslationLanguageEnum;
import com.rally.tour.convert.TournamentConvertMapper;
import com.rally.translation.TourTranslationService;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class TournamentQueryService {

    @Resource
    private TourTournamentGateway tourTournamentGateway;

    @Resource
    private TourTranslationService tourTranslationService;

    @Resource
    private QiniuClient qiniuClient;

    public List<TournamentDTO> queryTournaments(String status, String type, String range) {
        String dbStatus = resolveDbStatus(status);
        LocalDate dateFrom = null;
        LocalDate dateTo = null;
        if ("recent".equalsIgnoreCase(range)) {
            LocalDate today = LocalDate.now();
            dateFrom = today.minusMonths(1);
            dateTo = today.plusMonths(1);
        }

        List<TournamentData> list = tourTournamentGateway.listByCondition(dbStatus, type, dateFrom, dateTo);
        if (CollectionUtils.isEmpty(list)) {
            return List.of();
        }

        list = list.stream().filter(data -> isCategoryKept(data.getCategory())).toList();
        if (CollectionUtils.isEmpty(list)) {
            return List.of();
        }

        List<TournamentDTO> result = new ArrayList<>();
        List<List<TournamentData>> groups = groupByCityAndName(list);

        for (List<TournamentData> group : groups) {
            String groupId = "g" + (result.size() + 1);
            for (TournamentData data : group) {
                result.add(TournamentConvertMapper.INSTANCE.toDTO(data, groupId, qiniuClient));
            }
        }

        tourTranslationService.tournaments(result, TranslationLanguageEnum.ZH_CN);
        return result;
    }

    private String resolveDbStatus(String status) {
        if (status == null) return null;
        return switch (status) {
            case "FINISHED" -> "completed";
            case "ONGOING", "UPCOMING" -> "active";
            default -> null;
        };
    }

    private boolean isCategoryKept(String category) {
        if (category == null || category.isBlank()) return true;
        try {
            return Integer.parseInt(category.trim()) >= 250;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private List<List<TournamentData>> groupByCityAndName(List<TournamentData> list) {
        List<List<TournamentData>> groups = new ArrayList<>();
        for (TournamentData data : list) {
            boolean added = false;
            for (List<TournamentData> group : groups) {
                if (isSameGroup(group.get(0), data)) {
                    group.add(data);
                    added = true;
                    break;
                }
            }
            if (!added) {
                List<TournamentData> newGroup = new ArrayList<>();
                newGroup.add(data);
                groups.add(newGroup);
            }
        }
        for (List<TournamentData> group : groups) {
            group.sort(Comparator.comparing(TournamentData::getStartDate, Comparator.nullsLast(Comparator.naturalOrder())));
        }
        return groups;
    }

    private boolean isSameGroup(TournamentData a, TournamentData b) {
        String cityA = a.getCity() != null ? a.getCity().toLowerCase() : "";
        String cityB = b.getCity() != null ? b.getCity().toLowerCase() : "";
        if (!cityA.equals(cityB)) return false;
        if (a.getStartDate() == null || b.getStartDate() == null || a.getEndDate() == null || b.getEndDate() == null) return false;
        return !a.getStartDate().isAfter(b.getEndDate()) && !b.getStartDate().isAfter(a.getEndDate());
    }
}
