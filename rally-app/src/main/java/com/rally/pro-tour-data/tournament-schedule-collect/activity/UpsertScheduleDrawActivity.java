package com.rally.protourdata.tournamentschedulecollect.activity;

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
import com.rally.tour.parser.DrawParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 业务活动 upsert-schedule-draw：为一份有效单打赛程关联或建立签表。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UpsertScheduleDrawActivity {

    private static final String ATP_SINGLES_DRAW_TYPE = "MS";
    private static final String WTA_SINGLES_DRAW_TYPE = "LS";

    private final TourTournamentRepository tournamentRepository;
    private final TourDrawRepository drawRepository;

    /**
     * 签表单独提交；后续赛程比赛、球员或参赛资料失败时不回滚本步。
     *
     * @return 内部 drawId；空来源、未知巡回赛、非目标单打或身份错配时返回 {@code null}
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Long execute(DrawParams target, MatchCollectResult sourceDraw) {
        // A1：来源路由在外层按 tour/category 选择；本活动保留
        // main 的空来源/无目标单打跳过语义，并将未知 tour 视为不可路由。
        if (target == null
                || sourceDraw == null
                || sourceDraw.getMatches() == null
                || sourceDraw.getMatches().isEmpty()) {
            return null;
        }

        String expectedDrawType = expectedSinglesDrawType(target.getTour());
        if (expectedDrawType == null) {
            return null;
        }

        // A2：赛程仅接受当前目标的 ATP MS / WTA LS。赛事编号
        // 按数字比较，保留 main 对请求参数前导零的兼容。
        if (sourceDraw.getDiscipline() != Discipline.SINGLES
                || !expectedDrawType.equals(sourceDraw.getDrawTypeCode())
                || sourceDraw.getYear() != target.getYear()
                || !sameTournamentId(sourceDraw.getTournamentId(), target.getTournamentId())) {
            return null;
        }

        String tournamentId = target.getTournamentId();
        if (!tournamentRepository.exists(tournamentId)) {
            log.warn("Tournament not found, skip schedule draw: {}", tournamentId);
            return null;
        }

        // A3：自然身份保留来源原始 drawType。size/totalRounds 相互独立，
        // 只以非 null 来源值覆盖存量，允许零值且不推导数学结构。
        DrawMeta meta = sourceDraw.getDrawMeta();
        Integer size = meta == null ? null : meta.getDrawSize();
        Integer totalRounds = meta == null ? null : meta.getTotalRounds();
        DrawRepositoryPersistence persistence = new DrawRepositoryPersistence();
        TourDraw draw = TourDraw.associate(new AssociateTourDrawCommand(
                tournamentId,
                target.getYear(),
                sourceDraw.getDrawTypeCode(),
                true,
                size,
                totalRounds), persistence);
        return draw.id();
    }

    private static String expectedSinglesDrawType(String tour) {
        if ("ATP".equals(tour)) {
            return ATP_SINGLES_DRAW_TYPE;
        }
        if ("WTA".equals(tour)) {
            return WTA_SINGLES_DRAW_TYPE;
        }
        return null;
    }

    private static boolean sameTournamentId(String sourceId, String targetId) {
        if (sourceId == null || targetId == null) {
            return false;
        }
        try {
            return Integer.valueOf(sourceId).equals(Integer.valueOf(targetId));
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    /** 将既有仓储适配为 @tour.draw 的 C1/C2 写端口。 */
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
