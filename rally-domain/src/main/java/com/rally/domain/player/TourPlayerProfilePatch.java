package com.rally.domain.tour.player;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * C1 的规范化资料补丁。所有 null 字段都表示来源未提供有效补丁，持久化时不得清空存量。
 */
public record TourPlayerProfilePatch(
        String firstName,
        String lastName,
        String nationality,
        LocalDate birthDate,
        String gender,
        Integer rank,
        Integer points,
        String hand) {

    private static final int NAME_MAX_LENGTH = 50;

    public static TourPlayerProfilePatch from(RefreshTourPlayerCommand command) {
        if (command == null) {
            throw new TourPlayerDomainException(
                    TourPlayer.TOUR_PLAYER_PROFILE_INVALID,
                    "刷新球员资料命令不能为空");
        }
        return new TourPlayerProfilePatch(
                normalizeName(command.firstName(), "名"),
                normalizeName(command.lastName(), "姓"),
                normalizeNationality(command.nationality()),
                parseBirthDateOrNull(command.birthDate()),
                normalizeEnum(command.gender(), "性别", "M", "F"),
                validateRank(command.rank()),
                validatePoints(command.points()),
                normalizeEnum(command.hand(), "持拍手", "RIGHT", "LEFT", "UNKNOWN"));
    }

    public boolean isEmpty() {
        return firstName == null
                && lastName == null
                && nationality == null
                && birthDate == null
                && gender == null
                && rank == null
                && points == null
                && hand == null;
    }

    private static String normalizeName(String value, String fieldName) {
        String normalized = normalizeOptional(value);
        require(normalized == null || normalized.length() <= NAME_MAX_LENGTH,
                fieldName + "长度不能超过 50");
        return normalized;
    }

    private static String normalizeNationality(String value) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        require(normalized.matches("[A-Z]{3}"),
                "国籍必须是三位大写代码");
        return normalized;
    }

    private static LocalDate parseBirthDateOrNull(String value) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            return null;
        }
        try {
            return LocalDate.parse(normalized);
        } catch (DateTimeParseException exception) {
            // 来源日期解析失败不构成清空或拒绝：按“未提供该补丁”处理。
            return null;
        }
    }

    private static String normalizeEnum(
            String value, String fieldName, String... supportedValues) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        for (String supportedValue : supportedValues) {
            if (supportedValue.equals(normalized)) {
                return normalized;
            }
        }
        throw new TourPlayerDomainException(
                TourPlayer.TOUR_PLAYER_PROFILE_INVALID,
                fieldName + "不是受支持的枚举值");
    }

    private static Integer validateRank(Integer rank) {
        if (rank != null && rank <= 0) {
            throw new TourPlayerDomainException(
                    TourPlayer.TOUR_PLAYER_RANKING_INVALID,
                    "排名必须为正数");
        }
        return rank;
    }

    private static Integer validatePoints(Integer points) {
        if (points != null && points < 0) {
            throw new TourPlayerDomainException(
                    TourPlayer.TOUR_PLAYER_RANKING_INVALID,
                    "积分不得为负数");
        }
        return points;
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new TourPlayerDomainException(
                    TourPlayer.TOUR_PLAYER_PROFILE_INVALID, message);
        }
    }
}
