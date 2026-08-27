package com.rally.platformconfig.allconfigquery.activity;

import com.rally.db.config.entity.SysConfigPO;
import com.rally.db.config.repository.SysConfigRepository;
import com.rally.domain.system.enums.SystemConfigKey;
import com.rally.domain.system.model.HomeConfigDTO;
import com.rally.domain.system.model.HomeConfigItemDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 业务活动 query-all-config-view：组装全部已登记配置的当前视图。
 */
@Component
@RequiredArgsConstructor
public class QueryAllConfigViewActivity {

    private static final String GLOBAL_SCOPE = "global";

    private final SysConfigRepository sysConfigRepository;

    public HomeConfigDTO execute() {
        // A1 以枚举声明顺序逐项组装；库内未登记 key 不会进入结果。
        return new HomeConfigDTO(Arrays.stream(SystemConfigKey.values())
                .map(this::buildItem)
                .toList());
    }

    private HomeConfigItemDTO buildItem(SystemConfigKey configKey) {
        // A2 仅查询 global 作用域，不转换 value_type，不解析 JSON。
        SysConfigPO stored = sysConfigRepository.findByKeyAndScope(
                configKey.getKey(), GLOBAL_SCOPE);

        // A3 启用记录原样展示库值；不存在或停用则回退默认值。
        boolean overridden = stored != null && Boolean.TRUE.equals(stored.getEnabled());
        String value = overridden ? stored.getConfigValue() : configKey.getDefaultValue();
        int version = stored == null ? 0 : stored.getVersion();
        return new HomeConfigItemDTO(
                configKey.getKey(),
                configKey.getDesc(),
                value,
                configKey.getDefaultValue(),
                version,
                overridden);
    }
}
