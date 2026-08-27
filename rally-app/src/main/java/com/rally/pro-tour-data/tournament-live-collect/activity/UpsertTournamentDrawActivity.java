package com.rally.protourdata.tournamentlivecollect.activity;

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
import com.rally.tour.model.TourEnums;
import com.rally.tour.parser.DrawParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 业务活动 upsert-tournament-draw：为一批有效实时单打比赛关联或建立签表。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UpsertTournamentDrawActivity {

    private static final String ATP_SINGLES_DRAW_TYPE = "MS";
    private static final String WTA_SINGLES_DRAW_TYPE = "LS";

    private final TourTournamentRepository tournamentRepository;
    private final TourDrawRepository drawRepository;

    /**
     * 签表独立提交；后续实时比赛批次失败不补偿已建立的签表。
     *
     * @return 内部 drawId；空来源、非目标单打或身份错配时返回 {@code null}
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Long execute(DrawParams target, MatchCollectResult sourceDraw) {
        // A1：RPC 空响应由 client 转为空集合；活动对直接调用
        // 仍做最后防线。未知 tour 沿用 main 的 valueOf 失败语义。
        if (sourceDraw == null
                || sourceDraw.getMatches() == null
                || sourceDraw.getMatches().isEmpty()) {
            return null;
        }
        if (target == null) {
            throw new IllegalArgumentException("实时签表必须提供目标赛事");
        }

        String expectedDrawType = switch (TourEnums.valueOf(target.getTour())) {
            case ATP -> ATP_SINGLES_DRAW_TYPE;
            case WTA -> WTA_SINGLES_DRAW_TYPE;
        };

        // A2：ATP 仅接受 MS，WTA 仅接受 LS；响应赛事/年份
        // 不属于当前目标时整批丢弃。赛事编号按数字比较，
        // 保留 main 对前导零的兼容。
        if (sourceDraw.getDiscipline() != Discipline.SINGLES
                || !expectedDrawType.equals(sourceDraw.getDrawTypeCode())
                || sourceDraw.getYear() != target.getYear()
                || !sameTournamentId(sourceDraw.getTournamentId(), target.getTournamentId())) {
            return null;
        }

        String tournamentId = target.getTournamentId();
        if (!tournamentRepository.exists(tournamentId)) {
            log.warn("Tournament not found, skip live draw: {}", tournamentId);
            return null;
        }

        // A3：实时来源只关联原始 MS/LS 身份，始终传 null 结构，
        // 因此既不推断也不刷新 size/totalRounds。
        DrawRepositoryPersistence persistence = new DrawRepositoryPersistence();
        TourDraw draw = TourDraw.associate(new AssociateTourDrawCommand(
                tournamentId,
                target.getYear(),
                sourceDraw.getDrawTypeCode(),
                true,
                null,
                null), persistence);
        return draw.id();
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
                    .filter(draw -> Objects.equals(
                            draw.getTournamentId(), identity.tournamentId()))
                    .filter(draw -> Objects.equals(draw.getYear(), identity.year()))
                    .filter(draw -> Objects.equals(
                            draw.getDrawType(), identity.drawTypeCode()))
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
