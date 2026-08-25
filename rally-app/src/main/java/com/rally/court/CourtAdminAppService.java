package com.rally.court;

import com.rally.court.activity.CreateManualCourtActivity;
import com.rally.court.activity.CreateResolveCourtLocationActivity;
import com.rally.court.activity.DisableCourtActivity;
import com.rally.court.activity.FetchAmapCourtsActivity;
import com.rally.court.activity.ResolveCollectScopeActivity;
import com.rally.court.activity.ResolveCourtProfileActivity;
import com.rally.court.activity.ScreenAndMergePoisActivity;
import com.rally.court.activity.UpdateCourtProfileActivity;
import com.rally.court.activity.UpdateResolveCourtLocationActivity;
import com.rally.court.activity.UpsertCityCourtsActivity;
import com.rally.court.convert.CourtAdminAppConvertMapper;
import com.rally.court.model.CollectScope;
import com.rally.court.model.FetchedPois;
import com.rally.court.model.ResolvedCourts;
import com.rally.court.model.UpsertResult;
import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.court.model.CourtCollectApiCmd;
import com.rally.domain.court.model.CourtCollectResultDTO;
import com.rally.domain.court.model.CourtCreateApiCmd;
import com.rally.domain.court.model.CourtCreateCmd;
import com.rally.domain.court.model.CourtDisableApiCmd;
import com.rally.domain.court.model.CourtIdDTO;
import com.rally.domain.court.model.CourtLocation;
import com.rally.domain.court.model.CourtPoiScreenResult;
import com.rally.domain.court.model.CourtUpdateApiCmd;
import com.rally.domain.court.model.CourtUpdateCmd;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 球场后台运营应用服务：抓取收录、新增、编辑、停用四条流程的编排。
 */
@Service
@RequiredArgsConstructor
public class CourtAdminAppService {

    /** 同一城市同时只允许一份抓取在跑 */
    private static final Set<String> COLLECTING_CITIES = ConcurrentHashMap.newKeySet();

    private final ResolveCollectScopeActivity resolveCollectScopeActivity;
    private final FetchAmapCourtsActivity fetchAmapCourtsActivity;
    private final ScreenAndMergePoisActivity screenAndMergePoisActivity;
    private final ResolveCourtProfileActivity resolveCourtProfileActivity;
    private final UpsertCityCourtsActivity upsertCityCourtsActivity;
    private final CreateResolveCourtLocationActivity createResolveCourtLocationActivity;
    private final CreateManualCourtActivity createManualCourtActivity;
    private final UpdateResolveCourtLocationActivity updateResolveCourtLocationActivity;
    private final UpdateCourtProfileActivity updateCourtProfileActivity;
    private final DisableCourtActivity disableCourtActivity;

    /** 流程 collect-city-courts */
    public CourtCollectResultDTO collect(CourtCollectApiCmd cmd) {
        String cityCode = cmd.getCityCode();
        // 同城市互斥，前一份还没结束时后到的请求直接拒绝
        Assert.isTrue(COLLECTING_CITIES.add(cityCode), BizErrorCode.COURT_COLLECT_IN_PROGRESS);
        try {
            CollectScope scope = resolveCollectScopeActivity.execute(cityCode);
            FetchedPois fetched = fetchAmapCourtsActivity.execute(scope.getRegionCodes());
            CourtPoiScreenResult screened = screenAndMergePoisActivity.execute(fetched.getPois());
            ResolvedCourts resolved = resolveCourtProfileActivity.execute(screened.getClusters(), scope.getCityCode(), scope.getCityName());
            UpsertResult upsert = upsertCityCourtsActivity.execute(resolved.getCourts(), cmd.getMode());

            CourtCollectResultDTO result = new CourtCollectResultDTO();
            result.setCityCode(scope.getCityCode());
            result.setCityName(scope.getCityName());
            result.setMode(cmd.getMode());
            result.setDistrictCount(scope.getDistrictCount());
            result.setFetchedCount(fetched.getFetchedCount());
            result.setFilteredCount(screenAndMergePoisActivity.filteredCountOf(screened));
            result.setValidCount(resolved.getValidCount());
            result.setInsertedCount(upsert.getInsertedCount());
            result.setUpdatedCount(upsert.getUpdatedCount());
            result.setSkippedCount(upsert.getSkippedCount());
            result.setFailedDistricts(fetched.getFailedRegions());
            return result;
        } finally {
            COLLECTING_CITIES.remove(cityCode);
        }
    }

    /** 流程 create-court */
    public CourtIdDTO create(CourtCreateApiCmd apiCmd) {
        CourtLocation location = createResolveCourtLocationActivity.execute(apiCmd.getCityCode(), apiCmd.getDistrictCode());
        CourtCreateCmd cmd = CourtAdminAppConvertMapper.INSTANCE.toCreateCmd(apiCmd);
        cmd.setLocation(location);
        return new CourtIdDTO(createManualCourtActivity.execute(cmd));
    }

    /** 流程 update-court */
    public void update(CourtUpdateApiCmd apiCmd) {
        CourtLocation location = updateResolveCourtLocationActivity.execute(apiCmd.getCityCode(), apiCmd.getDistrictCode());
        CourtUpdateCmd cmd = CourtAdminAppConvertMapper.INSTANCE.toUpdateCmd(apiCmd);
        cmd.setLocation(location);
        updateCourtProfileActivity.execute(cmd);
    }

    /** 流程 disable-court */
    public void disable(CourtDisableApiCmd cmd) {
        disableCourtActivity.execute(cmd.getCourtId());
    }
}
