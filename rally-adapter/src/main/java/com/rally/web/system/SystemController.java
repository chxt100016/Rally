package com.rally.web.system;

import com.rally.domain.config.gateway.ConfigGateway;
import com.rally.domain.tennis.model.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置查询接口。
 * 前端传入 key，返回对应的配置值。
 */
@RestController
@RequestMapping("/system")
public class SystemController {

    @Resource
    private ConfigGateway configGateway;

    /**
     * 根据 key 查询配置值
     *
     * @param key 配置项 key
     * @return 配置值字符串
     */
    @GetMapping("/config")
    public Result<String> getConfig(@RequestParam("key") String key) {
        String value = configGateway.getString(key, null);
        return Result.ok(value);
    }

    /**
     * 批量查询配置值
     *
     * @param keys 配置项 key 列表
     * @return key -> value 映射，不存在的 key 不返回
     */
    @PostMapping("/config/batch")
    public Result<Map<String, String>> batchGetConfig(@RequestBody List<String> keys) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String key : keys) {
            String value = configGateway.getString(key, null);
            if (value != null) {
                result.put(key, value);
            }
        }
        return Result.ok(result);
    }
}
