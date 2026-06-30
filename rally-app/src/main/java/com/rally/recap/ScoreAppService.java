package com.rally.recap;

import com.rally.config.property.QiniuConfiguration;
import com.rally.domain.meetup.enums.MatchTypeEnum;
import com.rally.domain.meetup.enums.ResultTypeEnum;
import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.meetup.model.PageDTO;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.recap.model.ScoreAddCmd;
import com.rally.domain.recap.model.ScoreDeleteCmd;
import com.rally.domain.recap.model.ScoreItemDTO;
import com.rally.domain.recap.model.ScoreListQueryCmd;
import com.rally.domain.recap.model.ScoreRecordData;
import com.rally.domain.recap.model.ScoreStatsDTO;
import com.rally.domain.recap.model.ScoreUpdateCmd;
import com.rally.domain.recap.service.ScoreDomainService;
import com.rally.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScoreAppService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final ScoreDomainService scoreDomainService;
    private final MeetupDomainService meetupDomainService;

    public void addScore(ScoreAddCmd cmd) {
        String userId = UserContext.get();
        Meetup meetup = meetupDomainService.get(cmd.getMeetupId());
        meetup.assertReviewAvailable(userId);
        scoreDomainService.addScoreItem(meetup, userId, cmd);
    }

    public void updateScore(ScoreUpdateCmd cmd) {
        String userId = UserContext.get();
        Meetup meetup = meetupDomainService.get(cmd.getMeetupId());
        meetup.assertReviewAvailable(userId);
        scoreDomainService.updateScoreItem(meetup, userId, cmd);
    }

    public void deleteScore(ScoreDeleteCmd cmd) {
        String userId = UserContext.get();
        Meetup meetup = meetupDomainService.get(cmd.getMeetupId());
        meetup.assertReviewAvailable(userId);
        scoreDomainService.deleteScoreItem(meetup, cmd.getBizId());
    }

    public ScoreStatsDTO queryMyScoreStats(MatchTypeEnum matchType) {
        String userId = UserContext.get();
        List<ScoreRecordData> all = scoreDomainService.listScoresByUserId(userId);

        List<ScoreRecordData> filtered = matchType == null ? all : all.stream().filter(r -> r.getMatchType() == matchType).toList();
        long wins = filtered.stream().filter(r -> isWin(r, userId)).count();
        long losses = filtered.size() - wins;

        return new ScoreStatsDTO()
                .setTotal((long) filtered.size())
                .setWins(wins)
                .setLosses(losses)
                .setWinRate(formatRate(wins, filtered.size()))
                .setStreakType(computeStreakType(filtered, userId))
                .setStreakCount(computeStreakCount(filtered, userId));
    }

    public PageDTO<ScoreItemDTO> queryMyScores(ScoreListQueryCmd cmd) {
        String userId = UserContext.get();
        List<ScoreRecordData> all = scoreDomainService.listScoresByUserId(userId);

        List<ScoreRecordData> filtered = all.stream()
                .filter(r -> matchTypeMatches(r, cmd.getMatchType()))
                .filter(r -> meetupMatches(r, cmd.getMeetupId()))
                .toList();

        int size = cmd.getPageSize() != null ? cmd.getPageSize() : DEFAULT_PAGE_SIZE;
        List<Object> cursor = PageDTO.parseCursor(cmd.getLastId());
        String lastId = cursor.isEmpty() ? null : (String) cursor.get(0);
        List<ScoreRecordData> window = PageDTO.sliceAfter(filtered, lastId, size + 1, ScoreRecordData::getBizId);
        boolean hasMore = window.size() > size;
        List<ScoreRecordData> pageData = hasMore ? window.subList(0, size) : window;

        List<ScoreItemDTO> items = pageData.stream().map(r -> toItem(userId, r)).toList();
        PageDTO<ScoreItemDTO> page = new PageDTO<>(items, null, hasMore);
        page.buildCursor(ScoreItemDTO::getBizId);
        return page;
    }

    private boolean matchTypeMatches(ScoreRecordData r, ScoreListQueryCmd.MatchType matchType) {
        if (matchType == null) return true;
        if (matchType == ScoreListQueryCmd.MatchType.SINGLE) return r.getMatchType() == MatchTypeEnum.SINGLE;
        return r.getMatchType() == MatchTypeEnum.DOUBLE;
    }

    private boolean meetupMatches(ScoreRecordData r, String meetupId) {
        if (meetupId == null) return true;
        return meetupId.equals(r.getRallyMeetupId());
    }

    private boolean isWin(ScoreRecordData r, String userId) {
        boolean userInSideA = userId.equals(r.getSideAPlayer1()) || userId.equals(r.getSideAPlayer2());
        return (userInSideA && "A".equals(r.getWinSide())) || (!userInSideA && "B".equals(r.getWinSide()));
    }

    private ScoreItemDTO toItem(String userId, ScoreRecordData r) {
        boolean userInSideA = userId.equals(r.getSideAPlayer1()) || userId.equals(r.getSideAPlayer2());
        ResultTypeEnum resultType = isWin(r, userId) ? ResultTypeEnum.WIN : ResultTypeEnum.LOSE;
        String myScore = userInSideA ? String.valueOf(r.getSideAScore()) : String.valueOf(r.getSideBScore());
        String opponentScore = userInSideA ? String.valueOf(r.getSideBScore()) : String.valueOf(r.getSideAScore());
        // 当前用户性别
        com.rally.domain.user.enums.GenderEnum myGender = userInSideA ? (userId.equals(r.getSideAPlayer1()) ? r.getSideAPlayer1Gender() : r.getSideAPlayer2Gender()) : (userId.equals(r.getSideBPlayer1()) ? r.getSideBPlayer1Gender() : r.getSideBPlayer2Gender());
        // 队友（同侧的另一个人，双打才有）
        String mateId = userInSideA ? (userId.equals(r.getSideAPlayer1()) ? r.getSideAPlayer2() : r.getSideAPlayer1()) : (userId.equals(r.getSideBPlayer1()) ? r.getSideBPlayer2() : r.getSideBPlayer1());
        String mateNickname = userInSideA ? (userId.equals(r.getSideAPlayer1()) ? r.getSideAPlayer2Nickname() : r.getSideAPlayer1Nickname()) : (userId.equals(r.getSideBPlayer1()) ? r.getSideBPlayer2Nickname() : r.getSideBPlayer1Nickname());
        String mateAvatar = userInSideA ? (userId.equals(r.getSideAPlayer1()) ? r.getSideAPlayer2Avatar() : r.getSideAPlayer1Avatar()) : (userId.equals(r.getSideBPlayer1()) ? r.getSideBPlayer2Avatar() : r.getSideBPlayer1Avatar());
        com.rally.domain.user.enums.GenderEnum mateGender = userInSideA ? (userId.equals(r.getSideAPlayer1()) ? r.getSideAPlayer2Gender() : r.getSideAPlayer1Gender()) : (userId.equals(r.getSideBPlayer1()) ? r.getSideBPlayer2Gender() : r.getSideBPlayer1Gender());
        // 对手
        String opp1Id = userInSideA ? r.getSideBPlayer1() : r.getSideAPlayer1();
        String opp1Nickname = userInSideA ? r.getSideBPlayer1Nickname() : r.getSideAPlayer1Nickname();
        String opp1Avatar = userInSideA ? r.getSideBPlayer1Avatar() : r.getSideAPlayer1Avatar();
        com.rally.domain.user.enums.GenderEnum opp1Gender = userInSideA ? r.getSideBPlayer1Gender() : r.getSideAPlayer1Gender();
        String opp2Id = userInSideA ? r.getSideBPlayer2() : r.getSideAPlayer2();
        String opp2Nickname = userInSideA ? r.getSideBPlayer2Nickname() : r.getSideAPlayer2Nickname();
        String opp2Avatar = userInSideA ? r.getSideBPlayer2Avatar() : r.getSideAPlayer2Avatar();
        com.rally.domain.user.enums.GenderEnum opp2Gender = userInSideA ? r.getSideBPlayer2Gender() : r.getSideAPlayer2Gender();

        return new ScoreItemDTO()
                .setBizId(r.getBizId())
                .setMeetupId(r.getRallyMeetupId())
                .setResultType(resultType)
                .setResultTypeShow(resultType.getShow())
                .setMatchType(r.getMatchType())
                .setMatchTypeShow(r.getMatchType().getName())
                .setSetFormat(r.getSetFormat())
                .setSetFormatShow(r.getSetFormat().getShow())
                .setDate(r.getMeetupDate().format(DATE_FORMATTER))
                .setMyScore(myScore)
                .setOpponentScore(opponentScore)
                .setMyGender(myGender)
                .setTeammateId(mateId)
                .setTeammateNickname(mateNickname)
                .setTeammateAvatarUrl(QiniuConfiguration.buildSignedUrl(mateAvatar))
                .setTeammateGender(mateGender)
                .setOpponent1Id(opp1Id)
                .setOpponent1Nickname(opp1Nickname)
                .setOpponent1AvatarUrl(QiniuConfiguration.buildSignedUrl(opp1Avatar))
                .setOpponent1Gender(opp1Gender)
                .setOpponent2Id(opp2Id)
                .setOpponent2Nickname(opp2Nickname)
                .setOpponent2AvatarUrl(QiniuConfiguration.buildSignedUrl(opp2Avatar))
                .setOpponent2Gender(opp2Gender);
    }

    private String computeStreakType(List<ScoreRecordData> all, String userId) {
        if (all.isEmpty()) return null;
        return isWin(all.get(0), userId) ? "WIN" : "LOSE";
    }

    private Long computeStreakCount(List<ScoreRecordData> all, String userId) {
        if (all.isEmpty()) return null;
        boolean firstWin = isWin(all.get(0), userId);
        long count = 0;
        for (ScoreRecordData r : all) {
            if (isWin(r, userId) == firstWin) count++;
            else break;
        }
        return count;
    }

    private String formatRate(long wins, long total) {
        if (total == 0) return "--";
        return String.format("%.1f", wins * 100.0 / total);
    }
}
