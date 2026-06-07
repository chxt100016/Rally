package com.rally.domain.recap.model;

import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.model.RegistrationData;
import com.rally.domain.review.model.ReviewData;
import com.rally.domain.review.model.ScoreRecordData;

import java.util.List;

/**
 * 赛后收集聚合根工厂
 */
public class RecapFactory {

    /**
     * 构建 Recap 聚合根
     *
     * @param userId       当前用户 ID
     * @param meetupId     活动 ID
     * @param meetup       活动数据
     * @param participants 参与人列表
     * @param reviews      当前用户的评价列表（fromUser = userId）
     * @param scores       该场全部比分
     * @return Recap 聚合根
     */
    public static Recap build(String userId, String meetupId, MeetupData meetup,
                              List<RegistrationData> participants,
                              List<ReviewData> reviews,
                              List<ScoreRecordData> scores) {
        // 构建比分快照（version 取该场最大值，确保 meetup 级一致性）
        ScoreBoardSnapshot scoreBoard = new ScoreBoardSnapshot();
        if (scores != null && !scores.isEmpty()) {
            int maxVersion = 0;
            for (ScoreRecordData s : scores) {
                if (s.getVersion() != null && s.getVersion() > maxVersion) {
                    maxVersion = s.getVersion();
                }
            }
            scoreBoard.setVersion(maxVersion);
            scoreBoard.setScores(scores);
        } else {
            scoreBoard.setVersion(0);
        }

        return new Recap(userId, meetupId, meetup, participants, reviews, scoreBoard);
    }
}
