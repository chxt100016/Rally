package com.rally.home;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.system.enums.SystemConfigKey;
import com.rally.home.model.HomeDisplayItemDTO;
import com.rally.home.model.HomePageDTO;
import com.rally.platformconfig.homecontentquery.activity.QueryHomeMeetupSectionActivity;
import com.rally.platformconfig.homecontentquery.activity.QueryHomeNewsSectionActivity;
import com.rally.platformconfig.homecontentquery.activity.QueryHomePosterSectionActivity;
import com.rally.platformconfig.homecontentquery.activity.QueryHomeTourSectionActivity;
import com.rally.platformconfig.homecontentquery.activity.QueryHomeTourSectionResult;
import com.rally.platformconfig.homecontentquery.activity.RegisterMissingTourTranslationsActivity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeAppService {

    private static final String DEFAULT_CITY_CODE = "330100";

    private final QueryHomeMeetupSectionActivity queryHomeMeetupSectionActivity;
    private final QueryHomePosterSectionActivity queryHomePosterSectionActivity;
    private final QueryHomeTourSectionActivity queryHomeTourSectionActivity;
    private final QueryHomeNewsSectionActivity queryHomeNewsSectionActivity;
    private final RegisterMissingTourTranslationsActivity registerMissingTourTranslationsActivity;

    public HomePageDTO getHomePage(String cityCode) {
        String effectiveCityCode = cityCode == null || cityCode.isBlank()
                ? DEFAULT_CITY_CODE
                : cityCode;
        List<HomeDisplayItemDTO> displayItems = new ArrayList<>();
        JSONArray sections = parseLayout();
        for (int index = 0; index < sections.size(); index++) {
            JSONObject section = sections.getJSONObject(index);
            if (section == null || Boolean.FALSE.equals(section.getBoolean("enabled"))) {
                continue;
            }
            try {
                HomeDisplayItemDTO item = querySection(section, effectiveCityCode);
                if (item != null) {
                    displayItems.add(item);
                }
            } catch (Exception exception) {
                log.error("构建首页区域失败 id={} type={}",
                        section.getString("id"), section.getString("type"), exception);
            }
        }
        HomePageDTO result = new HomePageDTO();
        result.setDisplayItems(displayItems);
        return result;
    }

    private HomeDisplayItemDTO querySection(JSONObject section, String cityCode) {
        String type = section.getString("type");
        if (type == null) {
            return null;
        }
        return switch (type) {
            case "MEETUP" -> queryHomeMeetupSectionActivity.execute(section);
            case "TOURNAMENT_POSTER", "COURT_POSTER", "POSTER" ->
                    queryHomePosterSectionActivity.execute(section, cityCode);
            case "TOUR_MATCH" -> queryTourSection(section);
            case "NEWS" -> queryHomeNewsSectionActivity.execute(section);
            default -> null;
        };
    }

    private HomeDisplayItemDTO queryTourSection(JSONObject section) {
        QueryHomeTourSectionResult result = queryHomeTourSectionActivity.execute(section);
        registerMissingTourTranslationsActivity.execute(result.missingTranslationKeys());
        return result.displayItem();
    }

    private JSONArray parseLayout() {
        try {
            return JSON.parseArray(SystemConfig.getString(
                    SystemConfigKey.HOME_LAYOUT_CONFIG.getKey()));
        } catch (Exception exception) {
            log.error("解析首页布局配置失败，使用默认配置", exception);
            return JSON.parseArray(SystemConfigKey.HOME_LAYOUT_CONFIG.getDefaultValue());
        }
    }
}
