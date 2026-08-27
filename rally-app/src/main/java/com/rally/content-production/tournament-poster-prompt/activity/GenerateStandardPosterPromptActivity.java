package com.rally.contentproduction.tournamentposterprompt.activity;

import com.rally.domain.tour.TourTournamentQueryDomainService;
import com.rally.domain.tour.model.TournamentData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 业务活动 generate-standard-poster-prompt：按职业赛事资料生成通用背景图提示词。
 */
@Component
@RequiredArgsConstructor
public class GenerateStandardPosterPromptActivity {

    private static final Map<String, String> CATEGORY_ANGLE = Map.of(
            "GS", "低空斜俯视（45°）+ 全景球场，壮观史诗感，展现观众席规模",
            "1000", "中角度侧俯视（60°），专业感强，有一定气势",
            "500", "接近球场地面的斜视角，精致感，聚焦球场本身",
            "250", "球场局部特写 + 环境虚化，简洁干净",
            "final", "夜景 + 室内穹顶俯视，与所有常规赛事区分，强调收官之战氛围",
            "finals", "夜景 + 室内穹顶俯视，与所有常规赛事区分，强调收官之战氛围"
    );

    private static final Map<String, String> SURFACE_DESC = Map.of(
            "clay", "红土",
            "grass", "草地",
            "hard", "硬地",
            "indoor", "室内硬地",
            "indoor clay", "室内红土",
            "indoor hard", "室内硬地"
    );

    private final TourTournamentQueryDomainService tournamentQueryService;

    public String execute(String tournamentId) {
        // A1 仅按赛事编号取仓储返回的第一条，不附加年份或排序。
        TournamentData tournament = tournamentQueryService.findByTournamentId(tournamentId);
        if (tournament == null) {
            return null;
        }

        // A2 按既有规则规范化级别、场地与城市展示值。
        String category = tournament.getCategory() != null ? tournament.getCategory().trim() : "";
        String surface = tournament.getSurface() != null ? tournament.getSurface().trim().toLowerCase() : "";
        String city = tournament.getCity() != null ? tournament.getCity().trim() : "";
        String name = tournament.getName() != null ? tournament.getName().trim() : "";

        String surfaceDesc = SURFACE_DESC.getOrDefault(surface, surface);
        String levelLabel = resolveLevelLabel(category);
        String angleDesc = CATEGORY_ANGLE.getOrDefault(category.toLowerCase(),
                CATEGORY_ANGLE.getOrDefault(category, "中角度俯视球场"));

        // A3 保持主线通用背景图规则、赛事资料与建议角度的输出文本不变。
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
        prompt.append("场地类型: ").append(surfaceDesc).append("\n");
        if (!city.isBlank()) {
            prompt.append("城市: ").append(city).append("\n");
        }
        prompt.append("\n当前赛事拍摄角度参考：").append(angleDesc);
        return prompt.toString();
    }

    private String resolveLevelLabel(String category) {
        return switch (category.toUpperCase()) {
            case "GS" -> "GS（大满贯）";
            case "1000" -> "1000";
            case "500" -> "500";
            case "250" -> "250";
            case "FINAL", "FINALS" -> "final（年终总决赛）";
            default -> category;
        };
    }
}
