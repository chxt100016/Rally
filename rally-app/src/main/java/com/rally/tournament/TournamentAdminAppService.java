package com.rally.tournament;

import com.rally.domain.meetup.model.PageDTO;
import com.rally.domain.notify.enums.NoticeScene;
import com.rally.domain.notify.enums.NotifyBizType;
import com.rally.domain.notify.service.NotifySubscribeService;
import com.rally.domain.tournament.model.*;
import com.rally.domain.tournament.service.TournamentAdminService;
import com.rally.domain.tournament.service.TournamentBatchMatchService;
import com.rally.domain.tournament.service.TournamentOfflineMeetupService;
import com.rally.notify.TournamentNotifyAssembler;
import com.rally.tournament.convert.TournamentAppConvertMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 赛事管理（运营后台）写流程编排：创建/编辑/激活/废弃/列表
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TournamentAdminAppService {

    private final TournamentAdminService tournamentAdminService;

    private final TournamentOfflineMeetupService tournamentOfflineMeetupService;

    private final TournamentBatchMatchService tournamentBatchMatchService;

    private final NotifySubscribeService notifySubscribeService;

    /**
     * 创建赛事草稿
     */
    @Transactional
    public TournamentIdDTO create(TournamentCreateCmd cmd) {
        Tournament tournament = tournamentAdminService.create(cmd);
        return new TournamentIdDTO(tournament.getTournamentId());
    }

    /**
     * 编辑草稿
     */
    @Transactional
    public void update(TournamentUpdateCmd cmd) {
        tournamentAdminService.update(cmd);
    }

    /**
     * 激活赛事
     */
    @Transactional
    public void activate(TournamentActivateCmd cmd) {
        tournamentAdminService.activate(cmd.getTournamentId());
    }

    /**
     * 废弃赛事
     */
    @Transactional
    public void abandon(TournamentAbandonCmd cmd) {
        tournamentAdminService.abandon(cmd.getTournamentId());
    }

    /** 创建线下赛活动并自动加入所有达到线下赛轮次的参赛者。 */
    @Transactional
    public String createOfflineMeetup(TournamentOfflineMeetupCmd cmd) {
        return tournamentOfflineMeetupService.create(cmd);
    }

    /**
     * 批量匹配所有已到开始时间的赛事，并在每批比赛落地后触发匹配通知。
     * Job 与运营后台手动接口统一调用此入口。
     */
    public synchronized void runTournamentMatch() {
        List<TournamentData> tournaments = tournamentBatchMatchService.listTournamentsToMatch(LocalDateTime.now());
        for (TournamentData tournament : tournaments) {
            try {
                notifyMatched(tournament, tournamentBatchMatchService.matchQualifier(tournament.getBizId()));
                notifyMatched(tournament, tournamentBatchMatchService.matchMainRoundsAll(tournament.getBizId()));
            } catch (Exception e) {
                log.error("赛事匹配失败 tournamentId={}", tournament.getBizId(), e);
            }
        }
    }

    private void notifyMatched(TournamentData tournament, List<TournamentMatch> matches) {
        if (matches.isEmpty()) {
            return;
        }
        List<String> userIds = matches.stream()
                .flatMap(match -> match.getParticipants().stream())
                .map(MatchParticipantData::getUserId)
                .distinct()
                .toList();
        notifySubscribeService.notify(NotifyBizType.TOURNAMENT, tournament.getBizId(),
                NoticeScene.TOURNAMENT_MATCHED, userIds,
                TournamentNotifyAssembler.matchedData(tournament.getTournamentName()));
    }

    /**
     * 后台赛事列表
     */
    public PageDTO<TournamentAdminItemDTO> list(TournamentAdminListCmd cmd) {
        PageDTO<TournamentData> pageData = tournamentAdminService.pageList(cmd);
        return new PageDTO<>(TournamentAppConvertMapper.INSTANCE.toTournamentAdminItemDTOList(pageData.getList()), pageData.getTotal(), pageData.getHasMore());
    }
}
