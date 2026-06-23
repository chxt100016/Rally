package com.rally.tour;

import com.rally.domain.tour.gateway.TourTournamentGateway;
import com.rally.domain.tour.model.TournamentData;
import com.rally.domain.tour.model.TournamentPromptVO;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class TourPromptService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final Map<String, String> CATEGORY_ANGLE = Map.of(
            "GS",     "低空斜俯视（45°）+ 全景球场，壮观史诗感，展现观众席规模",
            "1000",   "中角度侧俯视（60°），专业感强，有一定气势",
            "500",    "接近球场地面的斜视角，精致感，聚焦球场本身",
            "250",    "球场局部特写 + 环境虚化，简洁干净",
            "final",  "夜景 + 室内穹顶俯视，与所有常规赛事区分，强调收官之战氛围",
            "finals", "夜景 + 室内穹顶俯视，与所有常规赛事区分，强调收官之战氛围"
    );

    private static final Map<String, String> SURFACE_DESC = Map.of(
            "clay",         "红土",
            "grass",        "草地",
            "hard",         "硬地",
            "indoor",       "室内硬地",
            "indoor clay",  "室内红土",
            "indoor hard",  "室内硬地"
    );

    @Resource
    private TourTournamentGateway tourTournamentGateway;

    public String generatePrompt(String tournamentId) {
        TournamentData data = tourTournamentGateway.findByTournamentId(tournamentId);
        if (data == null) return null;
        return buildPrompt(data);
    }

    public List<TournamentPromptVO> listPendingPrompts() {
        LocalDate today = LocalDate.now();
        LocalDate dateFrom = today.minusMonths(1);
        LocalDate dateTo = today.plusMonths(1);

        List<TournamentData> list = tourTournamentGateway.listPendingBackground(dateFrom, dateTo);
        if (CollectionUtils.isEmpty(list)) return List.of();

        return list.stream()
                .filter(data -> isCategoryKept(data.getCategory()))
                .map(data -> {
                    TournamentPromptVO vo = new TournamentPromptVO();
                    vo.setTournamentId(data.getTournamentId());
                    vo.setName(data.getName());
                    vo.setCategory(data.getCategory());
                    vo.setSurface(data.getSurface());
                    vo.setCity(data.getCity());
                    vo.setStartDate(data.getStartDate() != null ? data.getStartDate().format(DATE_FMT) : null);
                    vo.setPrompt(buildPrompt(data));
                    return vo;
                }).toList();
    }

    private boolean isCategoryKept(String category) {
        if (category == null || category.isBlank()) {
            return true;
        }
        try {
            return Integer.parseInt(category.trim()) >= 250;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private String buildPrompt(TournamentData data) {
        String category = data.getCategory() != null ? data.getCategory().trim() : "";
        String surface  = data.getSurface()  != null ? data.getSurface().trim().toLowerCase() : "";
        String city     = data.getCity()     != null ? data.getCity().trim() : "";
        String name     = data.getName()     != null ? data.getName().trim() : "";

        String surfaceDesc = SURFACE_DESC.getOrDefault(surface, surface);
        String levelLabel  = resolveLevelLabel(category);
        String angleDesc   = CATEGORY_ANGLE.getOrDefault(category.toLowerCase(),
                CATEGORY_ANGLE.getOrDefault(category, "中角度俯视球场"));

        StringBuilder sb = new StringBuilder();
        sb.append("基于赛事文化背景。生成用于app的网球系列赛赛事卡片的背景图。遵循以下的规则。\n");
        sb.append("1. 要体现出赛事的场地类型（比如红土、草地、硬地、室内、室内红土、室内硬地）\n");
        sb.append("2. 作为三方app展示赛程使用，不要出现赛事名字图标等可能侵权的元素。\n");
        sb.append("3. 需要体现出对应赛事中央球场的特点。\n");
        sb.append("4. 如果你存在数据在图片远景增加举办赛事的城市的特征元素，但是不要太突兀或者显眼，要自然融入。\n");
        sb.append("5. 网球赛事有多个级别 GS、1000、500、250、final 不同级别的赛事要在照片中体现出该赛事的重要程度。").append("\n");
        sb.append("   如果是一些特别的赛事就要和普通的系列赛区分开来，比如年终总决赛。\n");
        sb.append("   - GS: 低空斜俯视（45°）+ 全景球场, 壮观、史诗感，展现观众席规模\n");
        sb.append("   - 1000: 中角度侧俯视（60°）,专业感强，有一定气势\n");
        sb.append("   - 500: 接近球场地面的斜视角, 精致感，聚焦球场本身。\n");
        sb.append("   - 250: 球场局部特写 + 环境虚化, 简洁干净\n");
        sb.append("   - final: 年终总决赛, 夜景 + 室内穹顶俯视, 与所有常规赛事区分，强调\"收官之战\"\n");
        sb.append("6. 图片比例要为16:9\n");
        sb.append("---\n");
        sb.append("赛事名称: ").append(name).append("\n");
        sb.append("级别: ").append(levelLabel).append("\n");
        sb.append("场地类型: ").append(surfaceDesc).append("\n");
        if (!city.isBlank()) {
            sb.append("城市: ").append(city).append("\n");
        }
        sb.append("\n当前赛事拍摄角度参考：").append(angleDesc);

        return sb.toString();
    }

    private String resolveLevelLabel(String category) {
        if (category == null) return "";
        return switch (category.toUpperCase()) {
            case "GS"            -> "GS（大满贯）";
            case "1000"          -> "1000";
            case "500"           -> "500";
            case "250"           -> "250";
            case "FINAL", "FINALS" -> "final（年终总决赛）";
            default              -> category;
        };
    }
}
