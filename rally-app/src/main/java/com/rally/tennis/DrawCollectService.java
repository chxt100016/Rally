package com.rally.tennis;

import com.rally.client.tennistv.model.AtpDrawsResponse;
import com.rally.client.wta.model.WtaDrawsResponse;
import com.rally.db.tennis.repository.TennisDrawRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class DrawCollectService {

    @Resource
    private TennisDrawRepository tennisDrawRepository;


    public Long atp(AtpDrawsResponse response, String tournamentId, int year) {
        if (response.getMS() != null && CollectionUtils.isNotEmpty(response.getMS().getRounds())) {
            // 先创建 draw 记录，获取 drawId
            AtpDrawsResponse.Draw msDraw = response.getMS();
            Integer totalRounds = msDraw.getRounds() != null ? msDraw.getRounds().size() : 0;
            return tennisDrawRepository.saveOrUpdate(tournamentId, year, "MS", msDraw.getDrawSize(), totalRounds);
        }
        return null;
    }

    public Long wta(WtaDrawsResponse response, String tournamentId, int year) {
        if (response == null || response.getData() == null
                || CollectionUtils.isEmpty(response.getData().getResults())) {
            return null;
        }
        WtaDrawsResponse.DrawData data = response.getData();
        Integer drawSize = data.getEvent() != null ? data.getEvent().getSglDrawSize() : null;
        Integer totalRounds = data.getResults().size();
        return tennisDrawRepository.saveOrUpdate(tournamentId, year, "LS", drawSize, totalRounds);
    }



}
