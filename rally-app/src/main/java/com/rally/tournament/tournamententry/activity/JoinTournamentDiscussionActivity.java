package com.rally.tournament.tournamententry.activity;

import com.rally.domain.meetup.service.ChatDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 业务活动 join-tournament-discussion：为新报名用户建立赛事讨论成员关系。 */
@Component
@RequiredArgsConstructor
public class JoinTournamentDiscussionActivity {

    private final ChatDomainService chatDomainService;

    public void execute(String tournamentId, String userId) {
        // A1-A3：按 refId+userId 查重，并以雪花业务编号、空阅读位置和零未读建立成员。
        chatDomainService.join(tournamentId, userId);

        // 重复成员或保存失败继续向外抛出，由报名入口的同一事务回滚报名及搭档关系。
    }
}
