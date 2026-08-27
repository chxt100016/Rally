package com.rally.domain.tour.tournament;

/** 必须成对存在、成对替换的赛事展示图片资源键。 */
public record TourTournamentImageBinding(String imagePath, String backgroundPath) {

    private static final int RESOURCE_KEY_MAX_LENGTH = 255;

    public TourTournamentImageBinding {
        imagePath = normalizeStored(imagePath);
        backgroundPath = normalizeStored(backgroundPath);
        require(imagePath.isEmpty() == backgroundPath.isEmpty(),
                "主图与背景图资源键必须成对存在");
    }

    public static TourTournamentImageBinding empty() {
        return new TourTournamentImageBinding("", "");
    }

    public static TourTournamentImageBinding replacement(
            ReplaceTourTournamentImagesCommand command) {
        require(command != null, "替换赛事图片命令不能为空");
        return new TourTournamentImageBinding(
                required(command.imagePath(), "主图资源键"),
                required(command.backgroundPath(), "背景图资源键"));
    }

    public static TourTournamentImageBinding restore(
            String imagePath, String backgroundPath) {
        String normalizedImage = normalizeStored(imagePath);
        String normalizedBackground = normalizeStored(backgroundPath);
        require(normalizedImage.isEmpty() == normalizedBackground.isEmpty(),
                "主图与背景图资源键必须成对存在");
        return new TourTournamentImageBinding(normalizedImage, normalizedBackground);
    }

    public boolean isEmpty() {
        return imagePath.isEmpty();
    }

    private static String required(String value, String fieldName) {
        require(value != null && !value.isBlank(), fieldName + "不能为空");
        String normalized = value.strip();
        require(normalized.length() <= RESOURCE_KEY_MAX_LENGTH,
                fieldName + "长度不能超过 255");
        return normalized;
    }

    private static String normalizeStored(String value) {
        String normalized = value == null ? "" : value.strip();
        require(normalized.length() <= RESOURCE_KEY_MAX_LENGTH,
                "图片资源键长度不能超过 255");
        return normalized;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new TourTournamentDomainException(
                    TourTournament.TOUR_TOURNAMENT_IMAGE_BINDING_INVALID,
                    message);
        }
    }
}
