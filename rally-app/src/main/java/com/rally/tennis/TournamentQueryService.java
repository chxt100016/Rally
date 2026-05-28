package com.rally.tennis;

import com.rally.client.qiniu.QiniuClient;
import com.rally.db.tennis.entity.TennisTournamentPO;
import com.rally.db.tennis.repository.TennisTournamentRepository;
import com.rally.domain.tennis.model.TournamentDTO;
import com.rally.domain.translation.model.TranslationLanguageEnum;
import com.rally.tennis.convert.TournamentConvertMapper;
import com.rally.translation.TennisTranslationService;
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
    private TennisTournamentRepository tennisTournamentRepository;

    @Resource
    private TennisTranslationService tennisTranslationService;

    @Resource
    private QiniuClient qiniuClient;

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
                result.add(TournamentConvertMapper.INSTANCE.toDTO(po, groupId, qiniuClient));
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
     * category 过滤：非数字或数字 >= 250 才保留
     */
    private boolean isCategoryKept(String category) {
        if (category == null || category.isBlank()) {
            return true;
        }
        try {
            return Integer.parseInt(category.trim()) >= 250;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    /**
     * 按城市和时间分组：city 不区分大小写相同 且 startDate 和 endDate 时间重合算作同一分组
     */
    private List<List<TennisTournamentPO>> groupByCityAndName(List<TennisTournamentPO> list) {
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
        String cityA = a.getCity() != null ? a.getCity().toLowerCase() : "";
        String cityB = b.getCity() != null ? b.getCity().toLowerCase() : "";
        if (!cityA.equals(cityB)) {
            return false;
        }

        if (a.getStartDate() == null || b.getStartDate() == null ||
            a.getEndDate() == null || b.getEndDate() == null) {
            return false;
        }

        // 时间重合判断：a.start <= b.end && b.start <= a.end
        return !a.getStartDate().isAfter(b.getEndDate()) && !b.getStartDate().isAfter(a.getEndDate());
    }

}
