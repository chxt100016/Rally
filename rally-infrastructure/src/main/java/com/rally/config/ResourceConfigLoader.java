package com.rally.config;

import com.alibaba.fastjson2.JSON;
import com.rally.domain.system.model.Location;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CityLoader 的本地资源实现。
 * 从 classpath 下的 city.json 全量加载城市数据。
 */
@Slf4j
@Component
public class ResourceConfigLoader {


    public List<Location> city() {
        return load("city.json", "城市");
    }

    public List<Location> district() {
        return load("district.json", "区县");
    }

    private List<Location> load(String resourceName, String catalogName) {
        try (InputStream is = new ClassPathResource(resourceName).getInputStream()) {
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            List<Location> locations = JSON.parseArray(content, Location.class);
            if (locations == null || locations.isEmpty()) {
                return List.of();
            }

            // 同一编码重复时保留名录中第一条，也避免后续转 Map 时抛错。
            Map<String, Location> firstByCode = new LinkedHashMap<>();
            for (Location location : locations) {
                if (location != null && location.getCode() != null) {
                    firstByCode.putIfAbsent(location.getCode(), location);
                }
            }
            return new ArrayList<>(firstByCode.values());
        } catch (Exception e) {
            log.error("{}数据加载失败", catalogName, e);
            return List.of();
        }
    }
}
