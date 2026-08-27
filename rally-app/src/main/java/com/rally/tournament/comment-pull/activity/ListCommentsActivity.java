package com.rally.tournament.commentpull.activity;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.meetup.model.ChatMessageData;
import com.rally.domain.meetup.service.ChatDomainService;
import com.rally.domain.tournament.enums.TournamentEntryStatusEnum;
import com.rally.domain.tournament.model.TournamentCommentListDTO;
import com.rally.domain.tournament.model.TournamentEntry;
import com.rally.domain.tournament.service.TournamentEntryService;
import com.rally.domain.utils.Assert;
import com.rally.tournament.convert.TournamentCommentAppConvertMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 业务活动 list-comments：向未退赛参赛者倒序交付赛事评论。
 */
@Component
@RequiredArgsConstructor
public class ListCommentsActivity {

    private final TournamentEntryService tournamentEntryService;
    private final ChatDomainService chatDomainService;

    @Transactional(rollbackFor = Exception.class)
    public TournamentCommentListDTO execute(
            String tournamentId,
            String beforeCommentId,
            Integer limit,
            String userId) {
        // A1：报名缺失沿用 TOURNAMENT_ENTRY_NOT_FOUND，已退赛不交付评论。
        TournamentEntry entry = tournamentEntryService.getByTournamentAndUser(tournamentId, userId);
        Assert.isTrue(entry.getData().getStatus() != TournamentEntryStatusEnum.WITHDRAWN,
                BizErrorCode.TOURNAMENT_COMMENT_FORBIDDEN);

        // A2/A4：保留主线的数量钳制、上界游标倒序查询和单调已读推进。
        int pageSize = limit == null ? 20 : Math.max(1, Math.min(limit, 100));
        List<ChatMessageData> messages = chatDomainService.listLatest(
                tournamentId, userId, beforeCommentId, pageSize);

        // A3：直接使用发布时昵称/头像快照，映射时对头像资源键签名。
        return new TournamentCommentListDTO(
                TournamentCommentAppConvertMapper.INSTANCE.toCommentDTO(messages));
    }
}
