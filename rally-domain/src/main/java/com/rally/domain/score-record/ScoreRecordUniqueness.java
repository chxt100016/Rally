package com.rally.domain.meetup.scorerecord;

/** I1 的当前事务同场盘号预检端口；数据库唯一键仍是并发竞争的最终防线。 */
public interface ScoreRecordUniqueness {

    /** 保留早期生成签名；C1 不调用业务编号预检。 */
    boolean businessIdExists(String scoreRecordId);

    /**
     * 检查同场盘号是否已占用。仅 C1 使用，excludedScoreRecordId 传 null。
     */
    boolean meetupSetExists(
            String meetupId,
            int setNumber,
            String excludedScoreRecordId);
}
