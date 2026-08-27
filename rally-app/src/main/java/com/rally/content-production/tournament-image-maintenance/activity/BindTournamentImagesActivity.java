package com.rally.contentproduction.tournamentimagemaintenance.activity;

import com.rally.domain.tour.model.TournamentData;
import com.rally.domain.tour.repository.TourTournamentRepository;
import com.rally.domain.tour.tournament.ReplaceTourTournamentImagesCommand;
import com.rally.domain.tour.tournament.TourTournament;
import com.rally.domain.tour.tournament.TourTournamentIdentity;
import com.rally.domain.tour.tournament.TourTournamentImageBinding;
import com.rally.domain.tour.tournament.TourTournamentInsertResult;
import com.rally.domain.tour.tournament.TourTournamentPersistence;
import com.rally.domain.tour.tournament.TourTournamentProfile;
import com.rally.domain.tour.tournament.TourTournamentState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 业务活动 bind-tournament-images：把两项固定资源键绑定到同编号的全部职业赛事年度。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BindTournamentImagesActivity {

    private final TourTournamentRepository tournamentRepository;

    @Transactional(rollbackFor = Exception.class)
    public void execute(String tournamentId, String imageKey, String backgroundKey) {
        if (tournamentId == null || tournamentId.isEmpty()) {
            throw systemError("职业赛事编号为空", tournamentId, null);
        }

        try {
            // A1 匹配范围只使用 tournament_id；仓储会返回该编号下的全部年份。
            List<TournamentData> tournaments = tournamentRepository
                    .listByTournamentIds(List.of(tournamentId));
            if (tournaments == null || tournaments.isEmpty()) {
                // A3 没有匹配记录也是成功，已经保存的七牛对象继续保留。
                return;
            }

            ReplaceTourTournamentImagesCommand command =
                    new ReplaceTourTournamentImagesCommand(imageKey, backgroundKey);
            BulkImageBindingPersistence persistence =
                    new BulkImageBindingPersistence(tournamentId, tournaments);

            // A2 每个年度聚合分别执行 C2；实际落库继续沿用既有的一条按编号批量更新语句。
            for (TournamentData tournament : tournaments) {
                TourTournament.restore(toState(tournament))
                        .replaceImageBinding(command, persistence);
            }
            // A3 所有聚合命令完成后无业务数据返回。
        } catch (RuntimeException exception) {
            throw systemError("绑定职业赛事图片失败", tournamentId, exception);
        }
    }

    private TourTournamentState toState(TournamentData tournament) {
        return new TourTournamentState(
                tournament.getId(),
                tournament.getTournamentId(),
                tournament.getYear(),
                tournament.getName(),
                tournament.getTour(),
                tournament.getCategory(),
                tournament.getSurface(),
                tournament.getCity(),
                tournament.getCountry(),
                tournament.getPrizeMoney(),
                tournament.getPrizeMoneyText(),
                tournament.getStatus(),
                tournament.getStartDate(),
                tournament.getEndDate(),
                tournament.getImagePath(),
                tournament.getBackgroundPath(),
                null,
                null);
    }

    private IllegalStateException systemError(
            String message, String tournamentId, RuntimeException cause) {
        if (cause == null) {
            log.error("{}: tournamentId={}", message, tournamentId);
            return new IllegalStateException(message);
        }
        log.error("{}: tournamentId={}", message, tournamentId, cause);
        return new IllegalStateException(message, cause);
    }

    /**
     * C2 的持久化适配器。现有仓储按 tournament_id 一次更新全部年度，因此第一个真正
     * 发生变化的聚合负责执行该语句，其余年度命令在同一事务内确认已经被同批更新。
     */
    private final class BulkImageBindingPersistence implements TourTournamentPersistence {

        private final String tournamentId;
        private final Set<Long> aggregateIds;
        private boolean bindingPersisted;

        private BulkImageBindingPersistence(
                String tournamentId, List<TournamentData> tournaments) {
            this.tournamentId = tournamentId;
            this.aggregateIds = new HashSet<>();
            for (TournamentData tournament : tournaments) {
                if (tournament != null && tournament.getId() != null) {
                    aggregateIds.add(tournament.getId());
                }
            }
        }

        @Override
        public TourTournamentState findByIdentity(TourTournamentIdentity identity) {
            throw unsupported();
        }

        @Override
        public TourTournamentInsertResult insert(TourTournamentState state) {
            throw unsupported();
        }

        @Override
        public boolean replaceCatalogProfile(long id, TourTournamentProfile profile) {
            throw unsupported();
        }

        @Override
        public boolean replaceImageBinding(long id, TourTournamentImageBinding binding) {
            if (!aggregateIds.contains(id)) {
                return false;
            }
            if (!bindingPersisted) {
                tournamentRepository.updateImagePaths(
                        tournamentId, binding.imagePath(), binding.backgroundPath());
                bindingPersisted = true;
            }
            return true;
        }

        private UnsupportedOperationException unsupported() {
            return new UnsupportedOperationException("图片绑定活动不支持赛事名录写入");
        }
    }
}
