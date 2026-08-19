package com.rally.tournament;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.meetup.model.ChatMessageData;
import com.rally.domain.meetup.service.ChatDomainService;
import com.rally.domain.tournament.enums.TournamentEntryStatusEnum;
import com.rally.domain.tournament.model.TournamentCommentDTO;
import com.rally.domain.tournament.model.TournamentCommentPublishCmd;
import com.rally.domain.tournament.model.TournamentCommentListDTO;
import com.rally.domain.tournament.model.TournamentEntry;
import com.rally.domain.tournament.service.TournamentEntryService;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.user.service.UserProfileDomainService;
import com.rally.domain.utils.Assert;
import com.rally.tournament.convert.TournamentCommentAppConvertMapper;
import com.rally.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 赛事评论应用服务：负责参赛权限，消息与已读状态复用聊天领域能力。
 */
@Service
@RequiredArgsConstructor
public class TournamentCommentAppService {

    private final ChatDomainService chatDomainService;
    private final TournamentEntryService tournamentEntryService;
    private final UserProfileDomainService userProfileDomainService;

    @Transactional
    public TournamentCommentDTO publish(TournamentCommentPublishCmd cmd) {
        String userId = UserContext.get();
        assertCanAccess(cmd.getTournamentId(), userId);

        UserProfile sender = userProfileDomainService.get(userId);
        ChatMessageData message = chatDomainService.send(
                cmd.getTournamentId(), cmd.getContent(), cmd.getContentType(), sender);
        return TournamentCommentAppConvertMapper.INSTANCE.toCommentDTO(message);
    }

    @Transactional
    public TournamentCommentListDTO list(String tournamentId, String beforeCommentId, Integer limit) {
        String userId = UserContext.get();
        assertCanAccess(tournamentId, userId);

        int pageSize = limit == null ? 20 : Math.max(1, Math.min(limit, 100));
        List<ChatMessageData> messages = chatDomainService.listLatest(
                tournamentId, userId, beforeCommentId, pageSize);
        return new TournamentCommentListDTO(
                TournamentCommentAppConvertMapper.INSTANCE.toCommentDTO(messages));
    }

    private void assertCanAccess(String tournamentId, String userId) {
        TournamentEntry entry = tournamentEntryService.getByTournamentAndUser(tournamentId, userId);
        Assert.isTrue(entry.getData().getStatus() != TournamentEntryStatusEnum.WITHDRAWN,
                BizErrorCode.TOURNAMENT_COMMENT_FORBIDDEN);
    }
}
