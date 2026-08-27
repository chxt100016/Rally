package com.rally.protourdata.playerquery.activity;

import com.rally.domain.tour.model.CountryEnum;
import com.rally.domain.tour.model.PlayerData;
import com.rally.domain.tour.model.PlayerQueryVO;
import com.rally.domain.tour.repository.TourPlayerRepository;
import com.rally.domain.translation.cache.TranslationCache;
import com.rally.domain.translation.model.TranslationEntityTypeEnum;
import com.rally.domain.translation.model.TranslationKey;
import com.rally.domain.translation.model.TranslationLanguageEnum;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 业务活动 query-ranked-tour-players：查询已排名球员并应用已有简中姓名译文。 */
@Component
@RequiredArgsConstructor
public class QueryRankedTourPlayersActivity {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final TranslationLanguageEnum TARGET_LANGUAGE = TranslationLanguageEnum.ZH_CN;

    private final TourPlayerRepository tourPlayerRepository;
    private final TranslationCache translationCache;

    public Result execute(String tour) {
        // A1：保留 main 的参数口径，空白直接返回；非空只转大写，不裁剪或限制巡回赛枚举。
        if (tour == null || tour.isBlank()) {
            return Result.empty();
        }

        // A2：仓储保持 tour 精确匹配、rank 非空、rank 升序和不分页语义。
        LocalDate today = LocalDate.now();
        List<PlayerQueryVO> players = tourPlayerRepository.listByTourOrderByRank(tour.toUpperCase())
                .stream()
                .map(player -> toPlayerQueryVO(player, today))
                .toList();

        // A4：空译文或未命中时保留原姓名并输出缺译键，供后续登记活动逐项处理。
        Set<TranslationKey> missingTranslationKeys = new LinkedHashSet<>();
        for (PlayerQueryVO player : players) {
            TranslationKey key = new TranslationKey(
                    TranslationEntityTypeEnum.PLAYER, player.getName(), TARGET_LANGUAGE);
            String translated = translationCache.get(key);
            if (StringUtils.isNotBlank(translated)) {
                player.setName(translated);
            } else {
                missingTranslationKeys.add(key);
            }
        }
        return new Result(players, missingTranslationKeys);
    }

    private PlayerQueryVO toPlayerQueryVO(PlayerData player, LocalDate today) {
        // A3：DTO 字段、姓名拼接、国家回退及年龄/日期计算均保持 main 的现有行为。
        PlayerQueryVO result = new PlayerQueryVO();
        result.setId(player.getPlayerId());
        result.setRank(player.getRank());
        String firstName = player.getFirstName() == null ? "" : player.getFirstName();
        String lastName = player.getLastName() == null ? "" : player.getLastName();
        result.setName((firstName + " " + lastName).trim());
        result.setCountry(CountryEnum.getCountry(player.getNationality()));
        result.setPoints(player.getPoints());
        if (player.getBirthDate() != null) {
            result.setAge(Period.between(player.getBirthDate(), today).getYears());
            result.setBirthDate(player.getBirthDate().format(DATE_FORMATTER));
        }
        return result;
    }

    /** 返回旧接口 DTO 列表，以及交给后续登记活动的去重缺译键。 */
    public record Result(List<PlayerQueryVO> players, Set<TranslationKey> missingTranslationKeys) {
        public Result {
            players = players == null ? List.of() : List.copyOf(players);
            missingTranslationKeys = missingTranslationKeys == null
                    ? Set.of()
                    : Collections.unmodifiableSet(new LinkedHashSet<>(missingTranslationKeys));
        }

        public static Result empty() {
            return new Result(List.of(), Set.of());
        }
    }
}
