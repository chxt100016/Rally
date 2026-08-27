package com.rally.tournament.tournamentwithdraw.activity;

import com.rally.domain.meetup.service.ChatDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 业务活动 leave-tournament-discussion：移除退赛用户的赛事讨论成员关系。 */
@Component
@RequiredArgsConstructor
public class LeaveTournamentDiscussionActivity {

    private final ChatDomainService chatDomainService;

    /**
     * 按赛事频道与用户直接移除成员及阅读状态，历史评论保持不变。
     */
    public void execute(String tournamentId, String userId) {
        /*
         * A1-A2：领域命令按 refId+userId 物理删除成员关系及其阅读状态；
         * 成员不存在或仓储删除零行时幂等成功。
         */
        chatDomainService.quit(tournamentId, userId);

        /*
         * A3：本活动不操作评论仓储。调用失败继续向外抛出，由退赛入口的
         * 同一事务回滚报名、讨论成员及后续比赛联动。
         */
    }
}
