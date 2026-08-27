package com.rally.domain.media.assetstorage;

/**
 * 对象删除结论。DELETED 与 ALREADY_ABSENT 都表示删除目标已不存在。
 */
public enum AssetDeleteOutcome {
    DELETED,
    ALREADY_ABSENT,
    FAILED
}
