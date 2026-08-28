package com.rally.platformconfig.homecontentquery.activity;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.rally.domain.media.assetstorage.AssetStorageGateway;
import com.rally.domain.media.assetstorage.AssetStorageService;
import com.rally.domain.media.assetstorage.SignedReadOutcome;
import com.rally.domain.media.assetstorage.SignedReadResult;
import com.rally.domain.system.CityConfig;
import com.rally.home.model.DisplayType;
import com.rally.home.model.HomeDisplayItemDTO;
import com.rally.home.model.PosterCardDisplayData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 业务活动 query-home-poster-section：按城市可见性组装统一首页海报区块。
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
        if (!"POSTER".equals(type)) {
            throw new IllegalArgumentException("不支持的首页海报区域类型: " + type);
        }

        PosterCardDisplayData data = new PosterCardDisplayData();
        data.setTitle(section.getString("title"));
        data.setSubtitle(section.getString("subtitle"));
        data.setPosters(buildPosterItems(section.getJSONArray("posters"), cityCode));
        return displayItem(data);
    }

    private List<PosterCardDisplayData.PosterCardItem> buildPosterItems(
            JSONArray config,
            String cityCode) {
        List<PosterCardDisplayData.PosterCardItem> posters = new ArrayList<>();
        if (config == null) {
            return posters;
        }

        for (int index = 0; index < config.size(); index++) {
            JSONObject posterJson = config.getJSONObject(index);

            // A1：全城市海报直接保留；配置了非空 cityId 时按原字符串精确匹配。
            if (!isVisible(posterJson, cityCode)) {
                continue;
            }

            try {
                // A2：先校验并替换三个导航地址；未知城市仅省略当前海报。
                String[] navigationUrls = resolveNavigationUrls(posterJson, cityCode);
                if (navigationUrls == null) {
                    log.warn("首页海报城市不可用，省略当前海报 cityCode={} index={}",
                            cityCode, index);
                    continue;
                }

                // A3：导航处理完成后才转换交互和签名图片。
                PosterCardDisplayData.PosterCardItem poster =
                        new PosterCardDisplayData.PosterCardItem();
                poster.setType(PosterCardDisplayData.PosterType.valueOf(
                        posterJson.getString("actionType")));
                poster.setTitle(posterJson.getString("title"));
                poster.setSubtitle(posterJson.getString("subtitle"));
                poster.setWechatUrl(navigationUrls[0]);
                poster.setAppUrl(navigationUrls[1]);
                poster.setWebUrl(navigationUrls[2]);
                poster.setImageUrl(signImage(posterJson.getString("image")));
                posters.add(poster);
            } catch (Exception exception) {
                log.error("转换首页海报配置失败 index={}", index, exception);
                break;
            }
        }
        return posters;
    }

    private boolean isVisible(JSONObject posterJson, String cityCode) {
        if (posterJson == null) {
            return true;
        }
        String configuredCityId = posterJson.getString("cityId");
        return configuredCityId == null
                || configuredCityId.trim().isEmpty()
                || configuredCityId.equals(cityCode);
    }

    private String signImage(String resourceKey) {
        if (resourceKey == null || resourceKey.isBlank()) {
            return null;
        }
        SignedReadResult signedRead = assetStorageService.signReadUrl(
                resourceKey, URL_TTL_SECONDS);
        if (signedRead.getOutcome() != SignedReadOutcome.SIGNED) {
            throw new IllegalStateException("签发首页海报图片地址失败");
        }
        return signedRead.getSignedUrl();
    }

    private String[] resolveNavigationUrls(JSONObject posterJson, String cityCode) {
        String[] urls = {
                posterJson.getString("wechatUrl"),
                posterJson.getString("appUrl"),
                posterJson.getString("webUrl")
        };
        boolean needsCityName = false;
        for (String url : urls) {
            needsCityName |= validateTemplate(url);
        }

        String cityName = null;
        if (needsCityName) {
            cityName = cityNameOrNull(cityCode);
            if (cityName == null) {
                return null;
            }
        }

        for (int index = 0; index < urls.length; index++) {
            if (isNotBlank(urls[index])) {
                urls[index] = urls[index].replace("{{cityCode}}", cityCode);
                if (needsCityName) {
                    urls[index] = urls[index].replace("{{cityName}}", cityName);
                }
            }
        }
        return urls;
    }

    /**
     * 校验单个导航模板，并返回其是否使用城市名占位符。
     */
    private boolean validateTemplate(String url) {
        if (!isNotBlank(url)) {
            return false;
        }
        boolean needsCityName = false;
        int cursor = 0;
        while (cursor < url.length()) {
            int opening = url.indexOf("{{", cursor);
            int closing = url.indexOf("}}", cursor);
            if (closing >= 0 && (opening < 0 || closing < opening)) {
                throw new IllegalArgumentException("导航地址包含无配对的占位符结束标记");
            }
            if (opening < 0) {
                return needsCityName;
            }
            if (closing < 0) {
                throw new IllegalArgumentException("导航地址包含未闭合的占位符");
            }

            String placeholder = url.substring(opening + 2, closing);
            if ("cityName".equals(placeholder)) {
                needsCityName = true;
            } else if (!"cityCode".equals(placeholder)) {
                throw new IllegalArgumentException("导航地址包含未登记或空白占位符");
            }
            cursor = closing + 2;
        }
        return needsCityName;
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String cityNameOrNull(String cityCode) {
        try {
            String cityName = CityConfig.getCityName(cityCode);
            return isNotBlank(cityName) ? cityName : null;
        } catch (RuntimeException exception) {
            log.warn("查询首页海报城市名称失败 cityCode={}", cityCode, exception);
            return null;
        }
    }

    private HomeDisplayItemDTO displayItem(PosterCardDisplayData data) {
        HomeDisplayItemDTO item = new HomeDisplayItemDTO();
        item.setDisplayType(DisplayType.POSTER_CARD);
        item.setData(data);
        return item;
    }
}
