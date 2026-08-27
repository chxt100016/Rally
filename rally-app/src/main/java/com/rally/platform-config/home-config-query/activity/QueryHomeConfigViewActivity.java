package com.rally.platformconfig.homeconfigquery.activity;

import com.rally.db.config.entity.SysConfigPO;
import com.rally.db.config.repository.SysConfigRepository;
import com.rally.domain.system.enums.SystemConfigKey;
import com.rally.domain.system.model.HomeConfigDTO;
import com.rally.domain.system.model.HomeConfigItemDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 业务活动 query-home-config-view：按固定顺序组装三项首页配置视图。
 */
@Component
@RequiredArgsConstructor
public class QueryHomeConfigViewActivity {

    private static final String GLOBAL_SCOPE = "global";

    private final SysConfigRepository sysConfigRepository;

    public HomeConfigDTO execute() {
        // A1 仅按固定顺序查询三项首页配置，不遍历其他已登记配置。
        return new HomeConfigDTO(List.of(
                buildItem(SystemConfigKey.HOME_LAYOUT_CONFIG),
                buildItem(SystemConfigKey.HOME_TOURNAMENT_POSTER_CONFIG),
                buildItem(SystemConfigKey.HOME_POSTER_CONFIG)
        ));
    }

    private HomeConfigItemDTO buildItem(SystemConfigKey configKey) {
        SysConfigPO stored = sysConfigRepository.findByKeyAndScope(
                configKey.getKey(), GLOBAL_SCOPE);

        // A2 已启用记录保留 raw 库值；不存在或停用时回退枚举默认值。
        boolean overridden = stored != null && Boolean.TRUE.equals(stored.getEnabled());
        String configValue = overridden
                ? stored.getConfigValue()
                : configKey.getDefaultValue();

        // A3 记录存在就保留库内版本（包括停用），否则版本为 0。
        int version = stored == null ? 0 : stored.getVersion();
        return new HomeConfigItemDTO(
                configKey.getKey(),
                configKey.getDesc(),
                configValue,
                configKey.getDefaultValue(),
                version,
                overridden);
    }
}
