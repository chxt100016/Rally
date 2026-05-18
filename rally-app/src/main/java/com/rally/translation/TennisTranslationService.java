package com.rally.translation;

import com.rally.domain.tennis.model.MatchQueryVO;
import com.rally.domain.tennis.model.PlayerQueryVO;
import com.rally.domain.tennis.model.TournamentDTO;
import com.rally.domain.translation.TranslationQueryService;
import com.rally.domain.translation.model.TranslationData;
import com.rally.domain.translation.model.TranslationEntityTypeEnum;
import com.rally.domain.translation.model.TranslationKey;
import com.rally.domain.translation.model.TranslationLanguageEnum;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TennisTranslationService {

    private static final TranslationLanguageEnum DEFAULT_LANG = TranslationLanguageEnum.ZH_CN;



    @Resource
    private TranslationQueryService translationQueryService;



    /**
     * 批量翻译赛事列表：name（赛事名）、surfaceLabel（场地类型）、city（城市）
     */
    public void tournaments(List<TournamentDTO> data, TranslationLanguageEnum language) {
        if (CollectionUtils.isEmpty(data)) return;

        Map<TranslationKey, List<TournamentDTO>> map = new HashMap<>();
        for (TournamentDTO item : data) {
            map.computeIfAbsent(new TranslationKey(TranslationEntityTypeEnum.TOURNAMENT, item.getName(), language), k -> new ArrayList<>()).add(item);
            map.computeIfAbsent(new TranslationKey(TranslationEntityTypeEnum.CITY, item.getCity()  , language), k -> new ArrayList<>()).add(item);
            map.computeIfAbsent(new TranslationKey(TranslationEntityTypeEnum.SURFACE, item.getSurfaceLabel(), language), k -> new ArrayList<>()).add(item);
        }

        Map<TranslationKey, String> translationMap = this.translationQueryService.query(map.keySet());
        for (Map.Entry<TranslationKey, String> entry : translationMap.entrySet()) {
            switch (entry.getKey().getEntityType()) {
                case TOURNAMENT -> map.get(entry.getKey()).forEach(item -> item.setName(entry.getValue()));
                case CITY -> map.get(entry.getKey()).forEach(item -> item.setCity(entry.getValue()));
                case SURFACE -> map.get(entry.getKey()).forEach(item -> item.setSurface(entry.getValue()));
            }
        }
    }

    /**
     * 批量翻译比赛列表：court（球场）、round（轮次）、player name（球员姓名）
     */
    public void matches(List<MatchQueryVO> vos) {
        if (CollectionUtils.isEmpty(vos)) return;

        Map<String, String> translations = batchTranslate(buildMatchQueries(vos));

        for (MatchQueryVO vo : vos) {
            vo.setCourt(lookup(translations, TranslationEntityTypeEnum.COURT, vo.getCourt()));
            vo.setRound(lookup(translations, TranslationEntityTypeEnum.TOURNAMENT, vo.getRound()));
            if (vo.getPlayer1() != null) {
                vo.getPlayer1().setName(lookup(translations, TranslationEntityTypeEnum.PLAYER, vo.getPlayer1().getName()));
            }
            if (vo.getPlayer2() != null) {
                vo.getPlayer2().setName(lookup(translations, TranslationEntityTypeEnum.PLAYER, vo.getPlayer2().getName()));
            }
        }
    }

    /**
     * 批量翻译球员列表：name（球员姓名）
     */
    public void players(List<PlayerQueryVO> vos) {
        if (CollectionUtils.isEmpty(vos)) return;

        Map<String, String> translations = batchTranslate(buildPlayerQueries(vos));

        for (PlayerQueryVO vo : vos) {
            vo.setName(lookup(translations, TranslationEntityTypeEnum.PLAYER, vo.getName()));
        }
    }

    // ---------- 构建批量查询列表 ----------

    private List<TranslationData> buildTournamentQueries(List<TournamentDTO> vos) {
        Set<String> seen = new HashSet<>();
        List<TranslationData> queries = new ArrayList<>();
        for (TournamentDTO vo : vos) {
            addQuery(queries, seen, TranslationEntityTypeEnum.TOURNAMENT, vo.getName());
            addQuery(queries, seen, TranslationEntityTypeEnum.SURFACE, vo.getSurfaceLabel());
            addQuery(queries, seen, TranslationEntityTypeEnum.CITY, vo.getCity());
        }
        return queries;
    }

    private List<TranslationData> buildMatchQueries(List<MatchQueryVO> vos) {
        Set<String> seen = new HashSet<>();
        List<TranslationData> queries = new ArrayList<>();
        for (MatchQueryVO vo : vos) {
            addQuery(queries, seen, TranslationEntityTypeEnum.COURT, vo.getCourt());
            addQuery(queries, seen, TranslationEntityTypeEnum.TOURNAMENT, vo.getRound());
            if (vo.getPlayer1() != null) {
                addQuery(queries, seen, TranslationEntityTypeEnum.PLAYER, vo.getPlayer1().getName());
            }
            if (vo.getPlayer2() != null) {
                addQuery(queries, seen, TranslationEntityTypeEnum.PLAYER, vo.getPlayer2().getName());
            }
        }
        return queries;
    }

    private List<TranslationData> buildPlayerQueries(List<PlayerQueryVO> vos) {
        Set<String> seen = new HashSet<>();
        List<TranslationData> queries = new ArrayList<>();
        for (PlayerQueryVO vo : vos) {
            addQuery(queries, seen, TranslationEntityTypeEnum.PLAYER, vo.getName());
        }
        return queries;
    }

    // ---------- 工具方法 ----------

    /** 向查询列表追加一条，自动去重（相同 entityType+text 只追加一次） */
    private void addQuery(List<TranslationData> queries, Set<String> seen,
                          TranslationEntityTypeEnum entityType, String text) {
        if (text == null || text.isBlank()) return;
        // seen key 仅用于本次批量去重，与 translateBatch 内部 key 格式无关
        if (!seen.add(entityType.name() + ":" + text)) return;
        TranslationData q = new TranslationData();
        q.setEntityType(entityType);
        q.setOriginalText(text);
        q.setLanguage(DEFAULT_LANG);
        queries.add(q);
    }

    private Map<String, String> batchTranslate(List<TranslationData> queries) {
        if (queries.isEmpty()) return Map.of();
        return translationQueryService.translateBatch(queries);
    }

    /**
     * 从批量翻译结果中取值；key 格式与 TranslationQueryService#buildResultKey 一致：
     * entityType:originalText:language
     */
    private String lookup(Map<String, String> translations, TranslationEntityTypeEnum entityType, String original) {
        if (original == null || original.isBlank()) return original;
        String key = entityType.name() + ":" + original + ":" + DEFAULT_LANG.name();
        return translations.getOrDefault(key, original);
    }
}
