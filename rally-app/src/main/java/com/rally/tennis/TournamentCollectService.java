package com.rally.tennis;

import com.rally.client.atp.AtpClient;
import com.rally.client.atp.model.AtpTournamentsResponse;
import com.rally.client.wta.WtaClient;
import com.rally.client.wta.model.WtaTournamentsResponse;
import com.rally.db.tennis.entity.TennisTournamentEntryPO;
import com.rally.db.tennis.entity.TennisTournamentPO;
import com.rally.db.tennis.repository.TennisTournamentEntryRepository;
import com.rally.db.tennis.repository.TennisTournamentRepository;
import com.rally.tennis.convert.AtpTournamentAppConvertMapper;
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
    private AtpClient atpClient;

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
        AtpTournamentsResponse response = atpClient.getTournaments(year);
        if (response == null || CollectionUtils.isEmpty(response.getTournamentDates())) {
            log.warn("从ATP官网获取赛事列表为空, year={}", year);
            return;
        }
        // 将所有月份分组的赛事展平为一个列表
        List<Tournament> tournaments = response.getTournamentDates().stream()
                .filter(d -> d.getTournaments() != null)
                .flatMap(d -> d.getTournaments().stream())
                .map(AtpTournamentAppConvertMapper.INSTANCE::toTournament)
                .peek(t -> t.setYear(year))
                .toList();
        tennisTournamentRepository.saveOrUpdateBatch(AtpTournamentAppConvertMapper.INSTANCE.toTournamentPOList(tournaments));
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




}
