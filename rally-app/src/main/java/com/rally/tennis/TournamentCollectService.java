package com.rally.tennis;

import com.rally.client.tennistv.TennisTvClient;
import com.rally.client.tennistv.model.MatchesResponse;
import com.rally.client.wta.WtaClient;
import com.rally.client.wta.model.WtaTournamentsResponse;
import com.rally.domain.tennis.gateway.TennisEntryGateway;
import com.rally.domain.tennis.gateway.TennisTournamentGateway;
import com.rally.domain.tennis.model.TournamentData;
import com.rally.domain.tennis.model.TournamentEntryData;
import com.rally.tennis.convert.TournamentAppConvertMapper;
import com.rally.tennis.convert.WtaTournamentAppConvertMapper;
import com.rally.tennis.model.Tournament;
import com.rally.tennis.model.TournamentEntry;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class TournamentCollectService {

    @Resource
    private TennisTvClient tennisTvClient;

    @Resource
    private WtaClient wtaClient;

    @Resource
    private TennisTournamentGateway tennisTournamentGateway;

    @Resource
    private TennisEntryGateway tennisEntryGateway;

    public List<TournamentData> current() {
        LocalDate today = LocalDate.now();
        return tennisTournamentGateway.findCurrentTournaments(today);
    }

    public boolean exists(String tournamentId) {
        return tennisTournamentGateway.exists(tournamentId);
    }

    public void saveEntries(List<TournamentEntry> entries) {
        if (CollectionUtils.isEmpty(entries)) return;
        List<TournamentEntryData> dataList = new ArrayList<>();
        for (TournamentEntry entry : entries) {
            TournamentEntryData data = new TournamentEntryData();
            data.setPlayerId(entry.getPlayerId());
            data.setDrawId(entry.getDrawId());
            data.setSeed(entry.getSeed());
            data.setEntryType(entry.getEntryType());
            dataList.add(data);
        }
        tennisEntryGateway.saveEntries(dataList);
    }

    public void collectTournament(int year) {
        this.atp(year);
        this.wta(year);
    }

    public void atp(int year) {
        List<MatchesResponse.TournamentInfo> infos = tennisTvClient.getTournaments(year);
        if (CollectionUtils.isEmpty(infos)) {
            log.warn("从API获取赛事列表为空, year={}", year);
        }
        List<Tournament> tournaments = infos.stream()
                .map(TournamentAppConvertMapper.INSTANCE::toTournament)
                .peek(item -> item.setTour("ATP"))
                .toList();
        tennisTournamentGateway.saveOrUpdateBatch(TournamentAppConvertMapper.INSTANCE.toTournamentDataList(tournaments));
        log.info("ATP赛事采集完成: year={}, 数量={}", year, tournaments.size());
    }

    public void wta(int year) {
        WtaTournamentsResponse response = wtaClient.getTournaments(year);
        if (response == null || CollectionUtils.isEmpty(response.getContent())) {
            log.warn("从WTA API获取赛事列表为空, year={}", year);
            return;
        }
        List<Tournament> tournaments = response.getContent().stream()
                .map(WtaTournamentAppConvertMapper.INSTANCE::toTournament)
                .toList();
        tennisTournamentGateway.saveOrUpdateBatch(WtaTournamentAppConvertMapper.INSTANCE.toTournamentDataList(tournaments));
        log.info("WTA赛事采集完成: year={}, 数量={}", year, tournaments.size());
    }
}
