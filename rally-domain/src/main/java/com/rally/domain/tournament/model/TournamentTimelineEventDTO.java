package com.rally.domain.tournament.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 个人视角事件流条目
 */
@Data
@AllArgsConstructor
public class TournamentTimelineEventDTO {
    private LocalDateTime time;
    private String description;
}
