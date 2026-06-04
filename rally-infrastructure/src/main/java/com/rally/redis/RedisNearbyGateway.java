package com.rally.redis;

import com.rally.domain.meetup.gateway.NearbyGateway;
import com.rally.domain.meetup.model.NearbyResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Redis GEO 距离排序网关实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisNearbyGateway implements NearbyGateway {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String KEY_PREFIX = "nearby:meetup:";

    @Override
    public void add(String cityCode, String meetupId, double lng, double lat) {
        try {
            String key = KEY_PREFIX + cityCode;
            Long result = stringRedisTemplate.opsForGeo()
                    .add(key, new RedisGeoCommands.GeoLocation<>(meetupId,
                            new Point(lng, lat)));
            log.debug("GEOADD {} {} {} {} result: {}", key, lng, lat, meetupId, result);
        } catch (Exception e) {
            log.warn("GEOADD 失败: cityCode={}, meetupId={}, error={}", cityCode, meetupId, e.getMessage());
        }
    }

    @Override
    public void remove(String cityCode, String meetupId) {
        try {
            String key = KEY_PREFIX + cityCode;
            Long result = stringRedisTemplate.opsForGeo().remove(key, meetupId);
            log.debug("GEODEL {} {} result: {}", key, meetupId, result);
        } catch (Exception e) {
            log.warn("GEODEL 失败: cityCode={}, meetupId={}, error={}", cityCode, meetupId, e.getMessage());
        }
    }

    @Override
    public List<NearbyResult> searchByRadius(String cityCode, double lng, double lat, double radiusMeters) {
        try {
            String key = KEY_PREFIX + cityCode;
            Circle circle = new Circle(new Point(lng, lat), new Distance(radiusMeters / 1000.0, Metrics.KILOMETERS));
            GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo()
                    .radius(key, circle,
                            RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                                    .includeDistance()
                                    .sortAscending());

            if (results == null) {
                return List.of();
            }

            return results.getContent().stream()
                    .map(content -> {
                        NearbyResult result = new NearbyResult();
                        result.setMeetupId(content.getContent().getName());
                        result.setDistanceMeters(content.getDistance().getValue());
                        return result;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("GEOSEARCH 失败: cityCode={}, error={}", cityCode, e.getMessage());
            return List.of();
        }
    }

    @Override
    public Set<String> members(String cityCode) {
        try {
            String key = KEY_PREFIX + cityCode;
            // 使用 ZRANGE 获取所有成员
            Set<String> members = stringRedisTemplate.opsForZSet().range(key, 0, -1);
            return members != null ? members : Set.of();
        } catch (Exception e) {
            log.warn("ZRANGE 失败: cityCode={}, error={}", cityCode, e.getMessage());
            return Set.of();
        }
    }
}
