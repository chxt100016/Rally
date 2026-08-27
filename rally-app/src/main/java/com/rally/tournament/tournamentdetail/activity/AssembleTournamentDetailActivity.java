package com.rally.tournament.tournamentdetail.activity;

import com.rally.config.property.QiniuConfiguration;
import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.meetup.service.ChatDomainService;
import com.rally.domain.tournament.enums.TournamentEntryStatusEnum;
import com.rally.domain.tournament.enums.TournamentActionStateEnum;
import com.rally.domain.tournament.enums.TournamentJoinRestrictionEnum;
import com.rally.domain.tournament.enums.RebookReasonEnum;
import com.rally.domain.tournament.model.MatchOpponentDTO;
import com.rally.domain.tournament.model.MatchParticipantDTO;
import com.rally.domain.tournament.model.MyCurrentMatchDTO;
import com.rally.domain.tournament.model.TournamentActionDTO;
import com.rally.domain.tournament.model.TournamentBracketMatchDTO;
import com.rally.domain.tournament.model.TournamentDetailDTO;
import com.rally.domain.tournament.model.TournamentCommentStateDTO;
import com.rally.domain.tournament.model.TournamentEntrantDTO;
import com.rally.domain.tournament.model.TournamentRejectRecordDTO;
import com.rally.domain.tournament.service.TournamentDetailService;
import com.rally.domain.tournament.service.TournamentPolicy;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.user.service.UserProfileDomainService;
import com.rally.meetup.MeetupCardPackingService;
import com.rally.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 落地页详情编排：装配领域数据后批量查询用户昵称/头像/NTRP 回填
 */
@Component
@RequiredArgsConstructor
public class AssembleTournamentDetailActivity {

    private final TournamentDetailService tournamentDetailService;

    private final TournamentPolicy tournamentPolicy;

    private final UserProfileDomainService userProfileDomainService;

    private final MeetupDomainService meetupDomainService;

    private final MeetupCardPackingService meetupCardPackingService;

    private final ChatDomainService chatDomainService;

    /**
     * 赛事落地页详情，userId 从 UserContext 取，可匿名（未登录只返回公开区块）
     */
    public TournamentDetailDTO execute(String tournamentId, String userId) {
        TournamentDetailDTO detail = tournamentDetailService.assembleDetail(tournamentId, userId);
        fillCommentState(detail, tournamentId, userId);
        if (detail.getTournament() != null) {
            detail.getTournament().setPosterUrl(QiniuConfiguration.buildSignedUrl(detail.getTournament().getPosterUrl()));
            detail.getTournament().setRulePosterUrl(QiniuConfiguration.buildSignedUrl(detail.getTournament().getRulePosterUrl()));
            detail.getTournament().setWechatGroupQrCodeUrl(QiniuConfiguration.buildSignedUrl(detail.getTournament().getWechatGroupQrCodeUrl()));
        }

        Map<String, UserProfile> profiles = Map.of();
        List<String> userIds = collectUserIds(detail);
        if (!userIds.isEmpty()) {
            profiles = userProfileDomainService.listMap(userIds);
            fillNicknames(detail, profiles);
            fillOpponentPhones(detail, userId, profiles);
        }

        fillJoinRestrictions(detail, userId, profiles);
        resolveAwaitPlayingState(detail);
        fillAction(detail, profiles);
        fillMeetupCard(detail);
        fillOfflineMeetupCard(detail);
        return detail;
    }

    /** 详情只展示未读数，不推进评论的已读位置。 */
    private void fillCommentState(TournamentDetailDTO detail, String tournamentId, String userId) {
        if (userId == null || detail.getMyEntry() == null
                || detail.getMyEntry().getStatus() == TournamentEntryStatusEnum.WITHDRAWN) {
            return;
        }
        detail.setCommentState(new TournamentCommentStateDTO(
                chatDomainService.getUnreadCount(tournamentId, userId)));
    }

    /**
     * 未报名时返回登录、档案完整度和 NTRP 等级限制，供前端决定报名按钮及提示文案。
     */
    private void fillJoinRestrictions(TournamentDetailDTO detail, String userId, Map<String, UserProfile> profiles) {
        TournamentActionStateEnum actionState = actionState(detail);
        if ((actionState != TournamentActionStateEnum.NOT_REGISTERED && actionState != TournamentActionStateEnum.FROZEN) || detail.getTournament() == null) {
            return;
        }
        UserProfile userProfile = userId == null ? null : profiles.get(userId);
        if (userProfile == null && userId != null) {
            userProfile = userProfileDomainService.get(userId);
        }
        if (actionState == TournamentActionStateEnum.FROZEN) {
            detail.setRestrictions(tournamentPolicy.collectPhoneRestrictions(userProfile));
            return;
        }
        List<TournamentJoinRestrictionEnum> restrictions = tournamentPolicy.collectJoinRestrictions(
                detail.getTournament().getNtrpLevel(), detail.getTournament().getGenderLimit(), userProfile);
        detail.setRestrictions(restrictions);
    }

    /**
     * 装配当前比赛的约球卡片：订场后生成草稿约球，供对方确认赛约前查看
     */
    private void fillMeetupCard(TournamentDetailDTO detail) {
        MyCurrentMatchDTO myCurrentMatch = detail.getMyCurrentMatch();
        if (myCurrentMatch == null || myCurrentMatch.getMeetupId() == null) {
            return;
        }
        Meetup meetup = meetupDomainService.get(myCurrentMatch.getMeetupId());
        myCurrentMatch.setMeetupCard(meetupCardPackingService.packCard(meetup.getData(), null, null));
    }

    /** 已创建线下赛活动时才查询并填充对应约球卡片。 */
    private void fillOfflineMeetupCard(TournamentDetailDTO detail) {
        if (detail.getOffline() == null || detail.getOffline().getMeetupId() == null) {
            return;
        }
        Meetup meetup = meetupDomainService.get(detail.getOffline().getMeetupId());
        detail.getOffline().setMeetupCard(meetupCardPackingService.packCard(meetup.getData(), null, null));
    }

    private List<String> collectUserIds(TournamentDetailDTO detail) {
        List<String> userIds = new ArrayList<>();
        MyCurrentMatchDTO myCurrentMatch = detail.getMyCurrentMatch();
        if (myCurrentMatch != null && myCurrentMatch.getParticipants() != null) {
            myCurrentMatch.getParticipants().forEach(p -> userIds.add(p.getUserId()));
        }
        if (myCurrentMatch != null && myCurrentMatch.getLastRebookBy() != null) {
            userIds.add(myCurrentMatch.getLastRebookBy());
        }
        if (detail.getBracket() != null && detail.getBracket().getRounds() != null) {
            detail.getBracket().getRounds().forEach(round -> round.getMatches().forEach(match -> match.getParticipants().forEach(p -> userIds.add(p.getUserId()))));
        }
        if (detail.getRejectRecords() != null) {
            detail.getRejectRecords().forEach(r -> userIds.add(r.getUserId()));
        }
        if (detail.getEntrants() != null) {
            detail.getEntrants().forEach(entrant -> userIds.add(entrant.getUserId()));
        }
        return userIds;
    }

    private void fillNicknames(TournamentDetailDTO detail, Map<String, UserProfile> profiles) {
        MyCurrentMatchDTO myCurrentMatch = detail.getMyCurrentMatch();
        if (myCurrentMatch != null && myCurrentMatch.getParticipants() != null) {
            myCurrentMatch.getParticipants().forEach(p -> fillParticipantInfo(p, profiles));
        }
        if (detail.getBracket() != null && detail.getBracket().getRounds() != null) {
            for (var round : detail.getBracket().getRounds()) {
                for (TournamentBracketMatchDTO match : round.getMatches()) {
                    match.getParticipants().forEach(p -> fillOpponentInfo(p, profiles));
                }
            }
        }
        if (detail.getRejectRecords() != null) {
            for (TournamentRejectRecordDTO record : detail.getRejectRecords()) {
                UserProfile profile = profiles.get(record.getUserId());
                if (profile != null && profile.getUser() != null) {
                    record.setNickname(profile.getUser().getNickname());
                    record.setAvatarUrl(QiniuConfiguration.buildSignedUrl(profile.getUser().getAvatarUrl()));
                    record.setGender(profile.getUser().getGender());
                }
            }
        }
        if (detail.getEntrants() != null) {
            detail.getEntrants().forEach(entrant -> fillEntrantInfo(entrant, profiles));
        }
        if (detail.getEntrantOverview() != null && detail.getEntrantOverview().getRounds() != null) {
            detail.getEntrantOverview().getRounds().forEach(round ->
                    round.getEntrants().forEach(entrant -> fillEntrantInfo(entrant, profiles)));
        }
    }

    private void fillEntrantInfo(TournamentEntrantDTO entrant, Map<String, UserProfile> profiles) {
        UserProfile profile = profiles.get(entrant.getUserId());
        if (profile == null || profile.getUser() == null) {
            return;
        }
        entrant.setNickname(profile.getUser().getNickname());
        entrant.setAvatarUrl(QiniuConfiguration.buildSignedUrl(profile.getUser().getAvatarUrl()));
        entrant.setGender(profile.getUser().getGender());
    }

    private void fillOpponentInfo(MatchOpponentDTO opponent, Map<String, UserProfile> profiles) {
        UserProfile profile = profiles.get(opponent.getUserId());
        if (profile == null || profile.getUser() == null) {
            return;
        }
        opponent.setNickname(profile.getUser().getNickname());
        opponent.setAvatarUrl(QiniuConfiguration.buildSignedUrl(profile.getUser().getAvatarUrl()));
        opponent.setGender(profile.getUser().getGender());
        if (profile.getProfile() != null) {
            opponent.setNtrpScore(profile.getProfile().getNtrpScore());
        }
    }

    private void fillParticipantInfo(MatchParticipantDTO participant, Map<String, UserProfile> profiles) {
        UserProfile profile = profiles.get(participant.getUserId());
        if (profile == null || profile.getUser() == null) {
            return;
        }
        participant.setNickname(profile.getUser().getNickname());
        participant.setAvatarUrl(QiniuConfiguration.buildSignedUrl(profile.getUser().getAvatarUrl()));
        participant.setGender(profile.getUser().getGender());
    }

    /** 仅向当前用户返回不同 entryNo 对手的手机号；无法确认本人队伍时不返回任何手机号。 */
    void fillOpponentPhones(TournamentDetailDTO detail, String userId, Map<String, UserProfile> profiles) {
        MyCurrentMatchDTO match = detail.getMyCurrentMatch();
        if (userId == null || detail.getMyEntry() == null || detail.getMyEntry().getEntryNo() == null || match == null || match.getParticipants() == null) {
            return;
        }
        Integer currentEntryNo = detail.getMyEntry().getEntryNo();
        long currentParticipantCount = match.getParticipants().stream().filter(participant -> userId.equals(participant.getUserId())).filter(participant -> Objects.equals(currentEntryNo, participant.getEntryNo())).count();
        boolean entryNoConflict = match.getParticipants().stream().filter(participant -> userId.equals(participant.getUserId())).anyMatch(participant -> !Objects.equals(currentEntryNo, participant.getEntryNo()));
        if (currentParticipantCount != 1 || entryNoConflict) {
            return;
        }
        match.getParticipants().stream().filter(participant -> participant.getEntryNo() != null && !Objects.equals(currentEntryNo, participant.getEntryNo())).forEach(participant -> {
            UserProfile profile = profiles.get(participant.getUserId());
            if (profile != null && profile.getUser() != null && profile.getUser().hasPhone()) {
                participant.setPhone(profile.getUser().getPhone());
            }
        });
    }

    /**
     * 状态文案统一由枚举维护；涉及对手或打回重订的信息在昵称回填后替换占位内容。
     */
    private void fillAction(TournamentDetailDTO detail, Map<String, UserProfile> profiles) {
        TournamentActionStateEnum text = resolveActionStateText(detail);
        TournamentActionDTO action = detail.getAction();
        String title = text.getTitle();
        String subtitle = text.getSubtitle();
        MyCurrentMatchDTO match = detail.getMyCurrentMatch();

        if (text == TournamentActionStateEnum.AWAIT_BOOKING_REBOOK) {
            subtitle = String.format(subtitle, rebookerDisplayName(match, profiles), rebookReason(match));
        } else if (text == TournamentActionStateEnum.AWAIT_RESULT_CONFIRM
                || text == TournamentActionStateEnum.AWAIT_OPPONENT_RESULT_CONFIRM) {
            subtitle = String.format(subtitle, opponentNames(match), winnerNames(match));
        } else if (subtitle.contains("%s")) {
            subtitle = String.format(subtitle, opponentNames(match));
        }
        action.setStateShow(text.getLabel());
        action.setStateTitle(title);
        action.setStateSubtitle(subtitle);
    }

    /**
     * 比赛状态进入待录入结果后，仍需以关联赛约的开赛时间作为结果录入的前置条件。
     */
    private void resolveAwaitPlayingState(TournamentDetailDTO detail) {
        if (actionState(detail) != TournamentActionStateEnum.AWAIT_RESULT_SUBMIT) {
            return;
        }
        MyCurrentMatchDTO match = detail.getMyCurrentMatch();
        if (match == null || match.getMeetupId() == null) {
            return;
        }
        Meetup meetup = meetupDomainService.get(match.getMeetupId());
        LocalDateTime startTime = meetup.getData().getStartTime();
        if (startTime != null && !LocalDateTime.now().isAfter(startTime)) {
            detail.getAction().setState(TournamentActionStateEnum.AWAIT_PLAYING);
        }
    }

    private TournamentActionStateEnum resolveActionStateText(TournamentDetailDTO detail) {
        TournamentActionStateEnum actionState = actionState(detail);
        if (actionState == null || actionState == TournamentActionStateEnum.END) {
            return TournamentActionStateEnum.END;
        }
        return actionState;
    }

    private TournamentActionStateEnum actionState(TournamentDetailDTO detail) {
        return detail.getAction() == null ? null : detail.getAction().getState();
    }

    private String opponentNames(MyCurrentMatchDTO match) {
        if (match == null || match.getParticipants() == null) {
            return "对手";
        }
        String currentUserId = UserContext.getIfPresent();
        Integer currentEntryNo = match.getParticipants().stream()
                .filter(participant -> Objects.equals(participant.getUserId(), currentUserId))
                .map(MatchParticipantDTO::getEntryNo)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (currentEntryNo == null) {
            return "对手";
        }
        String names = match.getParticipants().stream()
                .filter(participant -> participant.getEntryNo() != null && !Objects.equals(participant.getEntryNo(), currentEntryNo))
                .map(MatchParticipantDTO::getNickname)
                .filter(Objects::nonNull)
                .filter(name -> !name.isBlank())
                .collect(Collectors.joining("、"));
        return names.isBlank() ? "对手" : names;
    }

    /** 同一 winnerEntryNo 的参与者均为胜者，支持双打同队成员共同展示。 */
    private String winnerNames(MyCurrentMatchDTO match) {
        if (match == null || match.getWinnerEntryNo() == null || match.getParticipants() == null) {
            return "获胜方";
        }
        String names = match.getParticipants().stream()
                .filter(participant -> match.getWinnerEntryNo().equals(participant.getEntryNo()))
                .map(MatchParticipantDTO::getNickname)
                .filter(Objects::nonNull)
                .filter(name -> !name.isBlank())
                .collect(Collectors.joining("、"));
        return names.isBlank() ? "获胜方" : names;
    }

    private String rebookerDisplayName(MyCurrentMatchDTO match, Map<String, UserProfile> profiles) {
        if (match == null || match.getLastRebookBy() == null) {
            return "对手";
        }
        String nickname = match.getParticipants() == null ? null : match.getParticipants().stream()
                .filter(participant -> match.getLastRebookBy().equals(participant.getUserId()))
                .map(MatchParticipantDTO::getNickname)
                .filter(Objects::nonNull)
                .filter(name -> !name.isBlank())
                .findFirst()
                .orElse(null);
        if (nickname != null) {
            return "对手" + nickname;
        }
        UserProfile profile = profiles.get(match.getLastRebookBy());
        if (profile == null || profile.getUser() == null || profile.getUser().getNickname() == null
                || profile.getUser().getNickname().isBlank()) {
            return "对手";
        }
        return "对手" + profile.getUser().getNickname();
    }

    private String rebookReason(MyCurrentMatchDTO match) {
        if (match == null || match.getLastRebookReasonCode() == null) {
            return "场地或时间不合适";
        }
        try {
            RebookReasonEnum reason = RebookReasonEnum.valueOf(match.getLastRebookReasonCode());
            return reason.getLabel();
        } catch (IllegalArgumentException ignored) {
            return match.getLastRebookReasonCode();
        }
    }
}
