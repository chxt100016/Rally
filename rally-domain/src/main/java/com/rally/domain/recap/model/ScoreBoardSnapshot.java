package com.rally.domain.recap.model;

import com.rally.domain.recap.model.ScoreRecordData;
import lombok.Data;

import java.util.List;

/**
 * 比分快照（活动级共享数据，以「快照 + 版本」形式引用）
 * 聚合根持有，用于乐观锁校验和 diff 比对
 */
@Data
public class ScoreBoardSnapshot {

    /** 当前版本号 */
    private int version;

    /** 当前全部盘比分 */
    private List<ScoreRecordData> scores;
}
