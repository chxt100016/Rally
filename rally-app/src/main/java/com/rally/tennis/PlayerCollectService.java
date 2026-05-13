package com.rally.tennis;

import com.rally.client.tennistv.model.AtpDrawsResponse;
import com.rally.client.tennistv.model.MatchesResponse;
import com.rally.client.tennistv.model.AtpOopResponse;
import com.rally.client.wta.model.WtaDrawsResponse;
import com.rally.client.wta.model.WtaMatchesResponse;
import com.rally.db.tennis.repository.TennisPlayerRepository;
import com.rally.tennis.convert.PlayerAppConvertMapper;
import com.rally.tennis.model.Player;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class PlayerCollectService {

    @Resource
    private TennisPlayerRepository tennisPlayerRepository;

    /**
     * 从 live matches 响应中提取球员
     */
    public int collect(List<MatchesResponse.MatchInfo> matches) {
        List<Player> players = new ArrayList<>();
        if (matches == null) {
            return 0;
        }
        for (MatchesResponse.MatchInfo match : matches) {
            if (match.getPlayerTeam1() != null) {
                players.add(PlayerAppConvertMapper.INSTANCE.toPlayer(match.getPlayerTeam1()));
            }
            if (match.getPlayerTeam2() != null) {
                players.add(PlayerAppConvertMapper.INSTANCE.toPlayer(match.getPlayerTeam2()));
            }
        }

        this.savePlayers(players);
        return players.size();
    }

    public void atpFromDraw(AtpDrawsResponse response) {
        if (response == null || response.getMS() == null || CollectionUtils.isEmpty(response.getMS().getRounds())) {
            return;
        }
        List<Player> allPlayers = new ArrayList<>();
        for (AtpDrawsResponse.Round round : response.getMS().getRounds()) {
            if (CollectionUtils.isEmpty(round.getFixtures())) {
                continue;
            }
            for (AtpDrawsResponse.Fixture fixture : round.getFixtures()) {
                allPlayers.addAll(this.extractFromDrawFixture(fixture));
            }
        }

        this.savePlayers(allPlayers);
    }

    /**
     * 从签表 fixture 中提取球员（teamTop + teamBottom）
     */
    public List<Player> extractFromDrawFixture(AtpDrawsResponse.Fixture fixture) {
        List<Player> players = new ArrayList<>();
        if (fixture.getResult() == null) {
            return players;
        }
        if (fixture.getResult().getTeamTop() != null
                && fixture.getResult().getTeamTop().getPlayer() != null) {
            players.add(PlayerAppConvertMapper.INSTANCE
                    .toPlayerFromDraw(fixture.getResult().getTeamTop().getPlayer()));
        }
        if (fixture.getResult().getTeamBottom() != null
                && fixture.getResult().getTeamBottom().getPlayer() != null) {
            players.add(PlayerAppConvertMapper.INSTANCE
                    .toPlayerFromDraw(fixture.getResult().getTeamBottom().getPlayer()));
        }
        return players;
    }

    /**
     * 从 OOP match detail 中提取球员
     */
    public List<Player> extractFromOopMatch(AtpOopResponse.MatchDetail detail) {
        List<Player> players = new ArrayList<>();
        if (detail.getPlayerTeam1() != null) {
            players.add(PlayerAppConvertMapper.INSTANCE.toPlayerFromOop(detail.getPlayerTeam1()));
        }
        if (detail.getPlayerTeam2() != null) {
            players.add(PlayerAppConvertMapper.INSTANCE.toPlayerFromOop(detail.getPlayerTeam2()));
        }
        return players;
    }

    /**
     * 批量保存球员，统一标记为 ATP
     */
    public void savePlayers(List<Player> players) {
        savePlayers(players, "ATP");
    }

    public void wtaFromDraw(WtaDrawsResponse response) {
        if (response == null || response.getData() == null
                || CollectionUtils.isEmpty(response.getData().getDraw())) {
            return;
        }
        List<Player> players = new ArrayList<>();
        for (WtaDrawsResponse.DrawEntry entry : response.getData().getDraw()) {
            // 跳过 BYE 位置
            if ("0".equals(entry.getPlayerId())) {
                continue;
            }
            Player player = new Player();
            player.setPlayerId(entry.getPlayerId());
            player.setFirstName(entry.getPlayerFirstName());
            player.setLastName(entry.getPlayerLastName());
            player.setNationality(entry.getPlayerNatlId());
            players.add(player);
        }
        this.savePlayers(players, "WTA");
    }

    public void savePlayers(List<Player> players, String tour) {
        if (CollectionUtils.isEmpty(players)) {
            return;
        }
        players.forEach(p -> p.setTour(tour));
        tennisPlayerRepository.saveOrUpdateBatch(
                PlayerAppConvertMapper.INSTANCE.toPlayerPOList(players));
    }


}
