package com.rally.tennis;

import com.rally.db.tennis.entity.TennisTournamentPO;
import com.rally.db.tennis.repository.TennisTournamentRepository;
import com.rally.domain.tennis.model.TournamentPromptVO;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class TennisPromptService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final Map<String, String> CATEGORY_ANGLE = Map.of(
            "GS",     "low-angle oblique aerial view (45°), full panoramic court, epic scale, packed grandstands visible",
            "1000",   "medium-angle side aerial view (60°), professional atmosphere, imposing presence",
            "500",    "near-ground oblique view, refined composition focused on the court surface",
            "250",    "partial court close-up with blurred surroundings, clean and minimal",
            "final",  "night scene, indoor arena dome overhead view, grand finale atmosphere, dramatic lighting",
            "finals", "night scene, indoor arena dome overhead view, grand finale atmosphere, dramatic lighting"
    );

    private static final Map<String, String> SURFACE_DESC = Map.of(
            "clay",         "red clay court with characteristic terracotta dust and baseline wear marks",
            "grass",        "lush green grass court with crisp white lines",
            "hard",         "blue-green hard court surface with sharp white lines",
            "indoor",       "indoor hard court under warm artificial lighting",
            "indoor clay",  "indoor red clay court under artificial lighting",
            "indoor hard",  "indoor hard court under artificial lighting"
    );

    @Resource
    private TennisTournamentRepository tennisTournamentRepository;

    public String generatePrompt(String tournamentId) {
        TennisTournamentPO po = tennisTournamentRepository.findByTournamentId(tournamentId);
        if (po == null) {
            return null;
        }
        return buildPrompt(po);
    }

    public List<TournamentPromptVO> listPendingPrompts() {
        LocalDate today = LocalDate.now();
        LocalDate dateFrom = today.minusMonths(1);
        LocalDate dateTo = today.plusMonths(1);

        List<TennisTournamentPO> list = tennisTournamentRepository.listPendingBackground(dateFrom, dateTo);
        if (CollectionUtils.isEmpty(list)) {
            return List.of();
        }

        return list.stream().map(po -> {
            TournamentPromptVO vo = new TournamentPromptVO();
            vo.setTournamentId(po.getTournamentId());
            vo.setName(po.getName());
            vo.setCategory(po.getCategory());
            vo.setSurface(po.getSurface());
            vo.setCity(po.getCity());
            vo.setStartDate(po.getStartDate() != null ? po.getStartDate().format(DATE_FMT) : null);
            vo.setPrompt(buildPrompt(po));
            return vo;
        }).toList();
    }

    private String buildPrompt(TennisTournamentPO po) {
        String category = po.getCategory() != null ? po.getCategory().trim() : "";
        String surface  = po.getSurface()  != null ? po.getSurface().trim().toLowerCase() : "";
        String city     = po.getCity()     != null ? po.getCity().trim() : "";

        String angleDesc   = CATEGORY_ANGLE.getOrDefault(category.toLowerCase(),
                CATEGORY_ANGLE.getOrDefault(category, "medium-angle view of the tennis court"));
        String surfaceDesc = SURFACE_DESC.getOrDefault(surface,
                surface + " tennis court");

        String cityLine = city.isBlank() ? ""
                : " Subtly integrate recognizable architectural or natural elements of " + city
                  + " into the distant background, blending naturally without being distracting.";

        String levelNote = resolveLevelNote(category);

        return "Photorealistic tennis venue background image for a mobile app card, 16:9 aspect ratio. "
                + "Court surface: " + surfaceDesc + ". "
                + "Camera angle: " + angleDesc + ". "
                + "Show the central court in detail — net, service boxes, and surrounding seating structure. "
                + cityLine
                + " Do not include any tournament names, logos, sponsor banners, or branded elements. "
                + levelNote
                + "Cinematic color grading, high detail, no people on court.";
    }

    private String resolveLevelNote(String category) {
        if (category == null) return "";
        return switch (category.toUpperCase()) {
            case "GS"     -> "This is a Grand Slam — convey a sense of history, grandeur, and massive scale. ";
            case "1000"   -> "This is a Masters 1000 event — convey prestige and professional intensity. ";
            case "500"    -> "This is an ATP/WTA 500 event — convey a polished, high-level atmosphere. ";
            case "250"    -> "This is an ATP/WTA 250 event — keep the composition clean and focused. ";
            case "FINAL", "FINALS" ->
                    "This is the year-end Finals — distinguish it clearly from regular tour events with a spectacular, celebratory feel. ";
            default       -> "";
        };
    }
}
