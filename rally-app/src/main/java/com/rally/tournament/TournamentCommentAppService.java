package com.rally.tournament;

import com.rally.domain.tournament.model.TournamentCommentDTO;
import com.rally.domain.tournament.model.TournamentCommentPublishCmd;
import com.rally.domain.tournament.model.TournamentCommentListDTO;
import com.rally.tournament.commentpull.activity.ListCommentsActivity;
import com.rally.tournament.commentpublish.activity.PublishCommentActivity;
import com.rally.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 赛事评论应用服务：负责参赛权限，消息与已读状态复用聊天领域能力。
 */
@Service
@RequiredArgsConstructor
public class TournamentCommentAppService {

    private final PublishCommentActivity publishCommentActivity;
    private final ListCommentsActivity listCommentsActivity;

    @Transactional
    public TournamentCommentDTO publish(TournamentCommentPublishCmd cmd) {
        return publishCommentActivity.execute(cmd, UserContext.get());
    }

    @Transactional
    public TournamentCommentListDTO list(String tournamentId, String beforeCommentId, Integer limit) {
        return listCommentsActivity.execute(
                tournamentId, beforeCommentId, limit, UserContext.get());
    }
}
