package com.rally.platformconfig.homecontentquery.activity;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.rally.domain.media.assetstorage.AssetStorageGateway;
import com.rally.domain.media.assetstorage.AssetStorageService;
import com.rally.domain.media.assetstorage.SignedReadOutcome;
import com.rally.domain.media.assetstorage.SignedReadResult;
import com.rally.domain.system.CityConfig;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.system.enums.SystemConfigKey;
import com.rally.home.model.DisplayType;
import com.rally.home.model.HomeDisplayItemDTO;
import com.rally.home.model.PosterCardDisplayData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 业务活动 query-home-poster-section：组装赛事、球场或自定义首页海报区块。
 */
@Slf4j
@Component
public class QueryHomePosterSectionActivity {

    private static final long URL_TTL_SECONDS = 3600L;

    private final AssetStorageService assetStorageService;

    public QueryHomePosterSectionActivity(AssetStorageGateway storageGateway) {
        this.assetStorageService = new AssetStorageService(storageGateway);
    }

    public HomeDisplayItemDTO execute(JSONObject section, String cityCode) {
        String type = section.getString("type");
        return switch (type) {
            case "TOURNAMENT_POSTER" -> buildTournamentPoster(section);
            case "COURT_POSTER" -> buildCourtPoster(section, cityCode);
            case "POSTER" -> buildCustomPoster(section, cityCode);
            default -> throw new IllegalArgumentException("不支持的首页海报区域类型: " + type);
        };
    }

    private HomeDisplayItemDTO buildTournamentPoster(JSONObject section) {
        // A1：赛事海报使用运行时对象配置；仅解析异常时回退枚举默认 JSON。
        JSONObject config = parseObjectConfig(SystemConfigKey.HOME_TOURNAMENT_POSTER_CONFIG);
        PosterCardDisplayData data = new PosterCardDisplayData();
        data.setTitle(configuredText(section, "title", config.getString("title")));
        data.setSubtitle(configuredText(section, "subtitle", config.getString("subtitle")));
        data.setPosters(buildPosterItems(config.getJSONArray("posters"), null));
        return displayItem(data);
    }

    private HomeDisplayItemDTO buildCourtPoster(JSONObject section, String cityCode) {
        // A1/A2：与 main 一致，城市名在构造默认副标题时立即解析；未知城市使整个区块失败。
        PosterCardDisplayData data = new PosterCardDisplayData();
        data.setTitle(configuredText(section, "title", "附近球场"));
        data.setSubtitle(configuredText(
                section,
                "subtitle",
                "寻找「" + CityConfig.getCityName(cityCode) + "」的球场"));
        data.setPosters(buildPosterItems(
                parseArrayConfig(SystemConfigKey.HOME_POSTER_CONFIG), cityCode));
        return displayItem(data);
    }

    private HomeDisplayItemDTO buildCustomPoster(JSONObject section, String cityCode) {
        // A1/A2：自定义区保留原始文案、数组和顺序，仅 cityAware=true 时附加城市参数。
        PosterCardDisplayData data = new PosterCardDisplayData();
        data.setTitle(section.getString("title"));
        data.setSubtitle(section.getString("subtitle"));
        String posterCityCode = Boolean.TRUE.equals(section.getBoolean("cityAware"))
                ? cityCode
                : null;
        data.setPosters(buildPosterItems(section.getJSONArray("posters"), posterCityCode));
        return displayItem(data);
    }

    private List<PosterCardDisplayData.PosterCardItem> buildPosterItems(
            JSONArray config,
            String cityCode) {
        List<PosterCardDisplayData.PosterCardItem> posters = new ArrayList<>();
        if (config == null) {
            return posters;
        }

        // A3：数组级保护保留 main 的部分成功语义；额外 JSON 字段自然忽略。
        try {
            for (int index = 0; index < config.size(); index++) {
                JSONObject posterJson = config.getJSONObject(index);
                PosterCardDisplayData.PosterCardItem poster =
                        new PosterCardDisplayData.PosterCardItem();
                poster.setType(PosterCardDisplayData.PosterType.valueOf(
                        posterJson.getString("type")));
                poster.setImageUrl(signImage(posterJson.getString("image")));
                poster.setTitle(posterJson.getString("title"));
                poster.setSubtitle(posterJson.getString("subtitle"));
                poster.setWechatUrl(cityAwareUrl(posterJson.getString("wechatUrl"), cityCode));
                poster.setAppUrl(cityAwareUrl(posterJson.getString("appUrl"), cityCode));
                poster.setWebUrl(cityAwareUrl(posterJson.getString("webUrl"), cityCode));
                posters.add(poster);
            }
        } catch (Exception exception) {
            log.error("解析首页海报配置失败", exception);
        }
        return posters;
    }

    private String signImage(String resourceKey) {
        SignedReadResult signedRead = assetStorageService.signReadUrl(
                resourceKey, URL_TTL_SECONDS);
        if (resourceKey == null || resourceKey.isBlank()) {
            return null;
        }
        if (signedRead.getOutcome() != SignedReadOutcome.SIGNED) {
            throw new IllegalStateException("签发首页海报图片地址失败");
        }
        return signedRead.getSignedUrl();
    }

    private String cityAwareUrl(String url, String cityCode) {
        if (cityCode == null || url == null || url.trim().isEmpty()) {
            return url;
        }
        String cityName = CityConfig.getCityName(cityCode);
        return url + "?cityCode=" + cityCode
                + "&cityName=" + cityName
                + "&mode=view";
    }

    private JSONObject parseObjectConfig(SystemConfigKey key) {
        try {
            return JSON.parseObject(SystemConfig.getString(key.getKey()));
        } catch (Exception exception) {
            log.error("解析首页配置失败 key={}", key.getKey(), exception);
            return JSON.parseObject(key.getDefaultValue());
        }
    }

    private JSONArray parseArrayConfig(SystemConfigKey key) {
        try {
            return JSON.parseArray(SystemConfig.getString(key.getKey()));
        } catch (Exception exception) {
            log.error("解析首页配置失败 key={}", key.getKey(), exception);
            return JSON.parseArray(key.getDefaultValue());
        }
    }

    private String configuredText(JSONObject section, String key, String fallback) {
        String value = section.getString(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private HomeDisplayItemDTO displayItem(PosterCardDisplayData data) {
        HomeDisplayItemDTO item = new HomeDisplayItemDTO();
        item.setDisplayType(DisplayType.POSTER_CARD);
        item.setData(data);
        return item;
    }
}
