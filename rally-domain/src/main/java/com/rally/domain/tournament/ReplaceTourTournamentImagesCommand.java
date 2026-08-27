package com.rally.domain.tour.tournament;

/** C2：成对替换一个已存在职业赛事年度的主图与背景图资源键。 */
public record ReplaceTourTournamentImagesCommand(
        String imagePath,
        String backgroundPath) {
}
