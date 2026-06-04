package com.rally.config;

import com.rally.db.config.entity.SysConfigPO;
import com.rally.db.config.repository.SysConfigRepository;
import com.rally.domain.system.gateway.SysConfigLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SysConfigLoader 的 MySQL 实现。
 * 从 sys_config 表全量加载启用的配置项。
 */
@Component
@RequiredArgsConstructor
public class SysConfigLoaderImpl implements SysConfigLoader {

    private final SysConfigRepository repository;

    @Override
    public Map<String, String> loadAll() {
        List<SysConfigPO> allEnabled = repository.findAllEnabled();
        Map<String, String> result = new HashMap<>();
        for (SysConfigPO po : allEnabled) {
            String cacheKey = po.getScope() + "|" + po.getConfigKey();
            result.put(cacheKey, po.getConfigValue());
        }
        return result;
    }
}
