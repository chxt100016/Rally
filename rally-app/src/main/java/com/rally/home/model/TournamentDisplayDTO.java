package com.rally.home.model;

import com.alibaba.fastjson2.annotation.JSONField;
import com.rally.domain.tour.model.MatchQueryVO;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class TournamentDisplayDTO {
    private String tournamentId;
    private String tournamentName;
    private String category;
    private String tour;
    private String courtName;
    @JSONField(format = "yyyy-MM-dd")
    private LocalDate matchDate;
    private List<MatchQueryVO> matches;
}
