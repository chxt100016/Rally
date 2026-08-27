package com.rally.domain.tournament.model;

import com.rally.domain.meetup.enums.MatchTypeEnum;
import com.rally.domain.tournament.enums.TournamentDisplayStatusEnum;
import com.rally.domain.tournament.enums.TournamentGenderLimitEnum;
import com.rally.domain.tournament.enums.TournamentRoundEnum;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 赛事公开基础信息
 */
@Data
public class TournamentDTO {
    private String tournamentId;
    private String tournamentName;
    private String posterUrl;
    /** 规则海报签名访问地址；未配置时为 null。 */
    private String rulePosterUrl;
    private String wechatGroupQrCodeUrl;
    /** 赛事落地页主题；未配置时为 null。 */
    private TournamentThemeConfig theme;
    private MatchTypeEnum matchType;
    private String matchTypeShow;
    private String cityName;
    private String ntrpLevel;
    private TournamentGenderLimitEnum genderLimit;
    private String genderLimitShow;
    private Long entryFee;
    /** 奖金拆分数组，按名次顺序，第1项为第一名奖金 */
    private List<Long> prizeMoneyList;
    private LocalDateTime registrationStartTime;
    private LocalDateTime registrationEndTime;
    private LocalDateTime qualifierStartTime;
    private LocalDateTime qualifierEndTime;
    private TournamentRoundEnum offlineFromRound;
    private String offlineFromRoundShow;
    /** 冠军报名编号；赛事未结束时为 null。 */
    private Integer championEntryNo;
    /** 冠军成员；尚未产生冠军或冠军报名缺失时为空列表。 */
    private List<TournamentChampionUserDTO> championUsers = List.of();
    private String matchRuleDescription;
    private TournamentDisplayStatusEnum displayStatus;
    private String displayStatusShow;
}
