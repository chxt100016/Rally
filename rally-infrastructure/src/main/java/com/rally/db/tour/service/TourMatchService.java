package com.rally.db.tour.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.tour.entity.TourMatchPO;
import com.rally.db.tour.mapper.TourMatchMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TourMatchService extends ServiceImpl<TourMatchMapper, TourMatchPO> {

    @Transactional(rollbackFor = Exception.class)
    public List<TourMatchPO> saveOrUpdateBatch(List<TourMatchPO> matches) {
        if (CollectionUtils.isEmpty(matches)) {
            return List.of();
        }

        matches.forEach(TourMatchService::validateIdentity);
        matches = new java.util.ArrayList<>(matches.stream().collect(Collectors.toMap(
                TourMatchService::key,
                Function.identity(),
                TourMatchService::merge,
                LinkedHashMap::new
        )).values());

        List<String> matchIds = matches.stream()
                .map(TourMatchPO::getMatchId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<String, TourMatchPO> existMap = this.lambdaQuery()
                .in(TourMatchPO::getMatchId, matchIds)
                .list()
                .stream()
                .collect(Collectors.toMap(
                        m -> m.getMatchId() + "|" + m.getDrawId(),
                        m -> m,
                        (a, b) -> a));

        List<TourMatchPO> toInsert = new java.util.ArrayList<>();
        List<TourMatchPO> toUpdate = new java.util.ArrayList<>();
        List<TourMatchPO> saved = new java.util.ArrayList<>(matches.size());
        for (TourMatchPO incoming : matches) {
            TourMatchPO existing = existMap.get(key(incoming));
            if (existing == null) {
                toInsert.add(incoming);
                saved.add(incoming);
            } else {
                TourMatchPO merged = merge(existing, incoming);
                toUpdate.add(merged);
                saved.add(merged);
            }
        }

        if (CollectionUtils.isNotEmpty(toInsert)) {
            this.saveBatch(toInsert);
            log.info("批量插入比赛: {}条", toInsert.size());
        }
        if (CollectionUtils.isNotEmpty(toUpdate)) {
            this.updateBatchById(toUpdate);
            log.info("批量更新比赛: {}条", toUpdate.size());
        }
        return saved;
    }

    /** Null/blank fields never erase collected data; all other fields overwrite directly. */
    static TourMatchPO merge(TourMatchPO existing, TourMatchPO incoming) {
        rejectIdentityConflict("tournamentId", existing.getTournamentId(), incoming.getTournamentId());
        rejectIdentityConflict("year", existing.getYear(), incoming.getYear());
        rejectIdentityConflict("matchId", existing.getMatchId(), incoming.getMatchId());

        setIfNotNull(incoming.getMatchIndex(), existing::setMatchIndex);
        setIfNotBlank(incoming.getTournamentId(), existing::setTournamentId);
        setIfNotNull(incoming.getYear(), existing::setYear);
        setIfNotNull(incoming.getRoundNumber(), existing::setRoundNumber);
        setIfNotBlank(incoming.getRoundName(), existing::setRoundName);
        setIfNotBlank(incoming.getPlayer1Id(), existing::setPlayer1Id);
        setIfNotBlank(incoming.getPlayer2Id(), existing::setPlayer2Id);
        setIfNotBlank(incoming.getWinnerId(), existing::setWinnerId);
        setIfNotNull(incoming.getScheduledAt(), existing::setScheduledAt);
        setIfNotNull(incoming.getStartedAt(), existing::setStartedAt);
        setIfNotNull(incoming.getEndedAt(), existing::setEndedAt);
        setIfNotBlank(incoming.getCourt(), existing::setCourt);
        setIfNotBlank(incoming.getStatus(), existing::setStatus);
        setIfNotNull(incoming.getDurationMinutes(), existing::setDurationMinutes);
        setIfNotBlank(incoming.getScheduledAtText(), existing::setScheduledAtText);
        setIfNotNull(incoming.getCourtSeq(), existing::setCourtSeq);
        setIfNotBlank(incoming.getDescription(), existing::setDescription);
        setIfNotNull(incoming.getMatchDate(), existing::setMatchDate);
        setIfNotBlank(incoming.getSetsJson(), existing::setSetsJson);
        return existing;
    }

    static void validateIdentity(TourMatchPO match) {
        if (match.getTournamentId() == null || match.getTournamentId().isBlank()
                || match.getYear() == null
                || match.getMatchId() == null || match.getMatchId().isBlank()) {
            throw new IllegalArgumentException("保存比赛必须提供 tournamentId、year、matchId");
        }
    }

    private static <T> void rejectIdentityConflict(String field, T existing, T incoming) {
        if (existing != null && incoming != null && !Objects.equals(existing, incoming)) {
            throw new IllegalArgumentException("比赛身份字段 " + field + " 冲突，拒绝合并");
        }
    }

    private static String key(TourMatchPO match) {
        return match.getMatchId() + "|" + match.getDrawId();
    }

    private static <T> void setIfNotNull(T value, java.util.function.Consumer<T> setter) {
        if (value != null) setter.accept(value);
    }

    private static void setIfNotBlank(String value, java.util.function.Consumer<String> setter) {
        if (value != null && !value.isBlank()) setter.accept(value);
    }

    public List<TourMatchPO> findActiveByTournament(String tournamentId, Integer year) {
        return this.lambdaQuery()
                .eq(TourMatchPO::getTournamentId, tournamentId)
                .eq(TourMatchPO::getYear, year)
                .in(TourMatchPO::getStatus, "live", "scheduled")
                .list();
    }

    public boolean hasActiveMatches() {
        return this.lambdaQuery()
                .in(TourMatchPO::getStatus, "live", "scheduled")
                .exists();
    }

    public List<TourMatchPO> findByTournamentIdsAndDate(List<String> tournamentIds, LocalDate date) {
        if (CollectionUtils.isEmpty(tournamentIds) || date == null) {
            return List.of();
        }
        return this.lambdaQuery()
                .in(TourMatchPO::getTournamentId, tournamentIds)
                .eq(TourMatchPO::getMatchDate, date)
                .list();
    }
}
