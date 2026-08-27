package com.rally.tournament.offlinemeetupcreate.activity;

import com.rally.domain.tournament.model.TournamentOfflineMeetupCmd;
import com.rally.domain.tournament.service.TournamentOfflineMeetupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 业务活动 create-offline-meetup：创建线下赛活动、加入候选成员并绑定赛事。
 */
@Component
@RequiredArgsConstructor
public class CreateOfflineMeetupActivity {

    private final TournamentOfflineMeetupService tournamentOfflineMeetupService;

    /**
     * 保持活动、成员和赛事关联在同一本地事务中成功或回滚。
     */
    @Transactional(rollbackFor = Exception.class)
    public String execute(TournamentOfflineMeetupCmd command) {
        /*
         * A1-A5：既有流程依次校验线下轮次与唯一关联，筛选并按
         * userId 去重 WAITING 候选人，将赛事 NTRP 映射为精确等级，
         * 按请求的时间、场地、城市与人数构造 TOURNAMENT/OPEN 活动，
         * 直接保存 JOINED 成员，最后以 offline_meetup_id IS NULL 条件绑定。
         * 这里不加强入参、赛事状态、报名阶段、费用或创建人资格校验，
         * 继续暴露原有业务错误和并发重复错误。
         */
        return tournamentOfflineMeetupService.create(command);
    }
}
