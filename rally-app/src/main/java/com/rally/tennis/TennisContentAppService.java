package com.rally.tennis;

import com.rally.db.tennis.entity.TennisMatchPO;
import com.rally.db.tennis.entity.TennisPlayerPO;
import com.rally.db.tennis.entity.TennisTournamentPO;
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
}
