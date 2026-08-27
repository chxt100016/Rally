package com.rally.domain.meetup.scorerecord;

/** C1 新增比分时生成雪花业务编号。 */
@FunctionalInterface
public interface ScoreRecordIdGenerator {

    String nextScoreRecordId();
}
