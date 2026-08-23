package com.rally.home;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.rally.config.property.QiniuConfiguration;
import com.rally.domain.meetup.enums.UserMeetupTabEnum;
import com.rally.domain.meetup.model.MeetupCardDTO;
import com.rally.domain.meetup.model.PageDTO;
import com.rally.domain.meetup.model.UserMeetupListCmd;
import com.rally.domain.system.CityConfig;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.system.enums.SystemConfigKey;
import com.rally.domain.tour.TourMatchQueryDomainService;
import com.rally.domain.tour.TourTournamentQueryDomainService;
import com.rally.domain.tour.model.MatchGroupDTO;
import com.rally.domain.tour.model.MatchQueryVO;
import com.rally.domain.tour.model.TournamentData;
import com.rally.domain.tour.model.TournamentGroupData;
import com.rally.domain.translation.TranslationQueryService;
import com.rally.domain.translation.model.TranslationEntityTypeEnum;
import com.rally.domain.translation.model.TranslationKey;
import com.rally.domain.translation.model.TranslationLanguageEnum;
import com.rally.home.convert.HomeAppConvertMapper;
import com.rally.home.model.*;
import com.rally.meetup.UserMeetupAppService;
import com.rally.translation.TourTranslationService;
import com.rally.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeAppService {

    private static final String DEFAULT_CITY_CODE = "330100";

    private final TourTournamentQueryDomainService tourTournamentQueryDomainService;
    private final TourMatchQueryDomainService tourMatchQueryDomainService;
    private final UserMeetupAppService userMeetupAppService;
    private final TourTranslationService tourTranslationService;
    private final TranslationQueryService translationQueryService;
    private final QiniuConfiguration qiniuConfiguration;

    public HomePageDTO getHomePage(String cityCode) {
        HomePageDTO homePageDTO = new HomePageDTO();
        List<HomeDisplayItemDTO> displayItems = new ArrayList<>();

        displayItems.add(buildMeetupDisplay());

        displayItems.add(buildTournamentPosterDisplay());

        HomeDisplayItemDTO matchDisplay = buildMatchDisplay();
        if (matchDisplay != null) {
            displayItems.add(matchDisplay);
        }

        String effectiveCityCode = (cityCode == null || cityCode.trim().isEmpty()) ? DEFAULT_CITY_CODE : cityCode;
        displayItems.add(buildPosterDisplay(effectiveCityCode));


        displayItems.add(buildNewsDisplay());

        homePageDTO.setDisplayItems(displayItems);
        return homePageDTO;
    }

    private HomeDisplayItemDTO buildMeetupDisplay() {
        HomeDisplayItemDTO item = new HomeDisplayItemDTO();
        item.setDisplayType(DisplayType.MEETUP);
        MeetupDisplayData data = new MeetupDisplayData();
        data.setTitle("我的约球");
        data.setMeetups(queryInProgressMeetups());
        item.setData(data);
        return item;
    }

    private List<MeetupCardDTO> queryInProgressMeetups() {
        String userId = UserContext.getIfPresent();
        if (userId == null) {
            return new ArrayList<>();
        }
        UserMeetupListCmd cmd = new UserMeetupListCmd();
        cmd.setTab(UserMeetupTabEnum.IN_PROGRESS);
        PageDTO<MeetupCardDTO> page = userMeetupAppService.queryUserMeetupList(cmd);
        return page.getList();
    }

    private HomeDisplayItemDTO buildTournamentPosterDisplay() {
        HomeDisplayItemDTO item = new HomeDisplayItemDTO();
        item.setDisplayType(DisplayType.POSTER_CARD);
        PosterCardDisplayData data = new PosterCardDisplayData();
        JSONObject config = parseObjectConfig(SystemConfigKey.HOME_TOURNAMENT_POSTER_CONFIG);
        data.setTitle(config.getString("title"));
        data.setSubtitle(config.getString("subtitle"));
        data.setPosters(buildPosterItems(config.getJSONArray("posters"), null));
        item.setData(data);
        return item;
    }

    private HomeDisplayItemDTO buildMatchDisplay() {
        List<TournamentGroupData> tournamentGroups = tourTournamentQueryDomainService.findValidCurrentTournamentGroups(LocalDate.now());
        if (CollectionUtils.isEmpty(tournamentGroups)) {
            return null;
        }

        List<TournamentDisplayDTO> tournamentDisplays = new ArrayList<>();
        List<TournamentGroupData> displayedTournamentGroups = new ArrayList<>();
        for (TournamentGroupData tournamentGroup : tournamentGroups) {
            TournamentDisplayDTO tournamentDisplay = buildTournamentDisplay(tournamentGroup);
            if (tournamentDisplay != null) {
                tournamentDisplays.add(tournamentDisplay);
                displayedTournamentGroups.add(tournamentGroup);
            }
        }
        if (tournamentDisplays.isEmpty()) {
            return null;
        }
        translateTournamentDisplays(tournamentDisplays);

        HomeDisplayItemDTO item = new HomeDisplayItemDTO();
        item.setDisplayType(DisplayType.TOUR_MATCH);
        MatchDisplayData data = new MatchDisplayData();
        data.setTitle("巡回赛");
        data.setSubtitle(buildTourSubtitle(displayedTournamentGroups));
        data.setTournaments(tournamentDisplays);
        item.setData(data);
        return item;
    }

    private TournamentDisplayDTO buildTournamentDisplay(TournamentGroupData tournamentGroup) {
        List<String> tournamentIds = tournamentGroup.getTournamentIds();
        if (CollectionUtils.isEmpty(tournamentIds)) {
            return null;
        }
        List<MatchGroupDTO> dateGroups = tourMatchQueryDomainService.upcomingDateGroups(tournamentIds);
        if (CollectionUtils.isEmpty(dateGroups)) {
            return null;
        }

        MatchGroupDTO firstDateGroup = dateGroups.get(0);
        if (CollectionUtils.isEmpty(firstDateGroup.getChildren())) {
            return null;
        }

        MatchGroupDTO firstCourtGroup = firstDateGroup.getChildren().get(0);
        if (CollectionUtils.isEmpty(firstCourtGroup.getData())) {
            return null;
        }

        TournamentData representative = tournamentGroup.getRepresentative();
        TournamentDisplayDTO dto = HomeAppConvertMapper.INSTANCE.toTournamentDisplayDTO(representative);
        dto.setTour(joinTours(tournamentGroup.getTournaments()));
        dto.setCourtName(firstCourtGroup.getName());
        dto.setMatchDate(LocalDate.parse(firstDateGroup.getKey()));
        dto.setImagePath(QiniuConfiguration.buildSignedUrl(representative.getImagePath()));
        dto.setMatches(firstCourtGroup.getData());
        return dto;
    }

    private void translateTournamentDisplays(List<TournamentDisplayDTO> tournamentDisplays) {
        TranslationLanguageEnum language = TranslationLanguageEnum.ZH_CN;

        Map<TranslationKey, List<TournamentDisplayDTO>> nameMap = new HashMap<>();
        Map<TranslationKey, List<TournamentDisplayDTO>> courtMap = new HashMap<>();
        for (TournamentDisplayDTO dto : tournamentDisplays) {
            nameMap.computeIfAbsent(new TranslationKey(TranslationEntityTypeEnum.TOURNAMENT, dto.getTournamentName(), language), k -> new ArrayList<>()).add(dto);
            courtMap.computeIfAbsent(new TranslationKey(TranslationEntityTypeEnum.COURT, dto.getCourtName(), language), k -> new ArrayList<>()).add(dto);
        }

        Set<TranslationKey> allKeys = new HashSet<>(nameMap.keySet());
        allKeys.addAll(courtMap.keySet());
        Map<TranslationKey, String> translationMap = translationQueryService.query(allKeys);
        for (Map.Entry<TranslationKey, String> entry : translationMap.entrySet()) {
            switch (entry.getKey().getEntityType()) {
                case TOURNAMENT -> nameMap.getOrDefault(entry.getKey(), List.of()).forEach(dto -> dto.setTournamentName(entry.getValue()));
                case COURT -> courtMap.getOrDefault(entry.getKey(), List.of()).forEach(dto -> dto.setCourtName(entry.getValue()));
                default -> {}
            }
        }

        for (TournamentDisplayDTO dto : tournamentDisplays) {
            tourTranslationService.matches(dto.getMatches(), language);
        }
    }

    private String buildTourSubtitle(List<TournamentGroupData> tournamentGroups) {
        Map<String, Long> tourCountMap = tournamentGroups.stream()
                .flatMap(group -> group.getTournaments().stream())
                .map(TournamentData::getTour)
                .filter(tour -> tour != null && !tour.isBlank())
                .map(tour -> tour.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.groupingBy(tour -> tour, Collectors.counting()));
        long atpCount = tourCountMap.getOrDefault("ATP", 0L);
        long wtaCount = tourCountMap.getOrDefault("WTA", 0L);

        StringBuilder subtitle = new StringBuilder();
        if (atpCount > 0) {
            subtitle.append(atpCount).append("场ATP、");
        }
        if (wtaCount > 0) {
            subtitle.append(wtaCount).append("场WTA、");
        }
        if (subtitle.length() > 0) {
            subtitle.setLength(subtitle.length() - 1);
        }
        subtitle.append("进行中");
        return subtitle.toString();
    }

    private String joinTours(List<TournamentData> tournaments) {
        return tournaments.stream()
                .map(TournamentData::getTour)
                .filter(tour -> tour != null && !tour.isBlank())
                .map(tour -> tour.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .sorted((first, second) -> {
                    int order = Integer.compare(tourOrder(first), tourOrder(second));
                    return order != 0 ? order : first.compareTo(second);
                })
                .collect(Collectors.joining("/"));
    }

    private static int tourOrder(String tour) {
        return switch (tour) {
            case "ATP" -> 0;
            case "WTA" -> 1;
            default -> 2;
        };
    }

    private HomeDisplayItemDTO buildPosterDisplay(String cityCode) {
        HomeDisplayItemDTO item = new HomeDisplayItemDTO();
        item.setDisplayType(DisplayType.POSTER_CARD);
        PosterCardDisplayData data = new PosterCardDisplayData();
        data.setTitle("附近球场");
        data.setSubtitle("寻找「" + CityConfig.getCityName(cityCode) + "」的球场");

        JSONArray config = parseArrayConfig(SystemConfigKey.HOME_POSTER_CONFIG);
        data.setPosters(buildPosterItems(config, cityCode));

        item.setData(data);
        return item;
    }

    private List<PosterCardDisplayData.PosterCardItem> buildPosterItems(JSONArray config, String cityCode) {
        List<PosterCardDisplayData.PosterCardItem> posters = new ArrayList<>();
        if (config == null) {
            return posters;
        }
        try {
            for (int i = 0; i < config.size(); i++) {
                JSONObject posterJson = config.getJSONObject(i);
                PosterCardDisplayData.PosterCardItem poster = new PosterCardDisplayData.PosterCardItem();
                poster.setType(PosterCardDisplayData.PosterType.valueOf(posterJson.getString("type")));
                String imageKey = posterJson.getString("image");
                poster.setImageUrl(QiniuConfiguration.buildSignedUrl(imageKey));
                poster.setTitle(posterJson.getString("title"));
                poster.setSubtitle(posterJson.getString("subtitle"));
                poster.setWechatUrl(cityCode == null ? posterJson.getString("wechatUrl") : appendCityCode(posterJson.getString("wechatUrl"), cityCode));
                poster.setAppUrl(cityCode == null ? posterJson.getString("appUrl") : appendCityCode(posterJson.getString("appUrl"), cityCode));
                poster.setWebUrl(cityCode == null ? posterJson.getString("webUrl") : appendCityCode(posterJson.getString("webUrl"), cityCode));
                posters.add(poster);
            }
        } catch (Exception e) {
            log.error("解析首页海报配置失败", e);
        }
        return posters;
    }

    private JSONObject parseObjectConfig(SystemConfigKey key) {
        try {
            return JSON.parseObject(SystemConfig.getString(key.getKey()));
        } catch (Exception e) {
            log.error("解析首页配置失败 key={}", key.getKey(), e);
            return JSON.parseObject(key.getDefaultValue());
        }
    }

    private JSONArray parseArrayConfig(SystemConfigKey key) {
        try {
            return JSON.parseArray(SystemConfig.getString(key.getKey()));
        } catch (Exception e) {
            log.error("解析首页配置失败 key={}", key.getKey(), e);
            return JSON.parseArray(key.getDefaultValue());
        }
    }

    private String appendCityCode(String url, String cityCode) {
        if (url == null || url.trim().isEmpty()) {
            return url;
        }
        String cityName = CityConfig.getCityName(cityCode);
        return url + "?cityCode=" + cityCode + "&cityName=" + cityName + "&mode=view";
    }

    private HomeDisplayItemDTO buildNewsDisplay() {
        HomeDisplayItemDTO item = new HomeDisplayItemDTO();
        item.setDisplayType(DisplayType.NEWS_TIMELINE);
        NewsTimelineDisplayData data = new NewsTimelineDisplayData();
        data.setTitle("资讯");
        data.setSubtitle("最新动态");
        data.setNewsItems(new ArrayList<>());
        item.setData(data);
        return item;
    }
}
