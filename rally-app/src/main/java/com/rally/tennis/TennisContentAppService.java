package com.rally.tennis;

import com.rally.db.tennis.entity.TennisDrawPO;
import com.rally.db.tennis.entity.TennisMatchPO;
import com.rally.db.tennis.entity.TennisPlayerPO;
import com.rally.db.tennis.entity.TennisTournamentEntryPO;
import com.rally.db.tennis.entity.TennisTournamentPO;
import com.rally.db.tennis.repository.TennisDrawRepository;
import com.rally.db.tennis.repository.TennisMatchRepository;
import com.rally.db.tennis.repository.TennisPlayerRepository;
import com.rally.db.tennis.repository.TennisTournamentEntryRepository;
import com.rally.db.tennis.repository.TennisTournamentRepository;
import com.rally.domain.translation.model.TranslationEntityTypeEnum;
import com.rally.domain.translation.model.TranslationKey;
import com.rally.domain.translation.model.TranslationLanguageEnum;
import com.rally.translation.TennisTranslationService;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TennisContentAppService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_DAY_FMT = DateTimeFormatter.ofPattern("M月d日");

    @Resource
    private TennisTournamentRepository tennisTournamentRepository;

    @Resource
    private TennisDrawRepository tennisDrawRepository;

    @Resource
    private TennisMatchRepository tennisMatchRepository;

    @Resource
    private TennisPlayerRepository tennisPlayerRepository;

    @Resource
    private TennisTournamentEntryRepository tennisTournamentEntryRepository;

    @Resource
    private TennisTranslationService tennisTranslationService;

    public String generateDailyContent(LocalDate date, TranslationLanguageEnum language) {
        // 1. 查询当天的赛事
        List<TennisTournamentPO> tournaments = tennisTournamentRepository.findCurrentTournaments(date);
        if (CollectionUtils.isEmpty(tournaments)) {
            return "";
        }

        // 过滤赛事：category 不为数字或者为数字但是 >=250
        tournaments = tournaments.stream()
                .filter(this::isCategoryKept)
                .toList();
        if (CollectionUtils.isEmpty(tournaments)) {
            return "";
        }

        // 2. 查询各赛事当天的比赛
        List<String> tournamentIds = tournaments.stream()
                .map(TennisTournamentPO::getTournamentId)
                .toList();
        List<TennisMatchPO> matches = tennisMatchRepository.findByTournamentIdsAndDate(tournamentIds, date);
        if (CollectionUtils.isEmpty(matches)) {
            return "";
        }

        // 3. 查询种子信息 (drawId -> playerId -> seed)
        List<Long> drawIds = matches.stream()
                .map(TennisMatchPO::getDrawId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<String, Short> seedMap = tennisTournamentEntryRepository.listSeedMapByDrawIds(drawIds);

        // 4. 查询球员信息
        Set<String> playerIds = new HashSet<>();
        for (TennisMatchPO match : matches) {
            if (match.getPlayer1Id() != null) playerIds.add(match.getPlayer1Id());
            if (match.getPlayer2Id() != null) playerIds.add(match.getPlayer2Id());
        }
        List<TennisPlayerPO> players = tennisPlayerRepository.listByPlayerIds(new ArrayList<>(playerIds));
        Map<String, TennisPlayerPO> playerMap = players.stream()
                .collect(Collectors.toMap(TennisPlayerPO::getPlayerId, p -> p, (a, b) -> a));

        // 5. 构建赛事映射
        Map<String, TennisTournamentPO> tournamentMap = tournaments.stream()
                .collect(Collectors.toMap(TennisTournamentPO::getTournamentId, t -> t, (a, b) -> a));

        // 6. 收集需要翻译的内容并翻译
        Map<String, String> translations = collectAndTranslate(tournaments, matches, players, language);

        // 7. 按赛事分组
        Map<String, List<TennisMatchPO>> matchesByTournament = matches.stream()
                .collect(Collectors.groupingBy(TennisMatchPO::getTournamentId));

        // 8. 合并相同赛事（城市相同且时间重合）
        List<List<TennisTournamentPO>> tournamentGroups = groupByCityAndTime(tournaments);

        // 9. 每个赛事组生成MD，用换行分隔
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (List<TennisTournamentPO> group : tournamentGroups) {
            // 收集该组所有比赛
            List<TennisMatchPO> groupMatches = new ArrayList<>();
            for (TennisTournamentPO tournament : group) {
                List<TennisMatchPO> tournamentMatches = matchesByTournament.get(tournament.getTournamentId());
                if (CollectionUtils.isNotEmpty(tournamentMatches)) {
                    groupMatches.addAll(tournamentMatches);
                }
            }
            if (CollectionUtils.isEmpty(groupMatches)) continue;

            if (!first) {
                sb.append("\n\n");
            }
            // 使用第一个赛事作为代表
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

        // 1. 查询赛事
        List<TennisTournamentPO> tournaments = tennisTournamentRepository.listByTournamentIds(tournamentIds);
        if (CollectionUtils.isEmpty(tournaments)) {
            return "";
        }

        // 过滤 category
        tournaments = tournaments.stream().filter(this::isCategoryKept).toList();
        if (CollectionUtils.isEmpty(tournaments)) {
            return "";
        }

        // 2. 查询 draw
        List<TennisDrawPO> draws = tennisDrawRepository.listByTournamentIds(tournamentIds);
        if (CollectionUtils.isEmpty(draws)) {
            return "";
        }

        // tournamentId -> drawIds
        Map<String, List<Long>> tournamentDrawMap = draws.stream()
                .collect(Collectors.groupingBy(
                        TennisDrawPO::getTournamentId,
                        Collectors.mapping(TennisDrawPO::getId, Collectors.toList())
                ));

        // 3. 查询所有种子 entries
        List<Long> allDrawIds = draws.stream().map(TennisDrawPO::getId).toList();
        List<TennisTournamentEntryPO> allEntries = tennisTournamentEntryRepository.listByDrawIds(allDrawIds);
        List<TennisTournamentEntryPO> seededEntries = allEntries.stream()
                .filter(e -> e.getSeed() != null && e.getSeed() > 0)
                .toList();
        if (CollectionUtils.isEmpty(seededEntries)) {
            return "";
        }

        // 4. 查询球员
        Set<String> playerIds = seededEntries.stream()
                .map(TennisTournamentEntryPO::getPlayerId)
                .collect(Collectors.toSet());
        List<TennisPlayerPO> players = tennisPlayerRepository.listByPlayerIds(new ArrayList<>(playerIds));
        Map<String, TennisPlayerPO> playerMap = players.stream()
                .collect(Collectors.toMap(TennisPlayerPO::getPlayerId, p -> p, (a, b) -> a));

        // 5. 翻译：赛事名、城市、球员名
        Map<String, String> translations = collectSeedTranslations(tournaments, players, language);

        // 6. 按 tournament 分组生成 MD
        Map<String, TennisTournamentPO> tournamentMap = tournaments.stream()
                .collect(Collectors.toMap(TennisTournamentPO::getTournamentId, t -> t, (a, b) -> a));

        // 按 tournamentId 分组 seed entries
        // 需要通过 drawId 反查 tournamentId
        Map<Long, String> drawToTournamentMap = new HashMap<>();
        for (TennisDrawPO draw : draws) {
            drawToTournamentMap.put(draw.getId(), draw.getTournamentId());
        }

        Map<String, List<TennisTournamentEntryPO>> seedsByTournament = new LinkedHashMap<>();
        for (TennisTournamentEntryPO entry : seededEntries) {
            String tid = drawToTournamentMap.get(entry.getDrawId());
            if (tid != null) {
                seedsByTournament.computeIfAbsent(tid, k -> new ArrayList<>()).add(entry);
            }
        }

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String tid : seedsByTournament.keySet()) {
            TennisTournamentPO tournament = tournamentMap.get(tid);
            if (tournament == null) continue;

            List<TennisTournamentEntryPO> entries = seedsByTournament.get(tid);
            if (CollectionUtils.isEmpty(entries)) continue;

            if (!first) {
                sb.append("\n\n");
            }

            sb.append(buildTournamentSeedMd(tournament, entries, playerMap, translations));
            first = false;
        }

        return sb.toString();
    }

    private Map<String, String> collectSeedTranslations(List<TennisTournamentPO> tournaments,
                                                         List<TennisPlayerPO> players,
                                                         TranslationLanguageEnum language) {
        Set<TranslationKey> keys = new HashSet<>();
        for (TennisTournamentPO t : tournaments) {
            if (t.getName() != null) {
                keys.add(new TranslationKey(TranslationEntityTypeEnum.TOURNAMENT, t.getName(), language));
            }
            if (t.getCity() != null) {
                keys.add(new TranslationKey(TranslationEntityTypeEnum.CITY, t.getCity(), language));
            }
        }
        for (TennisPlayerPO p : players) {
            String name = p.getLastName() != null ? p.getLastName() : p.getFirstName();
            if (name != null) {
                keys.add(new TranslationKey(TranslationEntityTypeEnum.PLAYER, name, language));
            }
        }
        Map<TranslationKey, String> translationMap = tennisTranslationService.translate(keys, language);
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<TranslationKey, String> entry : translationMap.entrySet()) {
            TranslationKey key = entry.getKey();
            result.put(key.getEntityType() + ":" + key.getOriginalText(), entry.getValue());
        }
        return result;
    }

    private String buildTournamentSeedMd(TennisTournamentPO tournament,
                                          List<TennisTournamentEntryPO> entries,
                                          Map<String, TennisPlayerPO> playerMap,
                                          Map<String, String> translations) {
        StringBuilder sb = new StringBuilder();

        // 赛事名称（翻译）
        String tournamentName = translations.getOrDefault(
                TranslationEntityTypeEnum.TOURNAMENT + ":" + tournament.getName(),
                tournament.getName()
        );

        // 城市（翻译）
        String city = translations.getOrDefault(
                TranslationEntityTypeEnum.CITY + ":" + tournament.getCity(),
                tournament.getCity()
        );

        // 场地类型
        String surface = resolveSurfaceLabel(tournament.getSurface());
        // 赛事级别
        String category = resolveCategoryLabel(tournament.getCategory());
        // tour
        String tour = tournament.getTour();

        // 标题行
        sb.append("# ").append(tournamentName).append(" 种子列表\n");
        // 信息行
        List<String> infoParts = new ArrayList<>();
        if (surface != null && !surface.isEmpty()) infoParts.add(surface);
        if (category != null && !category.isEmpty()) infoParts.add(category);
        if (city != null && !city.isEmpty()) infoParts.add(city);
        if (tour != null && !tour.isEmpty()) infoParts.add(tour);
        sb.append(String.join(" | ", infoParts)).append("\n\n");

        // 按种子排序
        entries.sort(Comparator.comparingInt(TennisTournamentEntryPO::getSeed));

        for (TennisTournamentEntryPO entry : entries) {
            TennisPlayerPO player = playerMap.get(entry.getPlayerId());
            String playerName = player != null
                    ? (player.getLastName() != null ? player.getLastName() : player.getFirstName())
                    : entry.getPlayerId();
            // 翻译球员名
            playerName = translations.getOrDefault(
                    TranslationEntityTypeEnum.PLAYER + ":" + playerName,
                    playerName
            );

            String nationality = player != null ? player.getNationality() : "";
            String flag = nationality != null ? getFlagEmoji(nationality) : "";
            if (flag == null) flag = "";

            sb.append(flag).append(playerName).append("[").append(entry.getSeed()).append("]\n");
        }

        return sb.toString();
    }

    /**
     * 按城市和时间分组：city 不区分大小写相同 且 startDate 和 endDate 时间重合算作同一分组
     */
    private List<List<TennisTournamentPO>> groupByCityAndTime(List<TennisTournamentPO> list) {
        List<List<TennisTournamentPO>> groups = new ArrayList<>();

        for (TennisTournamentPO po : list) {
            boolean added = false;
            for (List<TennisTournamentPO> group : groups) {
                TennisTournamentPO first = group.get(0);
                if (isSameGroup(first, po)) {
                    group.add(po);
                    added = true;
                    break;
                }
            }
            if (!added) {
                List<TennisTournamentPO> newGroup = new ArrayList<>();
                newGroup.add(po);
                groups.add(newGroup);
            }
        }

        // 组内按 startDate 排序
        for (List<TennisTournamentPO> group : groups) {
            group.sort(Comparator.comparing(TennisTournamentPO::getStartDate,
                    Comparator.nullsLast(Comparator.naturalOrder())));
        }

        return groups;
    }

    /**
     * 判断两个赛事是否属于同一分组：city 不区分大小写相同 且 startDate 和 endDate 时间重合
     */
    private boolean isSameGroup(TennisTournamentPO a, TennisTournamentPO b) {
        String cityA = a.getCity() != null ? a.getCity().toLowerCase() : "";
        String cityB = b.getCity() != null ? b.getCity().toLowerCase() : "";
        if (!cityA.equals(cityB)) {
            return false;
        }

        if (a.getStartDate() == null || b.getStartDate() == null ||
            a.getEndDate() == null || b.getEndDate() == null) {
            return false;
        }

        // 时间重合判断：a.start <= b.end && b.start <= a.end
        return !a.getStartDate().isAfter(b.getEndDate()) && !b.getStartDate().isAfter(a.getEndDate());
    }

    private boolean isCategoryKept(TennisTournamentPO tournament) {
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

    private String buildTournamentMd(TennisTournamentPO tournament, List<TennisMatchPO> matches,
                                      Map<String, TennisPlayerPO> playerMap, Map<String, Short> seedMap,
                                      Map<String, String> translations, LocalDate date) {
        StringBuilder sb = new StringBuilder();

        // 赛事标题: 赛事名称 月份日期 赛程安排
        String tournamentName = translations.getOrDefault(
                TranslationEntityTypeEnum.TOURNAMENT + ":" + tournament.getName(),
                tournament.getName()
        );
        sb.append("# ").append(tournamentName)
          .append(" ").append(date.format(MONTH_DAY_FMT))
          .append(" 赛程安排\n");
        sb.append(buildTournamentInfo(tournament, translations)).append("\n");

        // 按球场分组
        Map<String, List<TennisMatchPO>> matchesByCourt = matches.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getCourt() != null ? m.getCourt() : "未知球场",
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        for (Map.Entry<String, List<TennisMatchPO>> entry : matchesByCourt.entrySet()) {
            String courtName = translations.getOrDefault(
                    TranslationEntityTypeEnum.COURT + ":" + entry.getKey(),
                    entry.getKey()
            );
            sb.append("\n").append(courtName).append("\n");
            for (TennisMatchPO match : entry.getValue()) {
                sb.append(buildMatchLine(match, playerMap, seedMap, translations)).append("\n");
            }
        }

        return sb.toString();
    }

    private Map<String, String> collectAndTranslate(List<TennisTournamentPO> tournaments,
                                                     List<TennisMatchPO> matches,
                                                     List<TennisPlayerPO> players,
                                                     TranslationLanguageEnum language) {
        Set<TranslationKey> keys = new HashSet<>();

        // 收集赛事名
        for (TennisTournamentPO t : tournaments) {
            if (t.getName() != null) {
                keys.add(new TranslationKey(TranslationEntityTypeEnum.TOURNAMENT, t.getName(), language));
            }
            if (t.getCity() != null) {
                keys.add(new TranslationKey(TranslationEntityTypeEnum.CITY, t.getCity(), language));
            }
        }

        // 收集球场名
        for (TennisMatchPO m : matches) {
            if (m.getCourt() != null) {
                keys.add(new TranslationKey(TranslationEntityTypeEnum.COURT, m.getCourt(), language));
            }
        }

        // 收集球员名
        for (TennisPlayerPO p : players) {
            String name = p.getLastName() != null ? p.getLastName() : p.getFirstName();
            if (name != null) {
                keys.add(new TranslationKey(TranslationEntityTypeEnum.PLAYER, name, language));
            }
        }

        // 批量查询翻译
        Map<TranslationKey, String> translationMap = tennisTranslationService.translate(keys, language);

        // 转换为简单Map便于查询
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<TranslationKey, String> entry : translationMap.entrySet()) {
            TranslationKey key = entry.getKey();
            result.put(key.getEntityType() + ":" + key.getOriginalText(), entry.getValue());
        }
        return result;
    }

    private String buildTournamentInfo(TennisTournamentPO tournament, Map<String, String> translations) {
        List<String> parts = new ArrayList<>();
        if (tournament.getCategory() != null) {
            parts.add(resolveCategoryLabel(tournament.getCategory()));
        }
        if (tournament.getSurface() != null) {
            parts.add(resolveSurfaceLabel(tournament.getSurface()));
        }
        List<String> location = new ArrayList<>();
        if (tournament.getCity() != null) {
            String city = translations.getOrDefault(
                    TranslationEntityTypeEnum.CITY + ":" + tournament.getCity(),
                    tournament.getCity()
            );
            location.add(city);
        }
        if (tournament.getCountry() != null) location.add(tournament.getCountry());
        if (!location.isEmpty()) {
            parts.add(String.join(", ", location));
        }
        return String.join(" | ", parts);
    }

    private String buildMatchLine(TennisMatchPO match, Map<String, TennisPlayerPO> playerMap,
                                  Map<String, Short> seedMap, Map<String, String> translations) {
        StringBuilder sb = new StringBuilder();

        // 球员1
        sb.append(buildPlayerDisplay(match.getPlayer1Id(), match.getDrawId(), playerMap, seedMap, translations));
        sb.append(" vs ");
        // 球员2
        sb.append(buildPlayerDisplay(match.getPlayer2Id(), match.getDrawId(), playerMap, seedMap, translations));

        // 时间
        if (match.getScheduledAt() != null) {
            sb.append(" ").append(match.getScheduledAt().format(DateTimeFormatter.ofPattern("HH:mm")));
        } else if (match.getScheduledAtText() != null) {
            sb.append(" ").append(match.getScheduledAtText());
        }

        return sb.toString();
    }

    private String buildPlayerDisplay(String playerId, Long drawId,
                                      Map<String, TennisPlayerPO> playerMap,
                                      Map<String, Short> seedMap,
                                      Map<String, String> translations) {
        if (playerId == null) return "待定";

        TennisPlayerPO player = playerMap.get(playerId);
        String name;
        String nationality = null;

        if (player != null) {
            name = player.getLastName() != null ? player.getLastName() : player.getFirstName();
            nationality = player.getNationality();
        } else {
            name = playerId;
        }

        // 翻译球员名
        if (name != null) {
            name = translations.getOrDefault(TranslationEntityTypeEnum.PLAYER + ":" + name, name);
        }

        StringBuilder sb = new StringBuilder();

        // 国旗
        if (nationality != null) {
            String flag = getFlagEmoji(nationality);
            if (flag != null) {
                sb.append(flag).append(" ");
            }
        }

        // 球员名
        sb.append(name);

        // 种子
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
        // 将3字母国家代码转换为2字母代码，再转国旗emoji
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
