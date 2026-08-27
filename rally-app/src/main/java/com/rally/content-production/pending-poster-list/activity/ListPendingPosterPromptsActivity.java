package com.rally.contentproduction.pendingposterlist.activity;

import com.rally.domain.tour.model.TournamentData;
import com.rally.domain.tour.repository.TourTournamentRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 业务活动 list-pending-poster-prompts：生成近期缺少背景图的职业赛事海报提示词合集。
 */
@Component
@RequiredArgsConstructor
public class ListPendingPosterPromptsActivity {

    private static final String PROMPT_SEPARATOR = "\n\n---\n\n";

    private static final Map<String, String> CATEGORY_ANGLE = Map.of(
            "GS", "低空斜俯视（45°）+ 全景球场，壮观史诗感，展现观众席规模",
            "1000", "中角度侧俯视（60°），专业感强，有一定气势",
            "500", "接近球场地面的斜视角，精致感，聚焦球场本身",
            "250", "球场局部特写 + 环境虚化，简洁干净",
            "final", "夜景 + 室内穹顶俯视，与所有常规赛事区分，强调收官之战氛围",
            "finals", "夜景 + 室内穹顶俯视，与所有常规赛事区分，强调收官之战氛围");

    private static final Map<String, String> SURFACE_DESCRIPTION = Map.of(
            "clay", "红土",
            "grass", "草地",
            "hard", "硬地",
            "indoor", "室内硬地",
            "indoor clay", "室内红土",
            "indoor hard", "室内硬地");

    private final TourTournamentRepository tournamentRepository;

    public String execute() {
        // A1 整次请求使用同一运行日，计算前后各一个月的闭合窗口。
        LocalDate runDate = LocalDate.now();
        LocalDate dateFrom = runDate.minusMonths(1);
        LocalDate dateTo = runDate.plusMonths(1);

        // A2 既有仓储按开始日期升序返回窗口相交且 background_path 为 NULL 或空串的赛事。
        List<TournamentData> candidates = tournamentRepository.listPendingBackground(dateFrom, dateTo);
        if (CollectionUtils.isEmpty(candidates)) {
            return "";
        }

        // A3/A4 规范化素材并过滤低级别数字赛事，保留查询顺序构造、拼接提示词。
        return candidates.stream()
                .filter(this::isCategoryKept)
                .map(this::buildPrompt)
                .collect(Collectors.joining(PROMPT_SEPARATOR));
    }

    private boolean isCategoryKept(TournamentData tournament) {
        String category = tournament.getCategory();
        if (category == null || category.isBlank()) {
            return true;
        }
        try {
            return Integer.parseInt(category.trim()) >= 250;
        } catch (NumberFormatException exception) {
            return true;
        }
    }

    private String buildPrompt(TournamentData tournament) {
        String category = normalize(tournament.getCategory());
        String surface = normalize(tournament.getSurface()).toLowerCase(Locale.ROOT);
        String city = normalize(tournament.getCity());
        String name = normalize(tournament.getName());

        String surfaceDescription = SURFACE_DESCRIPTION.getOrDefault(surface, surface);
        String levelLabel = resolveLevelLabel(category);
        String angleDescription = CATEGORY_ANGLE.getOrDefault(
                category.toLowerCase(Locale.ROOT),
                CATEGORY_ANGLE.getOrDefault(category, "中角度俯视球场"));

        StringBuilder prompt = new StringBuilder();
        prompt.append("基于赛事文化背景。生成用于app的网球系列赛赛事卡片的背景图。遵循以下的规则。\n");
        prompt.append("1. 要体现出赛事的场地类型（比如红土、草地、硬地、室内、室内红土、室内硬地）\n");
        prompt.append("2. 作为三方app展示赛程使用，不要出现赛事名字图标等可能侵权的元素。\n");
        prompt.append("3. 需要体现出对应赛事中央球场的特点。\n");
        prompt.append("4. 如果你存在数据在图片远景增加举办赛事的城市的特征元素，但是不要太突兀或者显眼，要自然融入。\n");
        prompt.append("5. 网球赛事有多个级别 GS、1000、500、250、final 不同级别的赛事要在照片中体现出该赛事的重要程度。\n");
        prompt.append("   如果是一些特别的赛事就要和普通的系列赛区分开来，比如年终总决赛。\n");
        prompt.append("   - GS: 低空斜俯视（45°）+ 全景球场, 壮观、史诗感，展现观众席规模\n");
        prompt.append("   - 1000: 中角度侧俯视（60°）,专业感强，有一定气势\n");
        prompt.append("   - 500: 接近球场地面的斜视角, 精致感，聚焦球场本身。\n");
        prompt.append("   - 250: 球场局部特写 + 环境虚化, 简洁干净\n");
        prompt.append("   - final: 年终总决赛, 夜景 + 室内穹顶俯视, 与所有常规赛事区分，强调\"收官之战\"\n");
        prompt.append("6. 图片比例要为16:9\n");
        prompt.append("---\n");
        prompt.append("赛事名称: ").append(name).append("\n");
        prompt.append("级别: ").append(levelLabel).append("\n");
        prompt.append("场地类型: ").append(surfaceDescription).append("\n");
        if (!city.isBlank()) {
            prompt.append("城市: ").append(city).append("\n");
        }
        prompt.append("\n当前赛事拍摄角度参考：").append(angleDescription);
        return prompt.toString();
    }

    private String resolveLevelLabel(String category) {
        return switch (category.toUpperCase(Locale.ROOT)) {
            case "GS" -> "GS（大满贯）";
            case "1000" -> "1000";
            case "500" -> "500";
            case "250" -> "250";
            case "FINAL", "FINALS" -> "final（年终总决赛）";
            default -> category;
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
