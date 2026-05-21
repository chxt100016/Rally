package com.rally.tennis;

import com.rally.client.tennistv.TennisTvClient;
import com.rally.client.tennistv.model.AtpDrawsResponse;
import com.rally.client.tennistv.model.MatchesResponse;
import com.rally.client.wta.WtaClient;
import com.rally.client.wta.model.WtaDrawsResponse;
import com.rally.client.wta.model.WtaMatchesResponse;
import com.rally.client.wta.model.WtaTournamentsResponse;
import com.rally.db.tennis.entity.TennisTournamentEntryPO;
import com.rally.db.tennis.entity.TennisTournamentPO;
import com.rally.db.tennis.repository.TennisTournamentEntryRepository;
import com.rally.db.tennis.repository.TennisTournamentRepository;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class TournamentCollectService {

    @Resource
    private TennisTvClient tennisTvClient;

    @Resource
    private WtaClient wtaClient;

    @Resource
    private TennisTournamentRepository tennisTournamentRepository;

    @Resource
    private TennisTournamentEntryRepository tennisTournamentEntryRepository;

    /**
     * 查询当前时间在 start_date 和 end_date 之间的赛事
     */
    public List<TennisTournamentPO> current() {
        LocalDate today = LocalDate.now();
        return tennisTournamentRepository.findCurrentTournaments(today);
    }

    public boolean exists(String tournamentId) {
        return tennisTournamentRepository.exists(tournamentId);
    }

    public void saveEntries(List<TournamentEntry> entries) {
        if (CollectionUtils.isEmpty(entries)) return;
        List<TennisTournamentEntryPO> pos = new ArrayList<>();
        for (TournamentEntry entry : entries) {
            TennisTournamentEntryPO po = new TennisTournamentEntryPO();
            po.setPlayerId(entry.getPlayerId());
            po.setDrawId(entry.getDrawId());
            po.setSeed(entry.getSeed());
            po.setEntryType(entry.getEntryType());
            pos.add(po);
        }
        tennisTournamentEntryRepository.saveEntries(pos);
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
        tennisTournamentRepository.saveOrUpdateBatch(TournamentAppConvertMapper.INSTANCE.toTournamentPOList(tournaments));
        log.info("ATP赛事采集完成: year={}, 数量={}", year, tournaments.size());
    }




    /**
     * 拉取并保存指定年份的 WTA 赛事
     */
    public void wta(int year) {
        WtaTournamentsResponse response = wtaClient.getTournaments(year);
        if (response == null || CollectionUtils.isEmpty(response.getContent())) {
            log.warn("从WTA API获取赛事列表为空, year={}", year);
            return;
        }

        List<Tournament> tournaments = response.getContent().stream()
                .map(WtaTournamentAppConvertMapper.INSTANCE::toTournament)
                .toList();
        List<TennisTournamentPO> poList = WtaTournamentAppConvertMapper.INSTANCE.toTournamentPOList(tournaments);
        tennisTournamentRepository.saveOrUpdateBatch(poList);

        log.info("WTA赛事采集完成: year={}, 数量={}", year, poList.size());
    }

    public void wtaTournamentEntry(WtaDrawsResponse response, Long drawId) {
        if (response == null || response.getData() == null
                || CollectionUtils.isEmpty(response.getData().getDraw())) {
            return;
        }
        List<TennisTournamentEntryPO> entries = new ArrayList<>();
        for (WtaDrawsResponse.DrawEntry entry : response.getData().getDraw()) {
            // 跳过 BYE 位置
            if ("0".equals(entry.getPlayerId())) {
                continue;
            }
            TennisTournamentEntryPO po = new TennisTournamentEntryPO();
            po.setPlayerId(entry.getPlayerId());
            po.setDrawId(drawId);
            po.setSeed(entry.getSeed() != null ? entry.getSeed().shortValue() : null);
            po.setEntryType(entry.getEntryType());
            entries.add(po);
        }
        tennisTournamentEntryRepository.saveEntries(entries);
    }

    public void atpTournamentEntry(AtpDrawsResponse response, Long drawId) {
        AtpDrawsResponse.Draw draw = response.getMS();
        // key: playerId，value: TennisTournamentEntryPO
        Map<String, TennisTournamentEntryPO> entryMap = new LinkedHashMap<>();

        if (draw == null || CollectionUtils.isEmpty(draw.getRounds())) {
            return;
        }

        for (AtpDrawsResponse.Round round : draw.getRounds()) {
            if (CollectionUtils.isEmpty(round.getFixtures())) {
                continue;
            }
            for (AtpDrawsResponse.Fixture fixture : round.getFixtures()) {
                extractEntriesFromDrawLine(fixture.getDrawLineTop(), drawId, entryMap);
                extractEntriesFromDrawLine(fixture.getDrawLineBottom(), drawId, entryMap);
            }
        }

        tennisTournamentEntryRepository.saveEntries(new ArrayList<>(entryMap.values()));

    }

    private void extractEntriesFromDrawLine(
            AtpDrawsResponse.DrawLine drawLine, Long drawId,
            Map<String, TennisTournamentEntryPO> entryMap) {
        if (drawLine == null || CollectionUtils.isEmpty(drawLine.getPlayers())) {
            return;
        }

        for (AtpDrawsResponse.PlayerInfo playerInfo : drawLine.getPlayers()) {
            if (playerInfo == null || playerInfo.getPlayerId() == null) {
                continue;
            }
            TennisTournamentEntryPO entry = new TennisTournamentEntryPO();
            entry.setPlayerId(playerInfo.getPlayerId());
            entry.setDrawId(drawId);
            entry.setSeed(drawLine.getSeed() != null ? drawLine.getSeed().shortValue() : null);
            // 相同 playerId 覆盖，保留最新的种子数
            entryMap.put(playerInfo.getPlayerId(), entry);
        }
    }


}
