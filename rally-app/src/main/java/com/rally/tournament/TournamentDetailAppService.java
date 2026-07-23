package com.rally.tournament;

import com.rally.config.property.QiniuConfiguration;
import com.rally.domain.tournament.model.MatchOpponentDTO;
import com.rally.domain.tournament.model.MyCurrentMatchDTO;
import com.rally.domain.tournament.model.TournamentBracketMatchDTO;
import com.rally.domain.tournament.model.TournamentRejectRecordDTO;
import com.rally.domain.tournament.model.TournamentDetailDTO;
import com.rally.domain.tournament.service.TournamentDetailService;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.user.service.UserProfileDomainService;
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

    private final UserProfileDomainService userProfileDomainService;

    /**
     * 赛事落地页详情，userId 从 UserContext 取，可匿名（未登录只返回公开区块）
     */
    public TournamentDetailDTO detail(String tournamentId) {
        String userId = UserContext.getIfPresent();
        TournamentDetailDTO detail = tournamentDetailService.assembleDetail(tournamentId, userId);
        if (detail.getTournament() != null) {
            detail.getTournament().setPosterUrl(QiniuConfiguration.buildSignedUrl(detail.getTournament().getPosterUrl()));
        }

        List<String> userIds = collectUserIds(detail);
        if (!userIds.isEmpty()) {
            Map<String, UserProfile> profiles = userProfileDomainService.listMap(userIds);
            fillNicknames(detail, profiles);
        }
        return detail;
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
                }
            }
        }
    }

    private void fillOpponentInfo(MatchOpponentDTO opponent, Map<String, UserProfile> profiles) {
        UserProfile profile = profiles.get(opponent.getUserId());
        if (profile == null || profile.getUser() == null) {
            return;
        }
        opponent.setNickname(profile.getUser().getNickname());
        opponent.setAvatarUrl(QiniuConfiguration.buildSignedUrl(profile.getUser().getAvatarUrl()));
        if (profile.getProfile() != null) {
            opponent.setNtrpScore(profile.getProfile().getNtrpScore());
        }
    }
}
