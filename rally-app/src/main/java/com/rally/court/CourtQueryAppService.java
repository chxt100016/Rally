package com.rally.court;

import com.rally.court.activity.ListCityCourtsActivity;
import com.rally.court.activity.SearchCourtsByNameActivity;
import com.rally.domain.court.model.CourtDTO;
import com.rally.domain.court.model.CourtListCmd;
import com.rally.domain.court.model.CourtSearchCmd;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourtQueryAppService {

    private final ListCityCourtsActivity listCityCourtsActivity;
    private final SearchCourtsByNameActivity searchCourtsByNameActivity;

    public List<CourtDTO> getAll(CourtListCmd cmd) {
        return listCityCourtsActivity.execute(cmd);
    }

    public List<CourtDTO> search(CourtSearchCmd cmd) {
        return searchCourtsByNameActivity.execute(cmd);
    }
}
