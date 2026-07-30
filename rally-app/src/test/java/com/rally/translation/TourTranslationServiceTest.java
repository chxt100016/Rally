package com.rally.translation;

import com.rally.domain.tour.model.MatchGroupDTO;
import com.rally.domain.tour.model.MatchQueryVO;
import com.rally.domain.translation.TranslationQueryService;
import com.rally.domain.translation.model.TranslationKey;
import com.rally.domain.translation.model.TranslationLanguageEnum;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TourTranslationServiceTest {

    @Test
    public void matchGroupsOnlyTranslatesCourtGroups() throws Exception {
        MatchQueryVO match = new MatchQueryVO();
        match.setCourt("Center Court");

        MatchGroupDTO courtGroup = group("Center Court", "Center Court", List.of(match), null);
        MatchGroupDTO dateGroup = group("2026-07-30", "今天", null, List.of(courtGroup));
        MatchGroupDTO roundGroup = group("FINAL", "决赛", List.of(match), null);

        CapturingTranslationQueryService queryService = new CapturingTranslationQueryService();
        TourTranslationService service = new TourTranslationService();
        Field field = TourTranslationService.class.getDeclaredField("translationQueryService");
        field.setAccessible(true);
        field.set(service, queryService);

        service.matchGroups(List.of(dateGroup, roundGroup), TranslationLanguageEnum.ZH_CN);

        assertEquals("今天", dateGroup.getName());
        assertEquals("决赛", roundGroup.getName());
        assertEquals("中央球场", courtGroup.getName());
        assertTrue(queryService.originalTexts.contains("Center Court"));
        assertFalse(queryService.originalTexts.contains("今天"));
        assertFalse(queryService.originalTexts.contains("决赛"));
    }

    private MatchGroupDTO group(String key, String name, List<MatchQueryVO> data, List<MatchGroupDTO> children) {
        MatchGroupDTO group = new MatchGroupDTO();
        group.setKey(key);
        group.setName(name);
        group.setData(data);
        group.setChildren(children);
        return group;
    }

    private static class CapturingTranslationQueryService extends TranslationQueryService {
        private Set<String> originalTexts = Set.of();

        @Override
        public Map<TranslationKey, String> query(Set<TranslationKey> keys) {
            originalTexts = keys.stream().map(TranslationKey::getOriginalText).collect(java.util.stream.Collectors.toSet());
            Map<TranslationKey, String> result = new HashMap<>();
            keys.forEach(key -> result.put(key, "中央球场"));
            return result;
        }
    }
}
