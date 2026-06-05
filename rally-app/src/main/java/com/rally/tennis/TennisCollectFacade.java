package com.rally.tennis;

import com.rally.db.tennis.entity.TennisTournamentPO;
import com.rally.db.tennis.repository.TennisTournamentRepository;
import com.rally.db.user.entity.UserPO;
import com.rally.db.user.mapper.UserMapper;
import com.rally.domain.auth.gateway.TokenGateway;
import com.rally.tennis.model.TourEnums;
import com.rally.tennis.parser.CollectType;
import com.rally.tennis.parser.DrawParams;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class TennisCollectFacade {

    @Resource
    private TournamentCollectService tournamentCollectService;

    @Resource
    private PlayerCollectService playerCollectService;

    @Resource
    private MatchCollectManager matchCollectManager;

    @Resource
    private TennisTournamentRepository tennisTournamentRepository;

    @Resource
    private UserMapper userMapper;

    @Resource
    private TokenGateway tokenGateway;

    public void tournaments(int year) {
        tournamentCollectService.collectTournament(year);
    }

    public void currentDraws() {
        List<TennisTournamentPO> tournaments = tournamentCollectService.current();
        if (CollectionUtils.isEmpty(tournaments)) {
            log.info("当前无进行中的赛事");
            return;
        }

        for (TennisTournamentPO tournament : tournaments) {
            try {
                this.draws(tournament);
            } catch (Exception e) {
                log.error("采集签表失败, tournamentId={}", tournament.getTournamentId(), e);
            }
        }
    }

    public void draws(TennisTournamentPO tournament) {
        DrawParams params = new DrawParams(tournament.getTournamentId(), tournament.getYear(), tournament.getTour());
        switch (TourEnums.valueOf(tournament.getTour())) {
            case ATP -> matchCollectManager.collect(CollectType.ATP_APP_DRAW, params);
            case WTA -> {
                if (tournament.getCategory().equals("GS")) {
                    matchCollectManager.collect(CollectType.ATP_APP_DRAW, params);
                    matchCollectManager.collect(CollectType.ATP_APP_COMPLETED, params);
                } else {
                    matchCollectManager.collect(CollectType.WTA_DRAW, params);

                }

            }
        }
    }

    public void completed(TennisTournamentPO tournament) {
        DrawParams params = new DrawParams(tournament.getTournamentId(), tournament.getYear(), tournament.getTour());
        matchCollectManager.collect(CollectType.ATP_APP_COMPLETED, params);
    }

    public void oop() {
        List<TennisTournamentPO> current = tournamentCollectService.current();
        for (TennisTournamentPO item : current) {
            if ("WTA".equals(item.getTour())) {
                if (item.getCategory().equals("GS")) {
                    matchCollectManager.collect(CollectType.ATP_SCHEDULE_FOR_WTA, new DrawParams(item.getTournamentId(), item.getYear(), item.getTour()));
                } else {
                    matchCollectManager.collect(CollectType.WTA_SCHEDULE, new DrawParams(item.getTournamentId(), item.getYear(), item.getTour()));
                }

            } else if ("ATP".equals(item.getTour())) {
                matchCollectManager.collect(CollectType.ATP_SCHEDULE, new DrawParams(item.getTournamentId(), item.getYear(), item.getTour()));
            }
        }
    }

    public void liveMatch() {
//        matchCollectManager.collect(CollectType.ATP_LIVE, null);

        List<TennisTournamentPO> tournaments = tournamentCollectService.current();
        if (CollectionUtils.isEmpty(tournaments)) return;

        for (TennisTournamentPO tournament : tournaments) {
            matchCollectManager.collect(CollectType.ATP_APP_LIVE, new DrawParams(tournament.getTournamentId(), tournament.getYear(), tournament.getTour()));
        }
    }

    public void rank() {
        playerCollectService.atpRank();
        playerCollectService.wtaRank();
    }

    public void draws(String tournamentId) {
        TennisTournamentPO byTournamentId = this.tennisTournamentRepository.findByTournamentId(tournamentId);
        this.draws(byTournamentId);
    }

    /**
     * 测试接口：查询所有用户并生成token
     * @return userId -> token 映射
     */
    public Map<String, String> getAllUserTokens() {
        List<UserPO> users = userMapper.selectList(null);
        Map<String, String> result = new HashMap<>();
        for (UserPO user : users) {
            String token = tokenGateway.issue(user.getUserId());
            result.put(user.getUserId(), token);
        }
        return result;
    }
}
