package com.rally.tournament.commentpublish.activity;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.meetup.model.ChatMessageData;
import com.rally.domain.meetup.service.ChatDomainService;
import com.rally.domain.tournament.enums.TournamentEntryStatusEnum;
import com.rally.domain.tournament.model.TournamentCommentDTO;
import com.rally.domain.tournament.model.TournamentCommentPublishCmd;
import com.rally.domain.tournament.model.TournamentEntry;
import com.rally.domain.tournament.service.TournamentEntryService;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.user.service.UserProfileDomainService;
import com.rally.domain.utils.Assert;
import com.rally.tournament.convert.TournamentCommentAppConvertMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 业务活动 publish-comment：发布赛事评论并同步讨论成员阅读状态。
 */
@Component
@RequiredArgsConstructor
public class PublishCommentActivity {

    private final TournamentEntryService tournamentEntryService;
    private final UserProfileDomainService userProfileDomainService;
    private final ChatDomainService chatDomainService;

    @Transactional
    public TournamentCommentDTO execute(TournamentCommentPublishCmd command, String userId) {
        // A1：报名缺失沿用 TOURNAMENT_ENTRY_NOT_FOUND，已退赛禁止发评论。
        TournamentEntry entry = tournamentEntryService.getByTournamentAndUser(
                command.getTournamentId(), userId);
        Assert.isTrue(entry.getData().getStatus() != TournamentEntryStatusEnum.WITHDRAWN,
                BizErrorCode.TOURNAMENT_COMMENT_FORBIDDEN);

        // A2：读取当前用户资料，后续由聊天消息固化当前昵称和头像 key。
        UserProfile sender = userProfileDomainService.get(userId);

        // A3/A4：保存带发布者快照的消息，增加其他成员未读并推进发布者已读位置。
        ChatMessageData message = chatDomainService.send(
                command.getTournamentId(), command.getContent(), command.getContentType(), sender);

        // A5：保留原 DTO 映射与头像签名行为，签名异常使整个事务回滚。
        return TournamentCommentAppConvertMapper.INSTANCE.toCommentDTO(message);
    }
}
