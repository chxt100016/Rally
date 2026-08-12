package com.rally.db.tour.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.tour.entity.TourMatchPO;
import com.rally.db.tour.mapper.TourMatchMapper;
import com.rally.domain.tour.model.SetScore;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
                validateWinner(incoming);
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

    /** null fields never erase collected data; setsJson is an all-or-nothing snapshot. */
    static TourMatchPO merge(TourMatchPO existing, TourMatchPO incoming) {
        if (!Objects.equals(existing.getDrawId(), incoming.getDrawId())
                || !Objects.equals(existing.getMatchId(), incoming.getMatchId())) {
            throw new IllegalArgumentException("比赛身份不一致，拒绝合并");
        }
        rejectIdentityConflict("tournamentId", existing.getTournamentId(), incoming.getTournamentId());
        rejectIdentityConflict("year", existing.getYear(), incoming.getYear());

        if (hasCompleteParticipants(existing) && hasCompleteParticipants(incoming)
                && incoming.getSetsJson() != null
                && !participantKey(existing).equals(participantKey(incoming))) {
            log.error("比分球员方向冲突，拒绝写入: tourMatchId={}, tournamentId={}, year={}, drawId={}, drawType={}, matchId={}, "
                            + "existingPlayer1Id={}, existingPlayer2Id={}, existingWinnerId={}, existingSetsJson={}, "
                            + "incomingPlayer1Id={}, incomingPlayer2Id={}, incomingWinnerId={}, incomingSetsJson={}",
                    existing.getId(), existing.getTournamentId(), existing.getYear(), existing.getDrawId(),
                    incoming.getDrawType(), existing.getMatchId(),
                    existing.getPlayer1Id(), existing.getPlayer2Id(), existing.getWinnerId(), existing.getSetsJson(),
                    incoming.getPlayer1Id(), incoming.getPlayer2Id(), incoming.getWinnerId(), incoming.getSetsJson());
            throw new IllegalArgumentException("比分球员方向与已有比赛不一致，拒绝写入");
        }

        normalizeScoreOrientation(existing, incoming);

        setIfNotNull(incoming.getMatchIndex(), existing::setMatchIndex);
        setIfNotNull(incoming.getTournamentId(), existing::setTournamentId);
        setIfNotNull(incoming.getYear(), existing::setYear);
        setIfNotNull(incoming.getRoundNumber(), existing::setRoundNumber);
        setIfNotNull(incoming.getRoundName(), existing::setRoundName);
        setIfNotNull(incoming.getPlayer1Id(), existing::setPlayer1Id);
        setIfNotNull(incoming.getPlayer2Id(), existing::setPlayer2Id);
        setIfNotNull(incoming.getWinnerId(), existing::setWinnerId);
        setIfNotNull(incoming.getScheduledAt(), existing::setScheduledAt);
        setIfNotNull(incoming.getStartedAt(), existing::setStartedAt);
        setIfNotNull(incoming.getEndedAt(), existing::setEndedAt);
        setIfNotNull(incoming.getCourt(), existing::setCourt);
        setIfNotNull(incoming.getStatus(), existing::setStatus);
        setIfNotNull(incoming.getDurationMinutes(), existing::setDurationMinutes);
        setIfNotNull(incoming.getScheduledAtText(), existing::setScheduledAtText);
        setIfNotNull(incoming.getCourtSeq(), existing::setCourtSeq);
        setIfNotNull(incoming.getDescription(), existing::setDescription);
        setIfNotNull(incoming.getMatchDate(), existing::setMatchDate);
        setIfNotNull(incoming.getSetsJson(), existing::setSetsJson);
        validateWinner(existing);
        return existing;
    }

    /**
     * Different upstreams may describe the same match in opposite player order. When the participant set is
     * exactly the same and a score snapshot is present, normalize the whole incoming snapshot to the persisted
     * player1/player2 orientation. Partial participant conflicts are intentionally not normalized.
     */
    static void normalizeScoreOrientation(TourMatchPO existing, TourMatchPO incoming) {
        boolean sameParticipantsInDifferentOrder = incoming.getSetsJson() != null
                && hasCompleteParticipants(existing) && hasCompleteParticipants(incoming)
                && participantKey(existing).equals(participantKey(incoming))
                && !Objects.equals(existing.getPlayer1Id(), incoming.getPlayer1Id());
        if (!sameParticipantsInDifferentOrder) return;

        List<SetScore> sets = JSON.parseArray(incoming.getSetsJson(), SetScore.class);
        for (SetScore set : sets) {
            Integer p1Games = set.getP1Games();
            set.setP1Games(set.getP2Games());
            set.setP2Games(p1Games);

            Integer p1Tiebreak = set.getP1Tiebreak();
            set.setP1Tiebreak(set.getP2Tiebreak());
            set.setP2Tiebreak(p1Tiebreak);
        }

        String originalPlayer1Id = incoming.getPlayer1Id();
        incoming.setPlayer1Id(incoming.getPlayer2Id());
        incoming.setPlayer2Id(originalPlayer1Id);
        incoming.setSetsJson(JSON.toJSONString(sets));
    }

    private static boolean hasCompleteParticipants(TourMatchPO match) {
        return match.getPlayer1Id() != null && match.getPlayer2Id() != null;
    }

    private static String participantKey(TourMatchPO match) {
        String player1Id = match.getPlayer1Id();
        String player2Id = match.getPlayer2Id();
        return player1Id.compareTo(player2Id) <= 0
                ? player1Id + "|" + player2Id
                : player2Id + "|" + player1Id;
    }

    static void validateIdentity(TourMatchPO match) {
        if (match.getTournamentId() == null || match.getYear() == null
                || match.getDrawType() == null || match.getDrawType().isBlank()
                || match.getDrawId() == null || match.getMatchId() == null || match.getMatchId().isBlank()) {
            throw new IllegalArgumentException("保存比赛必须提供 tournamentId、year、drawType、drawId、matchId");
        }
    }

    static void validateWinner(TourMatchPO match) {
        if (match.getWinnerId() != null
                && match.getPlayer1Id() != null && match.getPlayer2Id() != null
                && !match.getWinnerId().equals(match.getPlayer1Id())
                && !match.getWinnerId().equals(match.getPlayer2Id())) {
            log.error("比赛 winner 非法，拒绝写入: tourMatchId={}, tournamentId={}, year={}, drawId={}, drawType={}, "
                            + "matchId={}, player1Id={}, player2Id={}, winnerId={}, setsJson={}",
                    match.getId(), match.getTournamentId(), match.getYear(), match.getDrawId(), match.getDrawType(),
                    match.getMatchId(), match.getPlayer1Id(), match.getPlayer2Id(), match.getWinnerId(), match.getSetsJson());
            throw new IllegalArgumentException("winnerId 不属于比赛双方，拒绝写入");
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
