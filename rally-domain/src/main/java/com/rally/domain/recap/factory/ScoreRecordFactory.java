package com.rally.domain.recap.factory;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.recap.model.ScoreAddCmd;
import com.rally.domain.recap.model.ScoreCmd;
import com.rally.domain.recap.model.ScoreRecordData;
import com.rally.domain.recap.model.ScoreUpdateCmd;
import com.rally.domain.recap.service.TennisScorePolicy;
import com.rally.domain.user.model.UserData;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.user.service.UserProfileDomainService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class ScoreRecordFactory {

    private final UserProfileDomainService userProfileDomainService;

    public ScoreRecordData create(ScoreAddCmd cmd, Meetup meetup, String userId) {
        ScoreRecordData data = buildCommon(cmd, meetup, userId);
        data.setBizId(IdWorker.getIdStr());
        return data;
    }

    public ScoreRecordData create(ScoreUpdateCmd cmd, Meetup meetup, String userId) {
        ScoreRecordData data = buildCommon(cmd, meetup, userId);
        data.setBizId(cmd.getBizId());
        return data;
    }

    private ScoreRecordData buildCommon(ScoreCmd cmd, Meetup meetup, String userId) {
        ScoreRecordData data = new ScoreRecordData();
        data.setRallyMeetupId(meetup.getMeetupId());
        data.setMeetupDate(meetup.getData().getStartTime());
        data.setSetNumber(cmd.getSetNum());
        data.setSetFormat(cmd.getSetFormatType());
        data.setMatchType(cmd.getMatchType());
        data.setSideAPlayer1(cmd.getSideAPlayer1());
        data.setSideAPlayer2(cmd.getSideAPlayer2());
        data.setSideBPlayer1(cmd.getSideBPlayer1());
        data.setSideBPlayer2(cmd.getSideBPlayer2());
        data.setSideAScore(cmd.getSideAScore());
        data.setSideBScore(cmd.getSideBScore());
        data.setSideATiebreakScore(cmd.getSideATiebreakScore());
        data.setSideBTiebreakScore(cmd.getSideBTiebreakScore());
        data.setRecordedBy(userId);
        data.setWinSide(TennisScorePolicy.calcWinSide(cmd.getSideAScore(), cmd.getSideBScore()));
        fillUserNicknames(data);
        return data;
    }

    private void fillUserNicknames(ScoreRecordData data) {
        List<String> playerIds = Stream.of(data.getSideAPlayer1(), data.getSideAPlayer2(), data.getSideBPlayer1(), data.getSideBPlayer2()).filter(StringUtils::isNotBlank).distinct().toList();
        Map<String, UserProfile> profileMap = userProfileDomainService.listMap(playerIds);
        if (profileMap == null || profileMap.isEmpty()) return;
        UserProfile a1 = profileMap.get(data.getSideAPlayer1());
        if (a1 != null && a1.getUser() != null) { UserData u = a1.getUser(); data.setSideAPlayer1Nickname(u.getNickname()); data.setSideAPlayer1Avatar(u.getAvatarUrl()); data.setSideAPlayer1Gender(u.getGender()); }
        UserProfile a2 = profileMap.get(data.getSideAPlayer2());
        if (a2 != null && a2.getUser() != null) { UserData u = a2.getUser(); data.setSideAPlayer2Nickname(u.getNickname()); data.setSideAPlayer2Avatar(u.getAvatarUrl()); data.setSideAPlayer2Gender(u.getGender()); }
        UserProfile b1 = profileMap.get(data.getSideBPlayer1());
        if (b1 != null && b1.getUser() != null) { UserData u = b1.getUser(); data.setSideBPlayer1Nickname(u.getNickname()); data.setSideBPlayer1Avatar(u.getAvatarUrl()); data.setSideBPlayer1Gender(u.getGender()); }
        UserProfile b2 = profileMap.get(data.getSideBPlayer2());
        if (b2 != null && b2.getUser() != null) { UserData u = b2.getUser(); data.setSideBPlayer2Nickname(u.getNickname()); data.setSideBPlayer2Avatar(u.getAvatarUrl()); data.setSideBPlayer2Gender(u.getGender()); }
    }
}
