package com.rally.config;

import com.alibaba.fastjson2.JSON;
import com.rally.domain.system.model.Location;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * CityLoader 的本地资源实现。
 * 从 classpath 下的 city.json 全量加载城市数据。
 */
@Slf4j
@Component
public class ResourceConfigLoader {


    public List<Location> city() {
        try (InputStream is = new ClassPathResource("city.json").getInputStream()) {
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return JSON.parseArray(content, Location.class);
        } catch (Exception e) {
            log.error("城市数据加载失败", e);
            return List.of();
        }
    }

    public List<Location> district() {
        try (InputStream is = new ClassPathResource("district.json").getInputStream()) {
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return JSON.parseArray(content, Location.class);
        } catch (Exception e) {
            log.error("城市数据加载失败", e);
            return List.of();
        }
    }
}
