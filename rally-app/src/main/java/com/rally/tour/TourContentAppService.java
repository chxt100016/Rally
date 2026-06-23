package com.rally.tour;

import com.rally.domain.tour.gateway.MatchQueryGateway;
import com.rally.domain.tour.gateway.TourDrawGateway;
import com.rally.domain.tour.gateway.TourEntryGateway;
import com.rally.domain.tour.gateway.TourPlayerGateway;
import com.rally.domain.tour.gateway.TourTournamentGateway;
import com.rally.domain.tour.model.MatchData;
import com.rally.domain.tour.model.PlayerData;
import com.rally.domain.tour.model.TourDrawData;
import com.rally.domain.tour.model.TournamentData;
import com.rally.domain.tour.model.TournamentEntryData;
import com.rally.domain.translation.model.TranslationEntityTypeEnum;
import com.rally.domain.translation.model.TranslationKey;
import com.rally.domain.translation.model.TranslationLanguageEnum;
import com.rally.translation.TourTranslationService;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TourContentAppService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_DAY_FMT = DateTimeFormatter.ofPattern("M月d日");

    @Resource
    private TourTournamentGateway tourTournamentGateway;

    @Resource
    private TourDrawGateway tourDrawGateway;

    @Resource
    private MatchQueryGateway matchQueryGateway;

    @Resource
    private TourPlayerGateway tourPlayerGateway;

    @Resource
    private TourEntryGateway tourEntryGateway;

    @Resource
    private TourTranslationService tourTranslationService;

    public String generateDailyContent(LocalDate date, TranslationLanguageEnum language) {
        List<TournamentData> tournaments = tourTournamentGateway.findCurrentTournaments(date);
        if (CollectionUtils.isEmpty(tournaments)) {
            return "";
        }

        tournaments = tournaments.stream()
                .filter(this::isCategoryKept)
                .toList();
        if (CollectionUtils.isEmpty(tournaments)) {
            return "";
        }

        List<String> tournamentIds = tournaments.stream()
                .map(TournamentData::getTournamentId)
                .toList();
        List<MatchData> matches = matchQueryGateway.findByTournamentIdsAndDate(tournamentIds, date);
        if (CollectionUtils.isEmpty(matches)) {
            return "";
        }

        List<Long> drawIds = matches.stream()
                .map(MatchData::getDrawId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<String, Short> seedMap = tourEntryGateway.listSeedMapByDrawIds(drawIds);

        Set<String> playerIds = new HashSet<>();
        for (MatchData match : matches) {
            if (match.getPlayer1Id() != null) playerIds.add(match.getPlayer1Id());
            if (match.getPlayer2Id() != null) playerIds.add(match.getPlayer2Id());
        }
        List<PlayerData> players = tourPlayerGateway.listByPlayerIds(new ArrayList<>(playerIds));
        Map<String, PlayerData> playerMap = players.stream()
                .collect(Collectors.toMap(PlayerData::getPlayerId, p -> p, (a, b) -> a));

        Map<String, TournamentData> tournamentMap = tournaments.stream()
                .collect(Collectors.toMap(TournamentData::getTournamentId, t -> t, (a, b) -> a));

        Map<String, String> translations = collectAndTranslate(tournaments, matches, players, language);

        Map<String, List<MatchData>> matchesByTournament = matches.stream()
                .collect(Collectors.groupingBy(MatchData::getTournamentId));

        List<List<TournamentData>> tournamentGroups = groupByCityAndTime(tournaments);

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (List<TournamentData> group : tournamentGroups) {
            List<MatchData> groupMatches = new ArrayList<>();
            for (TournamentData tournament : group) {
                List<MatchData> tournamentMatches = matchesByTournament.get(tournament.getTournamentId());
                if (CollectionUtils.isNotEmpty(tournamentMatches)) {
                    groupMatches.addAll(tournamentMatches);
                }
            }
            if (CollectionUtils.isEmpty(groupMatches)) continue;

            if (!first) {
                sb.append("\n\n");
            }
            sb.append(buildTournamentMd(group.get(0), groupMatches, playerMap, seedMap, translations, date));
            first = false;
        }

        this.image(sb);
        return sb.toString();
    }

    public String generateSeedListContent(List<String> tournamentIds, TranslationLanguageEnum language) {
        if (CollectionUtils.isEmpty(tournamentIds)) {
            return "";
        }

        List<TournamentData> tournaments = tourTournamentGateway.listByTournamentIds(tournamentIds);
        if (CollectionUtils.isEmpty(tournaments)) {
            return "";
        }

        tournaments = tournaments.stream().filter(this::isCategoryKept).toList();
        if (CollectionUtils.isEmpty(tournaments)) {
            return "";
        }

        List<TourDrawData> draws = tourDrawGateway.listByTournamentIds(tournamentIds);
        if (CollectionUtils.isEmpty(draws)) {
            return "";
        }

        List<Long> allDrawIds = draws.stream().map(TourDrawData::getId).toList();
        List<TournamentEntryData> allEntries = tourEntryGateway.listByDrawIds(allDrawIds);
        List<TournamentEntryData> seededEntries = allEntries.stream()
                .filter(e -> e.getSeed() != null && e.getSeed() > 0)
                .toList();
        if (CollectionUtils.isEmpty(seededEntries)) {
            return "";
        }

        Set<String> playerIds = seededEntries.stream()
                .map(TournamentEntryData::getPlayerId)
                .collect(Collectors.toSet());
        List<PlayerData> players = tourPlayerGateway.listByPlayerIds(new ArrayList<>(playerIds));
        Map<String, PlayerData> playerMap = players.stream()
                .collect(Collectors.toMap(PlayerData::getPlayerId, p -> p, (a, b) -> a));

        Map<String, String> translations = collectSeedTranslations(tournaments, players, language);

        Map<String, TournamentData> tournamentMap = tournaments.stream()
                .collect(Collectors.toMap(TournamentData::getTournamentId, t -> t, (a, b) -> a));

        Map<Long, String> drawToTournamentMap = new HashMap<>();
        for (TourDrawData draw : draws) {
            drawToTournamentMap.put(draw.getId(), draw.getTournamentId());
        }

        Map<String, List<TournamentEntryData>> seedsByTournament = new LinkedHashMap<>();
        for (TournamentEntryData entry : seededEntries) {
            String tid = drawToTournamentMap.get(entry.getDrawId());
            if (tid != null) {
                seedsByTournament.computeIfAbsent(tid, k -> new ArrayList<>()).add(entry);
            }
        }

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String tid : seedsByTournament.keySet()) {
            TournamentData tournament = tournamentMap.get(tid);
            if (tournament == null) continue;

            List<TournamentEntryData> entries = seedsByTournament.get(tid);
            if (CollectionUtils.isEmpty(entries)) continue;

            if (!first) {
                sb.append("\n\n");
            }

            sb.append(buildTournamentSeedMd(tournament, entries, playerMap, translations));
            first = false;
        }

        return sb.toString();
    }

    private Map<String, String> collectSeedTranslations(List<TournamentData> tournaments, List<PlayerData> players, TranslationLanguageEnum language) {
        Set<TranslationKey> keys = new HashSet<>();
        for (TournamentData t : tournaments) {
            if (t.getName() != null) {
                keys.add(new TranslationKey(TranslationEntityTypeEnum.TOURNAMENT, t.getName(), language));
            }
            if (t.getCity() != null) {
                keys.add(new TranslationKey(TranslationEntityTypeEnum.CITY, t.getCity(), language));
            }
        }
        for (PlayerData p : players) {
            String name = p.getLastName() != null ? p.getLastName() : p.getFirstName();
            if (name != null) {
                keys.add(new TranslationKey(TranslationEntityTypeEnum.PLAYER, name, language));
            }
        }
        Map<TranslationKey, String> translationMap = tourTranslationService.translate(keys, language);
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<TranslationKey, String> entry : translationMap.entrySet()) {
            TranslationKey key = entry.getKey();
            result.put(key.getEntityType() + ":" + key.getOriginalText(), entry.getValue());
        }
        return result;
    }

    private String buildTournamentSeedMd(TournamentData tournament, List<TournamentEntryData> entries, Map<String, PlayerData> playerMap, Map<String, String> translations) {
        StringBuilder sb = new StringBuilder();

        String tournamentName = translations.getOrDefault(TranslationEntityTypeEnum.TOURNAMENT + ":" + tournament.getName(), tournament.getName());
        String city = translations.getOrDefault(TranslationEntityTypeEnum.CITY + ":" + tournament.getCity(), tournament.getCity());
        String surface = resolveSurfaceLabel(tournament.getSurface());
        String category = resolveCategoryLabel(tournament.getCategory());
        String tour = tournament.getTour();

        sb.append("# ").append(tournamentName).append(" 种子列表\n");
        List<String> infoParts = new ArrayList<>();
        if (surface != null && !surface.isEmpty()) infoParts.add(surface);
        if (category != null && !category.isEmpty()) infoParts.add(category);
        if (city != null && !city.isEmpty()) infoParts.add(city);
        if (tour != null && !tour.isEmpty()) infoParts.add(tour);
        sb.append(String.join(" | ", infoParts)).append("\n\n");

        entries.sort(Comparator.comparingInt(e -> e.getSeed().intValue()));

        for (TournamentEntryData entry : entries) {
            PlayerData player = playerMap.get(entry.getPlayerId());
            String playerName = player != null
                    ? (player.getLastName() != null ? player.getLastName() : player.getFirstName())
                    : entry.getPlayerId();
            playerName = translations.getOrDefault(TranslationEntityTypeEnum.PLAYER + ":" + playerName, playerName);

            String nationality = player != null ? player.getNationality() : "";
            String flag = nationality != null ? getFlagEmoji(nationality) : "";
            if (flag == null) flag = "";

            sb.append(flag).append(playerName).append("[").append(entry.getSeed()).append("]\n");
        }

        return sb.toString();
    }

    private List<List<TournamentData>> groupByCityAndTime(List<TournamentData> list) {
        List<List<TournamentData>> groups = new ArrayList<>();

        for (TournamentData data : list) {
            boolean added = false;
            for (List<TournamentData> group : groups) {
                TournamentData first = group.get(0);
                if (isSameGroup(first, data)) {
                    group.add(data);
                    added = true;
                    break;
                }
            }
            if (!added) {
                List<TournamentData> newGroup = new ArrayList<>();
                newGroup.add(data);
                groups.add(newGroup);
            }
        }

        for (List<TournamentData> group : groups) {
            group.sort(Comparator.comparing(TournamentData::getStartDate, Comparator.nullsLast(Comparator.naturalOrder())));
        }

        return groups;
    }

    private boolean isSameGroup(TournamentData a, TournamentData b) {
        String cityA = a.getCity() != null ? a.getCity().toLowerCase() : "";
        String cityB = b.getCity() != null ? b.getCity().toLowerCase() : "";
        if (!cityA.equals(cityB)) {
            return false;
        }

        if (a.getStartDate() == null || b.getStartDate() == null || a.getEndDate() == null || b.getEndDate() == null) {
            return false;
        }

        return !a.getStartDate().isAfter(b.getEndDate()) && !b.getStartDate().isAfter(a.getEndDate());
    }

    private boolean isCategoryKept(TournamentData tournament) {
        String category = tournament.getCategory();
        if (category == null || category.isBlank()) {
            return true;
        }
        try {
            return Integer.parseInt(category.trim()) >= 250;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private String buildTournamentMd(TournamentData tournament, List<MatchData> matches, Map<String, PlayerData> playerMap, Map<String, Short> seedMap, Map<String, String> translations, LocalDate date) {
        StringBuilder sb = new StringBuilder();

        String tournamentName = translations.getOrDefault(TranslationEntityTypeEnum.TOURNAMENT + ":" + tournament.getName(), tournament.getName());
        sb.append("# ").append(tournamentName)
          .append(" ").append(date.format(MONTH_DAY_FMT))
          .append(" 赛程安排\n");
        sb.append(buildTournamentInfo(tournament, translations)).append("\n");

        Map<String, List<MatchData>> matchesByCourt = matches.stream()
                .collect(Collectors.groupingBy(m -> m.getCourt() != null ? m.getCourt() : "未知球场", LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<MatchData>> entry : matchesByCourt.entrySet()) {
            String courtName = translations.getOrDefault(TranslationEntityTypeEnum.COURT + ":" + entry.getKey(), entry.getKey());
            sb.append("\n").append(courtName).append("\n");
            for (MatchData match : entry.getValue()) {
                sb.append(buildMatchLine(match, playerMap, seedMap, translations)).append("\n");
            }
        }

        return sb.toString();
    }

    private Map<String, String> collectAndTranslate(List<TournamentData> tournaments, List<MatchData> matches, List<PlayerData> players, TranslationLanguageEnum language) {
        Set<TranslationKey> keys = new HashSet<>();

        for (TournamentData t : tournaments) {
            if (t.getName() != null) {
                keys.add(new TranslationKey(TranslationEntityTypeEnum.TOURNAMENT, t.getName(), language));
            }
            if (t.getCity() != null) {
                keys.add(new TranslationKey(TranslationEntityTypeEnum.CITY, t.getCity(), language));
            }
        }

        for (MatchData m : matches) {
            if (m.getCourt() != null) {
                keys.add(new TranslationKey(TranslationEntityTypeEnum.COURT, m.getCourt(), language));
            }
        }

        for (PlayerData p : players) {
            String name = p.getLastName() != null ? p.getLastName() : p.getFirstName();
            if (name != null) {
                keys.add(new TranslationKey(TranslationEntityTypeEnum.PLAYER, name, language));
            }
        }

        Map<TranslationKey, String> translationMap = tourTranslationService.translate(keys, language);

        Map<String, String> result = new HashMap<>();
        for (Map.Entry<TranslationKey, String> entry : translationMap.entrySet()) {
            TranslationKey key = entry.getKey();
            result.put(key.getEntityType() + ":" + key.getOriginalText(), entry.getValue());
        }
        return result;
    }

    private String buildTournamentInfo(TournamentData tournament, Map<String, String> translations) {
        List<String> parts = new ArrayList<>();
        if (tournament.getCategory() != null) {
            parts.add(resolveCategoryLabel(tournament.getCategory()));
        }
        if (tournament.getSurface() != null) {
            parts.add(resolveSurfaceLabel(tournament.getSurface()));
        }
        List<String> location = new ArrayList<>();
        if (tournament.getCity() != null) {
            String city = translations.getOrDefault(TranslationEntityTypeEnum.CITY + ":" + tournament.getCity(), tournament.getCity());
            location.add(city);
        }
        if (tournament.getCountry() != null) location.add(tournament.getCountry());
        if (!location.isEmpty()) {
            parts.add(String.join(", ", location));
        }
        return String.join(" | ", parts);
    }

    private String buildMatchLine(MatchData match, Map<String, PlayerData> playerMap, Map<String, Short> seedMap, Map<String, String> translations) {
        StringBuilder sb = new StringBuilder();

        sb.append(buildPlayerDisplay(match.getPlayer1Id(), match.getDrawId(), playerMap, seedMap, translations));
        sb.append(" vs ");
        sb.append(buildPlayerDisplay(match.getPlayer2Id(), match.getDrawId(), playerMap, seedMap, translations));

        if (match.getScheduledAt() != null) {
            sb.append(" ").append(match.getScheduledAt().format(DateTimeFormatter.ofPattern("HH:mm")));
        } else if (match.getScheduledAtText() != null) {
            sb.append(" ").append(match.getScheduledAtText());
        }

        return sb.toString();
    }

    private String buildPlayerDisplay(String playerId, Long drawId, Map<String, PlayerData> playerMap, Map<String, Short> seedMap, Map<String, String> translations) {
        if (playerId == null) return "待定";

        PlayerData player = playerMap.get(playerId);
        String name;
        String nationality = null;

        if (player != null) {
            name = player.getLastName() != null ? player.getLastName() : player.getFirstName();
            nationality = player.getNationality();
        } else {
            name = playerId;
        }

        if (name != null) {
            name = translations.getOrDefault(TranslationEntityTypeEnum.PLAYER + ":" + name, name);
        }

        StringBuilder sb = new StringBuilder();

        if (nationality != null) {
            String flag = getFlagEmoji(nationality);
            if (flag != null) {
                sb.append(flag).append(" ");
            }
        }

        sb.append(name);

        String seedKey = drawId + ":" + playerId;
        Short seed = seedMap.get(seedKey);
        if (seed != null) {
            sb.append("[").append(seed).append("]");
        }

        return sb.toString();
    }

    private String resolveCategoryLabel(String category) {
        if (category == null) return "";
        return switch (category.toUpperCase()) {
            case "GS" -> "大满贯";
            case "1000" -> "1000";
            case "500" -> "500";
            case "250" -> "250";
            case "FINAL", "FINALS" -> "年终总决赛";
            default -> category;
        };
    }

    private String resolveSurfaceLabel(String surface) {
        if (surface == null) return "";
        return switch (surface.toLowerCase()) {
            case "clay" -> "红土";
            case "grass" -> "草地";
            case "hard" -> "硬地";
            case "indoor" -> "室内硬地";
            case "indoor clay" -> "室内红土";
            case "indoor hard" -> "室内硬地";
            default -> surface;
        };
    }

    private String getFlagEmoji(String nationality) {
        if (nationality == null || nationality.length() != 3) return null;
        String code = switch (nationality.toUpperCase()) {
            case "USA" -> "US";
            case "GBR" -> "GB";
            case "FRA" -> "FR";
            case "ESP" -> "ES";
            case "GER" -> "DE";
            case "ITA" -> "IT";
            case "SRB" -> "RS";
            case "RUS" -> "RU";
            case "CHN" -> "CN";
            case "JPN" -> "JP";
            case "KOR" -> "KR";
            case "AUS" -> "AU";
            case "CAN" -> "CA";
            case "BRA" -> "BR";
            case "ARG" -> "AR";
            case "CRO" -> "HR";
            case "POL" -> "PL";
            case "CZE" -> "CZ";
            case "SUI" -> "CH";
            case "AUT" -> "AT";
            case "BEL" -> "BE";
            case "NED" -> "NL";
            case "SWE" -> "SE";
            case "NOR" -> "NO";
            case "DEN" -> "DK";
            case "FIN" -> "FI";
            case "GRE" -> "GR";
            case "POR" -> "PT";
            case "UKR" -> "UA";
            case "BLR" -> "BY";
            case "ROU" -> "RO";
            case "BUL" -> "BG";
            case "HUN" -> "HU";
            case "SVK" -> "SK";
            case "SLO" -> "SI";
            case "GEO" -> "GE";
            case "LAT" -> "LV";
            case "COL" -> "CO";
            case "CHI" -> "CL";
            case "MEX" -> "MX";
            case "TUN" -> "TN";
            case "RSA" -> "ZA";
            case "IND" -> "IN";
            case "THA" -> "TH";
            case "TPE" -> "TW";
            case "KAZ" -> "KZ";
            default -> null;
        };
        if (code == null) return null;
        int firstChar = Character.codePointAt(code, 0) - 0x41 + 0x1F1E6;
        int secondChar = Character.codePointAt(code, 1) - 0x41 + 0x1F1E6;
        return new String(Character.toChars(firstChar)) + new String(Character.toChars(secondChar));
    }

    public void image(StringBuilder sb) {
        String data = """

                # 网球赛事复古海报提示词模板

                1930年代装饰艺术（Art Deco）风格网球赛事宣传海报，手绘插画质感，做旧纸张底纹带有轻微折痕与岁月黄化效果，铅字排版风格，衬线字体大标题搭配细线分割，印刷油墨叠印轻微晕染，颗粒感胶印质感。画面以球场为叙事核心，呈现同一球场上先后登场的至多两场赛事对阵信息，赛程信息以剧院节目单式版式分区排列，背景融入举办城市标志性建筑轮廓作为远景，城市天际线以简笔线描手法贯穿画面中层，球场看台或场地边界线以透视线描方式融入构图，赛事场地氛围与城市地标共同构成画面叙事背景。

                画面主色调以场地类型决定：【场地色彩】。

                画面装饰繁复程度与构图隆重感随赛事级别递进：【级别装饰】。

                画面张力与戏剧氛围随赛程轮次递进：【轮次氛围】。

                【场地色彩】枚举替换：

                红土（Clay）→ 赭石红与沙土黄为主色，温暖厚重的南欧午后光感，土地质感纹理隐现于背景
                草地（Grass）→ 深绿与奶油白为主色，英伦贵族草坪的清雅气质，薄雾晨光氛围
                硬地（Hard Court）→ 午夜蓝与冷灰为主色，现代都市夜场的冷峻光感，城市霓虹隐约映照
                【级别装饰】枚举替换：

                ATP 250 → 细线单层边框，画面留白充裕，装饰纹样轻量，地方俱乐部赛事的亲切氛围
                ATP 500 → 双层边框，四角加入简约纹章，信息分区清晰，正式锦标赛气质
                ATP 1000 → 金线双重边框，四角对称繁复纹章，画面信息密度提升，巡回赛殿堂感
                大满贯 → 金箔烫印质感满版边框，奢华对称纹章占据画面四周，城市地标精细刻画占据显著位置，百年历史传承的仪式感与史诗气质
                年终总决赛 → 深黑底色叠加场地主色，王冠或奖杯图腾融入构图核心，参赛球员剪影对称分布，边框装饰最为繁复华丽，年度收官的终极荣耀感
                【轮次氛围】枚举替换：

                首轮 / 资格赛 → 构图平稳，色调沉稳内敛，两组对阵信息以平等并列方式呈现，张力蓄而未发
                八强 / 四强 → 对角线构图增强动势，色彩饱和度提升，两场对阵信息排布开始产生视觉重心偏移，淘汰赛压迫感隐现
                半决赛 → 强烈明暗对比，聚光灯效果叠加于复古质感之上，两场对阵以对峙感构图呈现，城市夜景或黄昏氛围渲染，悬念与紧张弥漫画面
                决赛 → 单场对阵独占满版，胜负未定的对峙剪影居于画面中心，城市地标以最精细线描占据背景，金色光晕从画面中心向四周扩散，荣耀加冕的史诗收尾感
                """;

        sb.append(data);
    }
}
