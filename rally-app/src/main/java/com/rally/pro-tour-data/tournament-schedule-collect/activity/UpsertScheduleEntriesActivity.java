package com.rally.protourdata.tournamentschedulecollect.activity;

import com.rally.domain.tour.model.TournamentEntryData;
import com.rally.domain.tour.repository.TourEntryRepository;
import com.rally.domain.tour.tournamententry.RefreshTourTournamentEntryCommand;
import com.rally.domain.tour.tournamententry.TourTournamentEntry;
import com.rally.domain.tour.tournamententry.TourTournamentEntryIdentity;
import com.rally.domain.tour.tournamententry.TourTournamentEntryInsertResult;
import com.rally.domain.tour.tournamententry.TourTournamentEntryPersistence;
import com.rally.domain.tour.tournamententry.TourTournamentEntryQualificationPatch;
import com.rally.domain.tour.tournamententry.TourTournamentEntryState;
import com.rally.domain.tour.tournamententry.TourTournamentEntryStatus;
import com.rally.tour.model.TournamentEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务活动 upsert-schedule-entries：按赛程来源补充签表参赛资格。
 */
@Component
@RequiredArgsConstructor
public class UpsertScheduleEntriesActivity {

    private final TourEntryRepository tourEntryRepository;

    /**
     * 参赛资料使用赛程采集的最后一个独立事务；失败不补偿此前保存步骤。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void execute(Long drawId, List<TournamentEntry> sourceEntries) {
        if (sourceEntries == null || sourceEntries.isEmpty()) {
            return;
        }
        if (drawId == null || drawId <= 0) {
            throw new IllegalArgumentException("保存赛程参赛信息必须提供有效 drawId");
        }

        // A1-A2：来源客户端已经完成种子解析；无法解析时 seed 为 null。
        // 将可识别球员关联到本签表，并按复合身份稳定去重。后到重复项
        // 只以非 null seed/entryType 合并，保留 raw entryType 原值。
        Map<String, TournamentEntry> deduplicated = new LinkedHashMap<>();
        for (TournamentEntry source : sourceEntries) {
            if (source == null || source.getPlayerId() == null) {
                continue;
            }
            source.setDrawId(drawId);
            deduplicated.merge(source.getPlayerId(), source,
                    UpsertScheduleEntriesActivity::mergeNonNull);
        }
        if (deduplicated.isEmpty()) {
            return;
        }

        // A3：通过 @tour.tournament-entry C1 新建或非空刷新资格。
        // 只在本批完成后调用一次既有批量 upsert；来源遗漏项不会进入写集合。
        EntryBatchPersistence persistence = new EntryBatchPersistence(drawId);
        for (TournamentEntry entry : deduplicated.values()) {
            TourTournamentEntry.saveOrRefresh(new RefreshTourTournamentEntryCommand(
                    drawId,
                    entry.getPlayerId(),
                    entry.getSeed(),
                    entry.getEntryType()), persistence);
        }
        persistence.flush();
    }

    private static TournamentEntry mergeNonNull(
            TournamentEntry existing, TournamentEntry incoming) {
        if (incoming.getSeed() != null) {
            existing.setSeed(incoming.getSeed());
        }
        if (incoming.getEntryType() != null) {
            existing.setEntryType(incoming.getEntryType());
        }
        return existing;
    }

    /**
     * 将既有批量 DTO 仓储适配为参赛聚合 C1 的持久化端口。
     */
    private final class EntryBatchPersistence implements TourTournamentEntryPersistence {

        private final Long drawId;
        private final Map<TourTournamentEntryIdentity, TourTournamentEntryState> states =
                new LinkedHashMap<>();
        private final Map<Long, TourTournamentEntryIdentity> identitiesBySessionId =
                new LinkedHashMap<>();
        private final Map<TourTournamentEntryIdentity, TourTournamentEntryState> dirty =
                new LinkedHashMap<>();
        private long nextSessionId = 1L;

        private EntryBatchPersistence(Long drawId) {
            this.drawId = drawId;
            List<TournamentEntryData> existing =
                    tourEntryRepository.listByDrawIds(List.of(drawId));
            if (existing == null) {
                return;
            }
            for (TournamentEntryData entry : existing) {
                if (entry == null
                        || entry.getDrawId() == null
                        || entry.getPlayerId() == null) {
                    continue;
                }
                long sessionId = nextSessionId++;
                TourTournamentEntryState state = new TourTournamentEntryState(
                        sessionId,
                        entry.getDrawId(),
                        entry.getPlayerId(),
                        entry.getSeed(),
                        entry.getEntryType(),
                        TourTournamentEntryStatus.CONFIRMED,
                        null,
                        null);
                TourTournamentEntryIdentity identity =
                        TourTournamentEntryIdentity.of(
                                entry.getDrawId(), entry.getPlayerId());
                states.put(identity, state);
                identitiesBySessionId.put(sessionId, identity);
            }
        }

        @Override
        public TourTournamentEntryState findByIdentity(
                TourTournamentEntryIdentity identity) {
            return states.get(identity);
        }

        @Override
        public TourTournamentEntryInsertResult insert(TourTournamentEntryState state) {
            long sessionId = nextSessionId++;
            TourTournamentEntryState saved = new TourTournamentEntryState(
                    sessionId,
                    state.drawId(),
                    state.playerId(),
                    state.seed(),
                    state.entryType(),
                    state.status(),
                    state.createTime(),
                    state.updateTime());
            TourTournamentEntryIdentity identity =
                    TourTournamentEntryIdentity.of(saved.drawId(), saved.playerId());
            states.put(identity, saved);
            identitiesBySessionId.put(sessionId, identity);
            dirty.put(identity, saved);
            return TourTournamentEntryInsertResult.created(sessionId);
        }

        @Override
        public boolean applyNonNullQualificationPatch(
                long id, TourTournamentEntryQualificationPatch patch) {
            TourTournamentEntryIdentity identity = identitiesBySessionId.get(id);
            TourTournamentEntryState existing = states.get(identity);
            if (existing == null) {
                return false;
            }
            TourTournamentEntryState updated = new TourTournamentEntryState(
                    existing.id(),
                    existing.drawId(),
                    existing.playerId(),
                    patch.seed() == null ? existing.seed() : patch.seed(),
                    patch.entryType() == null
                            ? existing.entryType()
                            : patch.entryType(),
                    existing.status(),
                    existing.createTime(),
                    existing.updateTime());
            states.put(identity, updated);
            dirty.put(identity, updated);
            return true;
        }

        @Override
        public boolean updateStatus(long id, TourTournamentEntryStatus targetStatus) {
            throw new UnsupportedOperationException(
                    "upsert-schedule-entries 只允许调用参赛资格 C1");
        }

        private void flush() {
            if (dirty.isEmpty()) {
                return;
            }
            List<TournamentEntryData> entries = new ArrayList<>(dirty.size());
            for (TourTournamentEntryState state : dirty.values()) {
                TournamentEntryData entry = new TournamentEntryData();
                entry.setDrawId(drawId);
                entry.setPlayerId(state.playerId());
                entry.setSeed(state.seed());
                entry.setEntryType(state.entryType());
                entries.add(entry);
            }
            tourEntryRepository.saveEntries(entries);
        }
    }
}
