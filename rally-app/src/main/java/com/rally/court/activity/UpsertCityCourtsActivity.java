package com.rally.court.activity;

import com.rally.court.model.UpsertResult;
import com.rally.domain.court.enums.CourtCollectModeEnum;
import com.rally.domain.court.gateway.CourtRepository;
import com.rally.domain.court.model.Court;
import com.rally.domain.court.model.CourtCollectCmd;
import com.rally.domain.court.model.CourtData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务活动 upsert-city-courts：按全量覆盖或增量模式把解析好的球场写入球场库并统计。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UpsertCityCourtsActivity {

    /** 分批提交规模 */
    private static final int BATCH_SIZE = 100;

    private final CourtRepository courtRepository;

    public UpsertResult execute(List<CourtCollectCmd> courts, CourtCollectModeEnum mode) {
        UpsertResult result = new UpsertResult();
        if (courts == null || courts.isEmpty()) {
            return result;
        }
        // A1 按本批球场资料的三方来源编号，一次批量取出其中已经收录过的那些
        Map<String, CourtData> existing = loadExisting(courts);

        List<CourtData> pendingInsert = new ArrayList<>();
        List<CourtData> pendingUpdate = new ArrayList<>();
        int skipped = 0;
        for (CourtCollectCmd cmd : courts) {
            CourtData found = existing.get(cmd.getSourceId());
            if (found == null) {
                // A4 未收录的球场一律新建
                pendingInsert.add(Court.collect(cmd).state());
                continue;
            }
            if (CourtCollectModeEnum.INCREMENT.equals(mode)) {
                // A2 增量模式下跳过已收录的球场
                skipped++;
                continue;
            }
            // A3 全量覆盖模式下把已收录的球场按本次资料改写
            try {
                Court court = Court.of(found);
                court.overwriteByCollect(cmd);
                pendingUpdate.add(court.state());
                // 同一批内出现重复三方来源编号时，后写的按已收录处理
                existing.put(cmd.getSourceId(), court.state());
            } catch (Exception e) {
                log.warn("球场覆盖失败，跳过该条。sourceId={}", cmd.getSourceId(), e);
            }
        }
        // A5 汇总新增、改写、跳过条数
        result.setInsertedCount(saveInBatches(pendingInsert));
        result.setUpdatedCount(saveInBatches(pendingUpdate));
        result.setSkippedCount(skipped);
        return result;
    }

    private Map<String, CourtData> loadExisting(List<CourtCollectCmd> courts) {
        List<String> sourceIds = courts.stream().map(CourtCollectCmd::getSourceId).filter(StringUtils::isNotBlank).distinct().toList();
        Map<String, CourtData> existing = new HashMap<>();
        for (CourtData data : courtRepository.findBySourceIds(sourceIds)) {
            existing.put(data.getSourceId(), data);
        }
        return existing;
    }

    /** 分批提交，单条写入失败时跳过该条继续写其余的，已写入的保留不回滚 */
    private int saveInBatches(List<CourtData> courts) {
        int success = 0;
        for (int i = 0; i < courts.size(); i += BATCH_SIZE) {
            List<CourtData> batch = courts.subList(i, Math.min(i + BATCH_SIZE, courts.size()));
            success += courtRepository.batchSave(batch);
        }
        return success;
    }
}
