package com.rally.tour;

import com.rally.client.tourtv.model.AtpDrawsResponse;
import com.rally.client.wta.model.WtaDrawsResponse;
import com.rally.domain.tour.repository.TourDrawRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DrawCollectService {

    @Resource
    private TourDrawRepository tourDrawRepository;

    public Long atp(AtpDrawsResponse response, String tournamentId, int year) {
        if (response.getMS() != null && CollectionUtils.isNotEmpty(response.getMS().getRounds())) {
            AtpDrawsResponse.Draw msDraw = response.getMS();
            Integer totalRounds = msDraw.getRounds() != null ? msDraw.getRounds().size() : 0;
            return tourDrawRepository.saveOrUpdate(tournamentId, year, "MS", msDraw.getDrawSize(), totalRounds);
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
        return tourDrawRepository.saveOrUpdate(tournamentId, year, "LS", drawSize, totalRounds);
    }

    public Long saveOrUpdate(String tournamentId, int year, String drawTypeCode, Integer drawSize, Integer totalRounds) {
        return tourDrawRepository.saveOrUpdate(tournamentId, year, drawTypeCode, drawSize, totalRounds);
    }
}
