package com.rally.domain.tour.match;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 经格式校验的非身份快照补丁；null 表示保留存量。 */
record TourMatchSnapshot(
        Integer roundNumber,
        String roundName,
        String player1Id,
        String player2Id,
        String winnerId,
        LocalDateTime scheduledAt,
        String scheduledAtText,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        String court,
        Integer courtSeq,
        TourMatchStatus status,
        Integer durationMinutes,
        String description,
        LocalDate matchDate,
        String setsJson) {

    static TourMatchSnapshot from(RefreshTourMatchCommand command) {
        require(command != null, "比赛快照命令不能为空");
        require(command.roundNumber() == null
                        || command.roundNumber() > 0 && command.roundNumber() <= Byte.MAX_VALUE,
                "轮次序号必须在 1 到 127 之间");
        require(command.courtSeq() == null
                        || command.courtSeq() > 0 && command.courtSeq() <= Byte.MAX_VALUE,
                "场序必须在 1 到 127 之间");
        require(command.durationMinutes() == null
                        || command.durationMinutes() >= 0
                        && command.durationMinutes() <= Short.MAX_VALUE,
                "比赛时长必须在 0 到 32767 分钟之间");

        return new TourMatchSnapshot(
                command.roundNumber(),
                normalizeText(command.roundName(), 32, "轮次名称"),
                normalizeText(command.player1Id(), 50, "球员 1 编号"),
                normalizeText(command.player2Id(), 50, "球员 2 编号"),
                normalizeText(command.winnerId(), 50, "胜方球员编号"),
                command.scheduledAt(),
                normalizeText(command.scheduledAtText(), 50, "排期原文"),
                command.startedAt(),
                command.endedAt(),
                normalizeText(command.court(), 50, "球场名称"),
                command.courtSeq(),
                TourMatchStatus.recognizePatch(command.status()),
                command.durationMinutes(),
                normalizeText(command.description(), null, "比赛描述"),
                command.matchDate(),
                normalizeSetsJson(command.setsJson()));
    }

    private static String normalizeText(String value, Integer maxLength, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        require(maxLength == null || normalized.length() <= maxLength,
                fieldName + "长度不能超过 " + maxLength);
        return normalized;
    }

    private static String normalizeSetsJson(String setsJson) {
        if (setsJson == null || setsJson.isBlank()) {
            return null;
        }
        String normalized = setsJson.strip();
        require(JSON.isValidArray(normalized), "盘分必须是有效的 JSON 数组");
        JSONArray sets = JSON.parseArray(normalized);
        return sets.isEmpty() ? null : normalized;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new TourMatchDomainException(
                    TourMatch.TOUR_MATCH_SNAPSHOT_INVALID, message);
        }
    }
}
