package com.rally.personalprofile.myscores.activity;

import com.rally.config.property.QiniuConfiguration;
import com.rally.domain.meetup.enums.MatchTypeEnum;
import com.rally.domain.meetup.enums.ResultTypeEnum;
import com.rally.domain.meetup.model.PageDTO;
import com.rally.domain.recap.model.ScoreItemDTO;
import com.rally.domain.recap.model.ScoreListQueryCmd;
import com.rally.domain.recap.model.ScoreRecordData;
import com.rally.domain.recap.service.ScoreDomainService;
import com.rally.domain.user.enums.GenderEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 业务活动 query-my-score-page：查询并转换本人参与的盘级比分页。
 */
@Component
@RequiredArgsConstructor
public class QueryMyScorePageActivity {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final ScoreDomainService scoreDomainService;

    public PageDTO<ScoreItemDTO> execute(String userId, ScoreListQueryCmd command) {
        // A1 仓储按四个球员位置匹配本人，并按 biz_id 倒序返回全量比分。
        List<ScoreRecordData> filtered = scoreDomainService.listScoresByUserId(userId).stream()
                .filter(record -> matchTypeMatches(record, command.getMatchType()))
                .filter(record -> meetupMatches(record, command.getMeetupId()))
                .toList();

        // A2 保留原始的游标解析、默认页大小、size + 1 探测及非法范围异常语义。
        int size = command.getPageSize() != null ? command.getPageSize() : DEFAULT_PAGE_SIZE;
        List<Object> cursor = PageDTO.parseCursor(command.getLastId());
        String lastId = cursor.isEmpty() ? null : (String) cursor.get(0);
        List<ScoreRecordData> window = PageDTO.sliceAfter(
                filtered,
                lastId,
                size + 1,
                ScoreRecordData::getBizId);
        boolean hasMore = window.size() > size;
        List<ScoreRecordData> pageData = hasMore ? window.subList(0, size) : window;

        List<ScoreItemDTO> items = pageData.stream()
                .map(record -> toItem(userId, record))
                .toList();
        PageDTO<ScoreItemDTO> page = new PageDTO<>(items, null, hasMore);

        // A5 只有存在下一页且当前页非空时，才用末条 bizId 生成下一页游标。
        page.buildCursor(ScoreItemDTO::getBizId);
        return page;
    }

    private boolean matchTypeMatches(ScoreRecordData record, ScoreListQueryCmd.MatchType matchType) {
        if (matchType == null) {
            return true;
        }
        if (matchType == ScoreListQueryCmd.MatchType.SINGLE) {
            return record.getMatchType() == MatchTypeEnum.SINGLE;
        }
        return record.getMatchType() == MatchTypeEnum.DOUBLE;
    }

    private boolean meetupMatches(ScoreRecordData record, String meetupId) {
        if (meetupId == null) {
            return true;
        }
        return meetupId.equals(record.getRallyMeetupId());
    }

    /** A3 同时出现在两侧时 A 侧优先，胜负只依持久化胜方判定。 */
    private boolean isWin(ScoreRecordData record, String userId) {
        boolean userInSideA = userId.equals(record.getSideAPlayer1())
                || userId.equals(record.getSideAPlayer2());
        return (userInSideA && "A".equals(record.getWinSide()))
                || (!userInSideA && "B".equals(record.getWinSide()));
    }

    /** A4 将持久化的比分、抢七分、性别与球员快照转换为本人视角。 */
    private ScoreItemDTO toItem(String userId, ScoreRecordData record) {
        boolean userInSideA = userId.equals(record.getSideAPlayer1())
                || userId.equals(record.getSideAPlayer2());
        ResultTypeEnum resultType = isWin(record, userId) ? ResultTypeEnum.WIN : ResultTypeEnum.LOSE;
        String myScore = userInSideA
                ? String.valueOf(record.getSideAScore())
                : String.valueOf(record.getSideBScore());
        String opponentScore = userInSideA
                ? String.valueOf(record.getSideBScore())
                : String.valueOf(record.getSideAScore());
        String myTiebreakScore = scoreToString(userInSideA
                ? record.getSideATiebreakScore()
                : record.getSideBTiebreakScore());
        String opponentTiebreakScore = scoreToString(userInSideA
                ? record.getSideBTiebreakScore()
                : record.getSideATiebreakScore());

        GenderEnum myGender = userInSideA
                ? (userId.equals(record.getSideAPlayer1())
                        ? record.getSideAPlayer1Gender()
                        : record.getSideAPlayer2Gender())
                : (userId.equals(record.getSideBPlayer1())
                        ? record.getSideBPlayer1Gender()
                        : record.getSideBPlayer2Gender());

        String teammateId = userInSideA
                ? (userId.equals(record.getSideAPlayer1())
                        ? record.getSideAPlayer2()
                        : record.getSideAPlayer1())
                : (userId.equals(record.getSideBPlayer1())
                        ? record.getSideBPlayer2()
                        : record.getSideBPlayer1());
        String teammateNickname = userInSideA
                ? (userId.equals(record.getSideAPlayer1())
                        ? record.getSideAPlayer2Nickname()
                        : record.getSideAPlayer1Nickname())
                : (userId.equals(record.getSideBPlayer1())
                        ? record.getSideBPlayer2Nickname()
                        : record.getSideBPlayer1Nickname());
        String teammateAvatar = userInSideA
                ? (userId.equals(record.getSideAPlayer1())
                        ? record.getSideAPlayer2Avatar()
                        : record.getSideAPlayer1Avatar())
                : (userId.equals(record.getSideBPlayer1())
                        ? record.getSideBPlayer2Avatar()
                        : record.getSideBPlayer1Avatar());
        GenderEnum teammateGender = userInSideA
                ? (userId.equals(record.getSideAPlayer1())
                        ? record.getSideAPlayer2Gender()
                        : record.getSideAPlayer1Gender())
                : (userId.equals(record.getSideBPlayer1())
                        ? record.getSideBPlayer2Gender()
                        : record.getSideBPlayer1Gender());

        String opponent1Id = userInSideA ? record.getSideBPlayer1() : record.getSideAPlayer1();
        String opponent1Nickname = userInSideA
                ? record.getSideBPlayer1Nickname()
                : record.getSideAPlayer1Nickname();
        String opponent1Avatar = userInSideA
                ? record.getSideBPlayer1Avatar()
                : record.getSideAPlayer1Avatar();
        GenderEnum opponent1Gender = userInSideA
                ? record.getSideBPlayer1Gender()
                : record.getSideAPlayer1Gender();
        String opponent2Id = userInSideA ? record.getSideBPlayer2() : record.getSideAPlayer2();
        String opponent2Nickname = userInSideA
                ? record.getSideBPlayer2Nickname()
                : record.getSideAPlayer2Nickname();
        String opponent2Avatar = userInSideA
                ? record.getSideBPlayer2Avatar()
                : record.getSideAPlayer2Avatar();
        GenderEnum opponent2Gender = userInSideA
                ? record.getSideBPlayer2Gender()
                : record.getSideAPlayer2Gender();

        return new ScoreItemDTO()
                .setBizId(record.getBizId())
                .setMeetupId(record.getRallyMeetupId())
                .setResultType(resultType)
                .setResultTypeShow(resultType.getShow())
                .setMatchType(record.getMatchType())
                .setMatchTypeShow(record.getMatchType().getName())
                .setSetFormat(record.getSetFormat())
                .setSetFormatShow(record.getSetFormat().getShow())
                .setDate(record.getMeetupDate().format(DATE_FORMATTER))
                .setMyScore(myScore)
                .setOpponentScore(opponentScore)
                .setMyTiebreakScore(myTiebreakScore)
                .setOpponentTiebreakScore(opponentTiebreakScore)
                .setMyGender(myGender)
                .setTeammateId(teammateId)
                .setTeammateNickname(teammateNickname)
                .setTeammateAvatarUrl(QiniuConfiguration.buildSignedUrl(teammateAvatar))
                .setTeammateGender(teammateGender)
                .setOpponent1Id(opponent1Id)
                .setOpponent1Nickname(opponent1Nickname)
                .setOpponent1AvatarUrl(QiniuConfiguration.buildSignedUrl(opponent1Avatar))
                .setOpponent1Gender(opponent1Gender)
                .setOpponent2Id(opponent2Id)
                .setOpponent2Nickname(opponent2Nickname)
                .setOpponent2AvatarUrl(QiniuConfiguration.buildSignedUrl(opponent2Avatar))
                .setOpponent2Gender(opponent2Gender);
    }

    private String scoreToString(Integer score) {
        return score == null ? null : score.toString();
    }
}
