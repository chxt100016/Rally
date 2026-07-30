package com.rally.tournament;

import com.rally.config.property.QiniuConfiguration;
import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.tournament.enums.TournamentActionStateEnum;
import com.rally.domain.tournament.enums.TournamentJoinRestrictionEnum;
import com.rally.domain.tournament.model.MatchOpponentDTO;
import com.rally.domain.tournament.model.MyCurrentMatchDTO;
import com.rally.domain.tournament.model.TournamentBracketMatchDTO;
import com.rally.domain.tournament.model.TournamentDetailDTO;
import com.rally.domain.tournament.model.TournamentEntrantDTO;
import com.rally.domain.tournament.model.TournamentRejectRecordDTO;
import com.rally.domain.tournament.service.TournamentDetailService;
import com.rally.domain.tournament.service.TournamentPolicy;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.user.service.UserProfileDomainService;
import com.rally.meetup.MeetupCardPackingService;
import com.rally.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 落地页详情编排：装配领域数据后批量查询用户昵称/头像/NTRP 回填
 */
@Service
@RequiredArgsConstructor
public class TournamentDetailAppService {

    private final TournamentDetailService tournamentDetailService;

    private final TournamentPolicy tournamentPolicy;

    private final UserProfileDomainService userProfileDomainService;

    private final MeetupDomainService meetupDomainService;

    private final MeetupCardPackingService meetupCardPackingService;

    /**
     * 赛事落地页详情，userId 从 UserContext 取，可匿名（未登录只返回公开区块）
     */
    public TournamentDetailDTO detail(String tournamentId) {
        String userId = UserContext.getIfPresent();
        TournamentDetailDTO detail = tournamentDetailService.assembleDetail(tournamentId, userId);
        if (detail.getTournament() != null) {
            detail.getTournament().setPosterUrl(QiniuConfiguration.buildSignedUrl(detail.getTournament().getPosterUrl()));
            detail.getTournament().setWechatGroupQrCodeUrl(QiniuConfiguration.buildSignedUrl(detail.getTournament().getWechatGroupQrCodeUrl()));
        }

        fillJoinRestrictions(detail, userId);

        List<String> userIds = collectUserIds(detail);
        if (!userIds.isEmpty()) {
            Map<String, UserProfile> profiles = userProfileDomainService.listMap(userIds);
            fillNicknames(detail, profiles);
        }

        fillMeetupCard(detail);
        return detail;
    }

    /**
     * 未报名时返回登录、档案完整度和 NTRP 等级限制，供前端决定报名按钮及提示文案。
     */
    private void fillJoinRestrictions(TournamentDetailDTO detail, String userId) {
        if (detail.getActionState() != TournamentActionStateEnum.NOT_REGISTERED || detail.getTournament() == null) {
            return;
        }
        UserProfile userProfile = userId == null ? null : userProfileDomainService.get(userId);
        List<TournamentJoinRestrictionEnum> restrictions = tournamentPolicy.collectJoinRestrictions(
                detail.getTournament().getNtrpLevel(), userProfile);
        detail.setRestrictions(restrictions);
        detail.setJoinable(restrictions.isEmpty());
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

    private List<String> collectUserIds(TournamentDetailDTO detail) {
        List<String> userIds = new ArrayList<>();
        MyCurrentMatchDTO myCurrentMatch = detail.getMyCurrentMatch();
        if (myCurrentMatch != null && myCurrentMatch.getOpponents() != null) {
            myCurrentMatch.getOpponents().forEach(o -> userIds.add(o.getUserId()));
        }
        if (detail.getBracket() != null && detail.getBracket().getRounds() != null) {
            detail.getBracket().getRounds().forEach(round -> round.getMatches().forEach(match -> match.getParticipants().forEach(p -> userIds.add(p.getUserId()))));
        }
        if (detail.getRejectRecords() != null) {
            detail.getRejectRecords().forEach(r -> userIds.add(r.getUserId()));
        }
        if (detail.getEntrants() != null) {
            detail.getEntrants().forEach(e -> userIds.add(e.getUserId()));
        }
        return userIds;
    }

    private void fillNicknames(TournamentDetailDTO detail, Map<String, UserProfile> profiles) {
        MyCurrentMatchDTO myCurrentMatch = detail.getMyCurrentMatch();
        if (myCurrentMatch != null && myCurrentMatch.getOpponents() != null) {
            myCurrentMatch.getOpponents().forEach(o -> fillOpponentInfo(o, profiles));
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
            detail.getEntrants().forEach(e -> fillEntrantInfo(e, profiles));
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
}
