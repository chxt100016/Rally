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
import com.rally.domain.translation.TranslationQueryService;
import com.rally.domain.translation.model.TranslationEntityTypeEnum;
import com.rally.domain.translation.model.TranslationKey;
import com.rally.domain.translation.model.TranslationLanguageEnum;
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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeAppService {

    private final TourTournamentQueryDomainService tourTournamentQueryDomainService;
    private final TourMatchQueryDomainService tourMatchQueryDomainService;
    private final UserMeetupAppService userMeetupAppService;
    private final TourTranslationService tourTranslationService;
    private final TranslationQueryService translationQueryService;

    public HomePageDTO getHomePage(String cityCode) {
        HomePageDTO homePageDTO = new HomePageDTO();
        List<HomeDisplayItemDTO> displayItems = new ArrayList<>();

        displayItems.add(buildMeetupDisplay());

        HomeDisplayItemDTO matchDisplay = buildMatchDisplay();
        if (matchDisplay != null) {
            displayItems.add(matchDisplay);
        }

        if (cityCode != null && !cityCode.trim().isEmpty()) {
            displayItems.add(buildPosterDisplay(cityCode));
        }


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

    private HomeDisplayItemDTO buildMatchDisplay() {
        List<TournamentData> tournaments = tourTournamentQueryDomainService.findValidCurrentTournaments(LocalDate.now());
        if (CollectionUtils.isEmpty(tournaments)) {
            return null;
        }

        List<TournamentDisplayDTO> tournamentDisplays = new ArrayList<>();
        for (TournamentData tournament : tournaments) {
            TournamentDisplayDTO tournamentDisplay = buildTournamentDisplay(tournament);
            if (tournamentDisplay != null) {
                tournamentDisplays.add(tournamentDisplay);
            }
        }
        if (tournamentDisplays.isEmpty()) {
            return null;
        }
        translateTournamentDisplays(tournamentDisplays);

        HomeDisplayItemDTO item = new HomeDisplayItemDTO();
        item.setDisplayType(DisplayType.TOUR_MATCH);
        MatchDisplayData data = new MatchDisplayData();
        data.setTitle("网球赛事");
        data.setSubtitle(buildTourSubtitle(tournamentDisplays));
        data.setTournaments(tournamentDisplays);
        item.setData(data);
        return item;
    }

    private TournamentDisplayDTO buildTournamentDisplay(TournamentData tournament) {
        List<String> tournamentIds = List.of(tournament.getTournamentId());
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

        TournamentDisplayDTO dto = new TournamentDisplayDTO();
        dto.setTournamentId(tournament.getTournamentId());
        dto.setTournamentName(tournament.getName());
        dto.setCategory(tournament.getCategory());
        dto.setTour(tournament.getTour());
        dto.setCourtName(firstCourtGroup.getName());
        dto.setMatchDate(LocalDate.parse(firstDateGroup.getKey()));
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

    private String buildTourSubtitle(List<TournamentDisplayDTO> tournamentDisplays) {
        Map<String, Long> tourCountMap = tournamentDisplays.stream()
                .filter(t -> t.getTour() != null)
                .collect(Collectors.groupingBy(TournamentDisplayDTO::getTour, Collectors.counting()));
        long atpCount = tourCountMap.getOrDefault("ATP", 0L);
        long wtaCount = tourCountMap.getOrDefault("WTA", 0L);
        return atpCount + "场ATP、" + wtaCount + "场WTA";
    }

    private HomeDisplayItemDTO buildPosterDisplay(String cityCode) {
        HomeDisplayItemDTO item = new HomeDisplayItemDTO();
        item.setDisplayType(DisplayType.POSTER_CARD);
        PosterCardDisplayData data = new PosterCardDisplayData();
        data.setTitle("附近球场");
        data.setSubtitle("寻找当前城市的球场");

        List<PosterCardDisplayData.PosterCardItem> posters = new ArrayList<>();
        String configJson = SystemConfig.getString(SystemConfigKey.HOME_POSTER_CONFIG.getKey());
        try {
            JSONArray jsonArray = JSON.parseArray(configJson);
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject posterJson = jsonArray.getJSONObject(i);
                PosterCardDisplayData.PosterCardItem poster = new PosterCardDisplayData.PosterCardItem();
                poster.setType(PosterCardDisplayData.PosterType.valueOf(posterJson.getString("type")));
                String imageKey = posterJson.getString("image");
                poster.setImageUrl(QiniuConfiguration.buildSignedUrl(imageKey));
                poster.setTitle(posterJson.getString("title"));
                poster.setSubtitle(posterJson.getString("subtitle"));
                poster.setWechatUrl(appendCityCode(posterJson.getString("wechatUrl"), cityCode));
                poster.setAppUrl(appendCityCode(posterJson.getString("appUrl"), cityCode));
                poster.setWebUrl(appendCityCode(posterJson.getString("webUrl"), cityCode));
                posters.add(poster);
            }
        } catch (Exception e) {
            log.error("解析首页海报配置失败", e);
        }
        data.setPosters(posters);

        item.setData(data);
        return item;
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
