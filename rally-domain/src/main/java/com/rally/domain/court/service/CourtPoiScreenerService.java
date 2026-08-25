package com.rally.domain.court.service;

import com.rally.domain.court.model.CourtPoi;
import com.rally.domain.court.model.CourtPoiCluster;
import com.rally.domain.court.model.CourtPoiScreenResult;
import com.rally.domain.utils.GeoUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 场所记录筛选与就近合并领域服务。
 * 只给出预检结论：聚簇只在单次抓取的这一批记录内成立，库中记录的唯一性靠 source_id 唯一约束保证。
 */
@Service
public class CourtPoiScreenerService {

    /** R1 名称或地址含这些关键词的判定为无效 */
    private static final List<String> REJECT_KEYWORDS = List.of("学练馆", "穿线", "培训中心");
    /** R2 推荐标签与关键标签拼接后必须含这个词 */
    private static final String REQUIRED_TAG = "网球场";
    /** R4 球面距离不超过这个米数视为同一处场馆 */
    private static final double MERGE_THRESHOLD_METERS = 100D;
    /** R6 评分为空或无法识别时视为最低 */
    private static final double LOWEST_RATING = -1D;

    public CourtPoiScreenResult screen(List<CourtPoi> pois) {
        CourtPoiScreenResult result = new CourtPoiScreenResult();
        if (pois == null || pois.isEmpty()) {
            return result;
        }
        List<CourtPoi> kept = new ArrayList<>();
        int rejected = 0;
        for (CourtPoi poi : pois) {
            // R3 R1 与 R2 任一命中即丢弃
            if (poi == null || isRejected(poi)) {
                rejected++;
            } else {
                kept.add(poi);
            }
        }
        List<CourtPoiCluster> clusters = merge(kept);
        int merged = 0;
        for (CourtPoiCluster cluster : clusters) {
            merged += cluster.getMerged().size();
        }
        result.setClusters(clusters);
        result.setRejectedCount(rejected);
        result.setMergedCount(merged);
        return result;
    }

    /** R1 R2 有效性判定 */
    private boolean isRejected(CourtPoi poi) {
        String name = StringUtils.defaultString(poi.getName());
        String address = StringUtils.defaultString(poi.getAddress());
        // R1 名称或地址含培训类、穿线类关键词
        for (String keyword : REJECT_KEYWORDS) {
            if (name.contains(keyword) || address.contains(keyword)) {
                return true;
            }
        }
        // R2 推荐标签与关键标签拼接后不含「网球场」
        String tags = StringUtils.defaultString(poi.getRectag()) + StringUtils.defaultString(poi.getKeytag());
        return !tags.contains(REQUIRED_TAG);
    }

    /** R4 R5 R6 R7 就近聚簇并挑主记录 */
    private List<CourtPoiCluster> merge(List<CourtPoi> pois) {
        int size = pois.size();
        int[] parent = new int[size];
        for (int i = 0; i < size; i++) {
            parent[i] = i;
        }
        double[][] coords = new double[size][];
        for (int i = 0; i < size; i++) {
            coords[i] = parseLocation(pois.get(i).getLocation());
        }
        for (int i = 0; i < size; i++) {
            // R7 经纬度缺失的不参与聚簇，各自成簇
            if (coords[i] == null) {
                continue;
            }
            for (int j = i + 1; j < size; j++) {
                if (coords[j] == null) {
                    continue;
                }
                double meters = GeoUtils.distance(coords[i][1], coords[i][0], coords[j][1], coords[j][0]) * 1000D;
                // R4 距离不超过阈值判定为同一处场馆
                if (meters <= MERGE_THRESHOLD_METERS) {
                    // R5 同簇关系可传递，靠并查集合并
                    union(parent, i, j);
                }
            }
        }
        Map<Integer, List<CourtPoi>> grouped = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            grouped.computeIfAbsent(find(parent, i), k -> new ArrayList<>()).add(pois.get(i));
        }
        List<CourtPoiCluster> clusters = new ArrayList<>();
        for (List<CourtPoi> group : grouped.values()) {
            // R6 主记录取评分最高的一条，评分为空视为最低
            group.sort(Comparator.comparingDouble(CourtPoiScreenerService::ratingOf).reversed());
            clusters.add(new CourtPoiCluster(group.get(0), new ArrayList<>(group.subList(1, group.size()))));
        }
        return clusters;
    }

    private static double ratingOf(CourtPoi poi) {
        try {
            return Double.parseDouble(StringUtils.trimToEmpty(poi.getRating()));
        } catch (Exception e) {
            return LOWEST_RATING;
        }
    }

    /** 返回 [经度, 纬度]，无法解析时返回 null */
    private static double[] parseLocation(String location) {
        if (StringUtils.isBlank(location)) {
            return null;
        }
        String[] parts = location.split(",");
        if (parts.length != 2) {
            return null;
        }
        try {
            return new double[]{Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim())};
        } catch (Exception e) {
            return null;
        }
    }

    private static int find(int[] parent, int x) {
        while (parent[x] != x) {
            parent[x] = parent[parent[x]];
            x = parent[x];
        }
        return x;
    }

    private static void union(int[] parent, int a, int b) {
        int rootA = find(parent, a);
        int rootB = find(parent, b);
        if (rootA != rootB) {
            parent[rootA] = rootB;
        }
    }
}
