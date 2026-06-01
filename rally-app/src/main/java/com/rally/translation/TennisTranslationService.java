package com.rally.translation;

import com.rally.domain.tennis.model.*;
import com.rally.domain.translation.TranslationQueryService;
import com.rally.domain.translation.model.TranslationEntityTypeEnum;
import com.rally.domain.translation.model.TranslationKey;
import com.rally.domain.translation.model.TranslationLanguageEnum;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TennisTranslationService {


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
                case SURFACE -> map.get(entry.getKey()).forEach(item -> item.setSurfaceLabel(entry.getValue()));
            }
        }
    }



    /**
     * 批量翻译比赛列表：court（球场）、round（轮次）、player name（球员姓名）
     */
    public void matches(List<MatchQueryVO> vos, TranslationLanguageEnum language) {
        if (CollectionUtils.isEmpty(vos)) return;

        Map<TranslationKey, List<MatchQueryVO>> map = new HashMap<>();
        for (MatchQueryVO vo : vos) {
            if (vo.getCourt() != null) {
                map.computeIfAbsent(new TranslationKey(TranslationEntityTypeEnum.COURT, vo.getCourt(), language), k -> new ArrayList<>()).add(vo);
            }
            if (vo.getPlayer1() != null) {
                map.computeIfAbsent(new TranslationKey(TranslationEntityTypeEnum.PLAYER, vo.getPlayer1().getName(), language), k -> new ArrayList<>()).add(vo);
            }
            if (vo.getPlayer2() != null) {
                map.computeIfAbsent(new TranslationKey(TranslationEntityTypeEnum.PLAYER, vo.getPlayer2().getName(), language), k -> new ArrayList<>()).add(vo);
            }
        }

        Map<TranslationKey, String> translationMap = this.translationQueryService.query(map.keySet());
        for (Map.Entry<TranslationKey, String> entry : translationMap.entrySet()) {
            String translated = entry.getValue();
            String original = entry.getKey().getOriginalText();
            switch (entry.getKey().getEntityType()) {
                case COURT -> map.get(entry.getKey()).forEach(vo -> vo.setCourt(translated));
                case TOURNAMENT -> map.get(entry.getKey()).forEach(vo -> vo.setRound(translated));
                case PLAYER -> map.get(entry.getKey()).forEach(vo -> {
                    if (vo.getPlayer1() != null && original.equals(vo.getPlayer1().getName())) vo.getPlayer1().setName(translated);
                    if (vo.getPlayer2() != null && original.equals(vo.getPlayer2().getName())) vo.getPlayer2().setName(translated);
                });
            }
        }
    }

    /**
     * 批量翻译球员列表：name（球员姓名）
     */
    public void players(List<PlayerQueryVO> vos, TranslationLanguageEnum language) {
        if (CollectionUtils.isEmpty(vos)) return;

        Map<TranslationKey, List<PlayerQueryVO>> map = new HashMap<>();
        for (PlayerQueryVO vo : vos) {
            map.computeIfAbsent(new TranslationKey(TranslationEntityTypeEnum.PLAYER, vo.getName(), language), k -> new ArrayList<>()).add(vo);
        }

        Map<TranslationKey, String> translationMap = this.translationQueryService.query(map.keySet());
        for (Map.Entry<TranslationKey, String> entry : translationMap.entrySet()) {
            map.get(entry.getKey()).forEach(vo -> vo.setName(entry.getValue()));
        }
    }

    public void seeds(List<SeedVO> vos, TranslationLanguageEnum language) {
        if (CollectionUtils.isEmpty(vos)) return;

        Map<TranslationKey, List<SeedVO>> map = new HashMap<>();
        for (SeedVO vo : vos) {
            map.computeIfAbsent(new TranslationKey(TranslationEntityTypeEnum.PLAYER, vo.getName(), language), k -> new ArrayList<>()).add(vo);
        }

        Map<TranslationKey, String> translationMap = this.translationQueryService.query(map.keySet());
        for (Map.Entry<TranslationKey, String> entry : translationMap.entrySet()) {
            map.get(entry.getKey()).forEach(vo -> vo.setName(entry.getValue()));
        }
    }

    /**
     * 翻译球员弹窗 VO：主球员姓名 + 所有对手姓名（晋级路线、前方对手、出局信息）
     */
    public void playerTournament(PlayerTournamentVO vo, TranslationLanguageEnum language) {
        if (vo == null) return;

        // 收集所有 MatchProgressVO（晋级路线 + 前方对手 + 出局信息）
        List<MatchProgressVO> allProgress = new ArrayList<>();
        if (vo.getProgressPath() != null) allProgress.addAll(vo.getProgressPath());
        if (vo.getUpcomingOpponents() != null) allProgress.addAll(vo.getUpcomingOpponents());
        if (vo.getEliminationInfo() != null) allProgress.add(vo.getEliminationInfo());

        // key → MatchProgressVO 列表（同名对手可能出现在多条记录中）
        Map<TranslationKey, List<MatchProgressVO>> progressMap = new HashMap<>();
        for (MatchProgressVO m : allProgress) {
            if (m.getOpponentName() != null) {
                progressMap.computeIfAbsent(
                        new TranslationKey(TranslationEntityTypeEnum.PLAYER, m.getOpponentName(), language),
                        k -> new ArrayList<>()
                ).add(m);
            }
        }

        // 合并主球员 key，一次批量查询
        Set<TranslationKey> allKeys = new HashSet<>(progressMap.keySet());
        PlayerTournamentDetailVO player = vo.getPlayer();
        TranslationKey playerKey = null;
        if (player != null && player.getName() != null) {
            playerKey = new TranslationKey(TranslationEntityTypeEnum.PLAYER, player.getName(), language);
            allKeys.add(playerKey);
        }
        if (allKeys.isEmpty()) return;

        Map<TranslationKey, String> translationMap = this.translationQueryService.query(allKeys);

        // 回写主球员姓名
        if (playerKey != null) {
            String translated = translationMap.get(playerKey);
            if (translated != null) player.setName(translated);
        }

        // 回写对手姓名
        for (Map.Entry<TranslationKey, String> entry : translationMap.entrySet()) {
            List<MatchProgressVO> matches = progressMap.get(entry.getKey());
            if (matches != null) {
                matches.forEach(m -> m.setOpponentName(entry.getValue()));
            }
        }
    }
}
