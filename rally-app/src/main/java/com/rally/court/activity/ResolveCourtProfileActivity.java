package com.rally.court.activity;

import com.rally.court.model.ResolvedCourts;
import com.rally.domain.court.model.CourtCollectCmd;
import com.rally.domain.court.model.CourtLocation;
import com.rally.domain.court.model.CourtPoi;
import com.rally.domain.court.model.CourtPoiCluster;
import com.rally.domain.court.model.CourtProfileResolveResult;
import com.rally.domain.court.service.CourtProfileResolverService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 业务活动 resolve-court-profile：把主记录逐条解析成可以写库的球场资料。
 */
@Component
@RequiredArgsConstructor
public class ResolveCourtProfileActivity {

    private final CourtProfileResolverService courtProfileResolverService;

    public ResolvedCourts execute(List<CourtPoiCluster> clusters, String cityCode, String cityName) {
        List<CourtCollectCmd> courts = new ArrayList<>();
        for (CourtPoiCluster cluster : clusters) {
            CourtPoi keeper = cluster.getKeeper();
            if (keeper == null || StringUtils.isBlank(keeper.getId()) || StringUtils.isBlank(keeper.getName())) {
                continue;
            }
            // A1 逐条把主记录交给解析规则，取回球场环境、标签、经纬度与展示资料
            CourtProfileResolveResult resolved = courtProfileResolverService.resolve(keeper);

            CourtCollectCmd cmd = new CourtCollectCmd();
            cmd.setSourceId(keeper.getId());
            cmd.setName(keeper.getName());
            cmd.setAddress(keeper.getAddress());
            cmd.setLng(resolved.getLng());
            cmd.setLat(resolved.getLat());
            cmd.setType(resolved.getEnvironment());
            cmd.setTags(resolved.getTags());
            cmd.setProfile(resolved.getProfile());
            // A2 城市归属一律以本次抓取的城市为准，区域归属取主记录自带的区划编码与名称
            cmd.setLocation(locationOf(cityCode, cityName, keeper));
            // A3 该主记录被并掉的记录名称作为球场别名，没有被并掉的则留空
            cmd.setAlias(aliasOf(cluster));
            courts.add(cmd);
        }
        // A4 汇成待写入的球场资料清单与有效条数返回
        ResolvedCourts result = new ResolvedCourts();
        result.setCourts(courts);
        result.setValidCount(courts.size());
        return result;
    }

    private CourtLocation locationOf(String cityCode, String cityName, CourtPoi keeper) {
        String districtCode = StringUtils.trimToNull(keeper.getAdcode());
        String districtName = StringUtils.trimToNull(keeper.getAdname());
        // 区域编码与名称必须成对，缺一则两者都留空
        if (districtCode == null || districtName == null) {
            return new CourtLocation(cityCode, cityName, null, null);
        }
        return new CourtLocation(cityCode, cityName, districtCode, districtName);
    }

    private List<String> aliasOf(CourtPoiCluster cluster) {
        if (cluster.getMerged() == null || cluster.getMerged().isEmpty()) {
            return List.of();
        }
        return cluster.getMerged().stream().map(CourtPoi::getName).filter(Objects::nonNull).toList();
    }
}
