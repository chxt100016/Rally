package com.rally.domain.meetup.scorerecord;

/**
 * C3 的物理删除载荷。
 *
 * <p>alreadyAbsent 是早期生成签名的兼容字段，统一归一为 false；调用方总是
 * 执行 DELETE，影响零行也视为成功。</p>
 */
public record ScoreRecordRemoval(
        String meetupId,
        String scoreRecordId,
        boolean alreadyAbsent) {

    public ScoreRecordRemoval {
        alreadyAbsent = false;
    }
}
