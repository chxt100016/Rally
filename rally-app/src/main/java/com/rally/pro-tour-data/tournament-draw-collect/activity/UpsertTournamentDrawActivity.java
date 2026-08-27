package com.rally.protourdata.tournamentdrawcollect.activity;

import com.rally.domain.tour.draw.AssociateTourDrawCommand;
import com.rally.domain.tour.draw.TourDraw;
import com.rally.domain.tour.draw.TourDrawIdentity;
import com.rally.domain.tour.draw.TourDrawInsertResult;
import com.rally.domain.tour.draw.TourDrawPersistence;
import com.rally.domain.tour.draw.TourDrawState;
import com.rally.domain.tour.model.TourDrawData;
import com.rally.domain.tour.repository.TourDrawRepository;
import com.rally.domain.tour.repository.TourTournamentRepository;
import com.rally.tour.client.MatchCollectResult;
import com.rally.tour.model.Discipline;
import com.rally.tour.parser.DrawMeta;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 业务活动 upsert-tournament-draw：保存一份已路由、可识别的来源签表。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UpsertTournamentDrawActivity {

    private final TourTournamentRepository tournamentRepository;
    private final TourDrawRepository drawRepository;

    @Value("${tour.collect.doubles:false}")
    private boolean collectDoubles;

    /**
     * @return 内部 drawId；来源为空、不可识别、赛事未收录或双打禁用时返回 {@code null}
     */
    @Transactional(rollbackFor = Exception.class)
    public Long execute(MatchCollectResult sourceDraw) {
        // A1：来源路由和 RPC 空响应由外层完成；本活动仍对无可识别签表、
        // 双打开关及本地赛事名录作最后过滤。MIXED 沿用 main，不按双打排除。
        if (sourceDraw == null
                || sourceDraw.getDrawTypeCode() == null
                || sourceDraw.getDrawTypeCode().isBlank()) {
            return null;
        }
        if (sourceDraw.getDiscipline() == Discipline.DOUBLES && !collectDoubles) {
            return null;
        }

        String tournamentId = sourceDraw.getTournamentId();
        if (!tournamentRepository.exists(tournamentId)) {
            log.warn("Tournament not found, skip draw: {}", tournamentId);
            return null;
        }

        // A2-A3：自然身份保留来源原始 drawType；结构字段彼此独立，只有
        // 非 null 来源值覆盖存量。后续比赛保存不属于本事务。
        DrawMeta meta = sourceDraw.getDrawMeta();
        Integer size = meta == null ? null : meta.getDrawSize();
        Integer totalRounds = meta == null ? null : meta.getTotalRounds();
        DrawRepositoryPersistence persistence = new DrawRepositoryPersistence();
        TourDraw draw = TourDraw.associate(new AssociateTourDrawCommand(
                tournamentId,
                sourceDraw.getYear(),
                sourceDraw.getDrawTypeCode(),
                true,
                size,
                totalRounds), persistence);
        return draw.id();
    }

    /**
     * 把既有仓储适配为 @tour.draw 的 C1/C2 写端口；仓储自身按相同自然键
     * upsert，并对 size/totalRounds 分别执行非空覆盖。
     */
    private final class DrawRepositoryPersistence implements TourDrawPersistence {

        private TourDrawIdentity currentIdentity;

        @Override
        public TourDrawState findByIdentity(TourDrawIdentity identity) {
            currentIdentity = identity;
            List<TourDrawData> draws = drawRepository.listByTournamentIds(
                    List.of(identity.tournamentId()));
            if (draws == null) {
                return null;
            }
            return draws.stream()
                    .filter(Objects::nonNull)
                    .filter(draw -> Objects.equals(draw.getTournamentId(), identity.tournamentId()))
                    .filter(draw -> Objects.equals(draw.getYear(), identity.year()))
                    .filter(draw -> Objects.equals(draw.getDrawType(), identity.drawTypeCode()))
                    .findFirst()
                    .map(this::toState)
                    .orElse(null);
        }

        @Override
        public TourDrawInsertResult insert(TourDrawState state) {
            currentIdentity = new TourDrawIdentity(
                    state.tournamentId(), state.year(), state.drawType());
            Long id = drawRepository.saveOrUpdate(
                    state.tournamentId(),
                    state.year(),
                    state.drawType(),
                    state.size(),
                    state.totalRounds());
            return TourDrawInsertResult.created(id);
        }

        @Override
        public boolean refreshStructure(long id, Integer size, Integer totalRounds) {
            if (currentIdentity == null) {
                return false;
            }
            Long updatedId = drawRepository.saveOrUpdate(
                    currentIdentity.tournamentId(),
                    currentIdentity.year(),
                    currentIdentity.drawTypeCode(),
                    size,
                    totalRounds);
            return Objects.equals(updatedId, id);
        }

        private TourDrawState toState(TourDrawData draw) {
            return new TourDrawState(
                    draw.getId(),
                    draw.getTournamentId(),
                    draw.getYear(),
                    draw.getDrawType(),
                    draw.getSize(),
                    draw.getTotalRounds(),
                    null,
                    null);
        }
    }
}
