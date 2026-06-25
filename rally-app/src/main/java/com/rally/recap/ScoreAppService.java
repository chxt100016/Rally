package com.rally.recap;

import com.rally.config.property.QiniuConfiguration;
import com.rally.domain.meetup.enums.MatchTypeEnum;
import com.rally.domain.meetup.enums.ResultTypeEnum;
import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.recap.model.ScoreAddCmd;
import com.rally.domain.recap.model.ScoreDeleteCmd;
import com.rally.domain.recap.model.ScoreListDTO;
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
import java.util.stream.Collectors;

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
        scoreDomainService.addScoreItem(meetup, userId, cmd, meetup.getData().getStartTime(), meetup.getData().getCourtName());
    }

    public void updateScore(ScoreUpdateCmd cmd) {
        String userId = UserContext.get();
        Meetup meetup = meetupDomainService.get(cmd.getMeetupId());
        meetup.assertReviewAvailable(userId);
        scoreDomainService.updateScoreItem(meetup, userId, cmd, meetup.getData().getStartTime(), meetup.getData().getCourtName());
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

    public ScoreListDTO queryMyScores(ScoreListQueryCmd cmd) {
        String userId = UserContext.get();
        List<ScoreRecordData> all = scoreDomainService.listScoresByUserId(userId);

        List<ScoreRecordData> filtered = all.stream()
                .filter(r -> matchTypeMatches(r, cmd.getMatchType()))
                .filter(r -> meetupMatches(r, cmd.getMeetupId()))
                .toList();

        int size = cmd.getPageSize() != null ? cmd.getPageSize() : DEFAULT_PAGE_SIZE;
        int startIdx = 0;
        if (cmd.getLastId() != null) {
            for (int i = 0; i < filtered.size(); i++) {
                if (filtered.get(i).getBizId().equals(cmd.getLastId())) {
                    startIdx = i + 1;
                    break;
                }
            }
        }
        int endIdx = Math.min(startIdx + size, filtered.size());
        List<ScoreRecordData> page = filtered.subList(startIdx, endIdx);
        boolean hasMore = endIdx < filtered.size();

        return new ScoreListDTO()
                .setList(page.stream().map(r -> toItem(userId, r)).collect(Collectors.toList()))
                .setHasMore(hasMore)
                .setNextCursor(hasMore ? page.get(page.size() - 1).getBizId() : null);
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

    private ScoreListDTO.Item toItem(String userId, ScoreRecordData r) {
        boolean userInSideA = userId.equals(r.getSideAPlayer1()) || userId.equals(r.getSideAPlayer2());
        ResultTypeEnum resultType = isWin(r, userId) ? ResultTypeEnum.WIN : ResultTypeEnum.LOSE;
        String myScore = userInSideA ? String.valueOf(r.getSideAScore()) : String.valueOf(r.getSideBScore());
        String opponentScore = userInSideA ? String.valueOf(r.getSideBScore()) : String.valueOf(r.getSideAScore());
        // 队友（同侧的另一个人，双打才有）
        String mateId = userInSideA ? (userId.equals(r.getSideAPlayer1()) ? r.getSideAPlayer2() : r.getSideAPlayer1()) : (userId.equals(r.getSideBPlayer1()) ? r.getSideBPlayer2() : r.getSideBPlayer1());
        String mateNickname = userInSideA ? (userId.equals(r.getSideAPlayer1()) ? r.getSideAPlayer2Nickname() : r.getSideAPlayer1Nickname()) : (userId.equals(r.getSideBPlayer1()) ? r.getSideBPlayer2Nickname() : r.getSideBPlayer1Nickname());
        String mateAvatar = userInSideA ? (userId.equals(r.getSideAPlayer1()) ? r.getSideAPlayer2Avatar() : r.getSideAPlayer1Avatar()) : (userId.equals(r.getSideBPlayer1()) ? r.getSideBPlayer2Avatar() : r.getSideBPlayer1Avatar());
        // 对手
        String opp1Id = userInSideA ? r.getSideBPlayer1() : r.getSideAPlayer1();
        String opp1Nickname = userInSideA ? r.getSideBPlayer1Nickname() : r.getSideAPlayer1Nickname();
        String opp1Avatar = userInSideA ? r.getSideBPlayer1Avatar() : r.getSideAPlayer1Avatar();
        String opp2Id = userInSideA ? r.getSideBPlayer2() : r.getSideAPlayer2();
        String opp2Nickname = userInSideA ? r.getSideBPlayer2Nickname() : r.getSideAPlayer2Nickname();
        String opp2Avatar = userInSideA ? r.getSideBPlayer2Avatar() : r.getSideAPlayer2Avatar();

        return new ScoreListDTO.Item()
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
                .setTeammateId(mateId)
                .setTeammateNickname(mateNickname)
                .setTeammateAvatarUrl(QiniuConfiguration.buildSignedUrl(mateAvatar))
                .setOpponent1Id(opp1Id)
                .setOpponent1Nickname(opp1Nickname)
                .setOpponent1AvatarUrl(QiniuConfiguration.buildSignedUrl(opp1Avatar))
                .setOpponent2Id(opp2Id)
                .setOpponent2Nickname(opp2Nickname)
                .setOpponent2AvatarUrl(QiniuConfiguration.buildSignedUrl(opp2Avatar));
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
