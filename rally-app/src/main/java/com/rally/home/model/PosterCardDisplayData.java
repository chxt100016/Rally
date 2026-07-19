package com.rally.home.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class PosterCardDisplayData extends BaseDisplayData {
    private List<PosterCardItem> posters;

    @Data
    public static class PosterCardItem {
        private PosterType type;
        private String imageUrl;
        private String title;
        private String subtitle;
        private String wechatUrl;
        private String appUrl;
        private String webUrl;
    }

    public enum PosterType {
        NAVIGATE,
        PREVIEW
    }
}
