package com.rally.domain.court.service;

import com.rally.domain.court.enums.CourtEnvironmentEnum;
import com.rally.domain.court.model.CourtPoi;
import com.rally.domain.court.model.CourtProfile;
import com.rally.domain.court.model.CourtProfileResolveResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 场所记录解析领域服务：从一条场所记录算出球场环境、标签、坐标与展示资料。
 * 只给出预检结论，运营随时能用球场编辑改掉；字段长度由球场聚合在存入时兜底。
 */
@Service
public class CourtProfileResolverService {

    /** R3 R7 楼顶类关键词 */
    private static final List<String> ROOFTOP_KEYWORDS = List.of("楼顶", "天台", "屋顶", "露台");
    /** R5 户外场所类关键词 */
    private static final List<String> OUTDOOR_PLACE_KEYWORDS = List.of("公园", "广场", "体育场");
    /** R7 公共设施类关键词 */
    private static final List<String> PUBLIC_FACILITY_KEYWORDS = List.of("公园", "体育中心", "体育公园");
    /** R4 三层及以上楼层写法 */
    private static final Pattern HIGH_FLOOR = Pattern.compile("[3-9][Ff]|[3-9]楼|[3-9]层");
    /** R7 高评分阈值 */
    private static final double HIGH_RATING_THRESHOLD = 4.6D;

    public CourtProfileResolveResult resolve(CourtPoi poi) {
        CourtProfileResolveResult result = new CourtProfileResolveResult();
        if (poi == null) {
            return result;
        }
        String name = StringUtils.defaultString(poi.getName());
        String address = StringUtils.defaultString(poi.getAddress());
        // R1 拼出判定文本
        String text = name + address + StringUtils.defaultString(poi.getPoiType())
                + StringUtils.defaultString(poi.getRectag()) + StringUtils.defaultString(poi.getKeytag());

        CourtEnvironmentEnum environment = resolveEnvironment(text, address);
        result.setEnvironment(environment);
        result.setTags(resolveTags(name, address, poi, environment));
        // R9 拆经纬度
        double[] coords = parseLocation(poi.getLocation());
        if (coords != null) {
            result.setLng(coords[0]);
            result.setLat(coords[1]);
        }
        result.setProfile(resolveProfile(poi));
        return result;
    }

    /** R2 R3 R4 R5 R6 球场环境判定，命中即止 */
    private CourtEnvironmentEnum resolveEnvironment(String text, String address) {
        // R2 含「室内」判为室内
        if (text.contains("室内")) {
            return CourtEnvironmentEnum.INDOOR;
        }
        // R3 含楼顶类关键词判为室外
        if (containsAny(text, ROOFTOP_KEYWORDS)) {
            return CourtEnvironmentEnum.OUTDOOR;
        }
        // R4 地址中出现三层及以上楼层判为室外
        if (StringUtils.isNotBlank(address) && HIGH_FLOOR.matcher(address).find()) {
            return CourtEnvironmentEnum.OUTDOOR;
        }
        // R5 含公园、广场、体育场判为室外
        if (containsAny(text, OUTDOOR_PLACE_KEYWORDS)) {
            return CourtEnvironmentEnum.OUTDOOR;
        }
        // R6 都不命中时为空
        return null;
    }

    /** R7 R8 标签生成，命中即按顺序追加，不去重不排序 */
    private List<String> resolveTags(String name, String address, CourtPoi poi, CourtEnvironmentEnum environment) {
        String combined = name + address;
        List<String> tags = new ArrayList<>();
        if (combined.contains("空调")) {
            tags.add("空调");
        }
        if (containsAny(combined, ROOFTOP_KEYWORDS)) {
            tags.add("楼顶");
        }
        if (name.contains("草地")) {
            tags.add("草地");
        }
        if (name.contains("红土")) {
            tags.add("红土");
        }
        if (name.contains("硬地") || name.contains("硬场")) {
            tags.add("硬地");
        }
        if (combined.contains("地铁") || address.contains("站")) {
            tags.add("近地铁");
        }
        if (combined.contains("停车") || name.toLowerCase().contains("parking")) {
            tags.add("停车方便");
        }
        if (containsAny(name, PUBLIC_FACILITY_KEYWORDS)) {
            tags.add("公共设施");
        }
        Double rating = parseRating(poi.getRating());
        if (rating != null && rating >= HIGH_RATING_THRESHOLD) {
            tags.add("高评分");
        }
        String opentime = StringUtils.defaultString(poi.getOpentime());
        if (opentime.contains("24小时") || opentime.contains("00:00-24:00")) {
            tags.add("24小时营业");
        }
        if (combined.contains("灯光") || combined.contains("夜间")) {
            tags.add("有灯光");
        }
        // R8 随球场环境结论追加室内场或室外场
        if (CourtEnvironmentEnum.INDOOR.equals(environment)) {
            tags.add("室内场");
        } else if (CourtEnvironmentEnum.OUTDOOR.equals(environment)) {
            tags.add("室外场");
        }
        return tags;
    }

    /** R10 展示资料逐项判定，空白项不放进来 */
    private CourtProfile resolveProfile(CourtPoi poi) {
        CourtProfile profile = new CourtProfile();
        profile.setRating(StringUtils.trimToNull(poi.getRating()));
        profile.setCost(StringUtils.trimToNull(poi.getCost()));
        profile.setOpentime(StringUtils.trimToNull(poi.getOpentime()));
        profile.setTel(StringUtils.trimToNull(poi.getTel()));
        return profile;
    }

    /** R9 经纬度串按逗号拆成经度与纬度，任一段解析不出则都给空 */
    private static double[] parseLocation(String location) {
        if (StringUtils.isBlank(location)) {
            return null;
        }
        String[] parts = location.split(",");
        if (parts.length != 2) {
            return null;
        }
        try {
            return new double[]{Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim())};
        } catch (Exception e) {
            return null;
        }
    }

    private static Double parseRating(String rating) {
        try {
            return Double.parseDouble(StringUtils.trimToEmpty(rating));
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean containsAny(String text, List<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
