package com.rally.domain.tour.tournament;

import java.time.LocalDate;

/** C1 整体替换的来源主档；不包含由内容生产维护的图片绑定。 */
public record TourTournamentProfile(
        String name,
        String tour,
        String category,
        String surface,
        String city,
        String country,
        Integer prizeMoney,
        String prizeMoneyText,
        TourTournamentStatus status,
        LocalDate startDate,
        LocalDate endDate) {

    public TourTournamentProfile {
        name = required(name, 100, "赛事名称");
        tour = required(tour, 10, "巡回赛");
        category = optionalOrEmpty(category, 20, "赛事级别");
        surface = required(surface, 10, "场地类型");
        city = required(city, 50, "举办城市");
        country = optional(country, 32, "举办国家");
        require(prizeMoney == null || prizeMoney >= 0, "赛事奖金不得为负数");
        prizeMoneyText = optional(prizeMoneyText, 50, "奖金展示文本");
        require(status != null, "赛事状态不能为空");
        require(startDate != null, "赛事开始日不能为空");
        require(endDate != null, "赛事结束日不能为空");
        require(!startDate.isAfter(endDate), "赛事开始日不得晚于结束日");
    }

    public static TourTournamentProfile from(RefreshTourTournamentCommand command) {
        if (command == null) {
            throw invalid("刷新赛事名录命令不能为空");
        }
        return new TourTournamentProfile(
                command.name(),
                command.tour(),
                command.category(),
                command.surface(),
                command.city(),
                command.country(),
                command.prizeMoney(),
                command.prizeMoneyText(),
                TourTournamentStatus.fromSource(command.status()),
                command.startDate(),
                command.endDate());
    }

    private static String required(String value, int maxLength, String fieldName) {
        require(value != null && !value.isBlank(), fieldName + "不能为空");
        String normalized = value.strip();
        require(normalized.length() <= maxLength,
                fieldName + "长度不能超过 " + maxLength);
        return normalized;
    }

    private static String optionalOrEmpty(String value, int maxLength, String fieldName) {
        String normalized = optional(value, maxLength, fieldName);
        return normalized == null ? "" : normalized;
    }

    private static String optional(String value, int maxLength, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        require(normalized.length() <= maxLength,
                fieldName + "长度不能超过 " + maxLength);
        return normalized;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw invalid(message);
        }
    }

    private static TourTournamentDomainException invalid(String message) {
        return new TourTournamentDomainException(
                TourTournament.TOUR_TOURNAMENT_PROFILE_INVALID, message);
    }
}
