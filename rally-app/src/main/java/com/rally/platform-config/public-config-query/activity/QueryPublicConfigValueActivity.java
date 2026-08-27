package com.rally.platformconfig.publicconfigquery.activity;

import com.rally.domain.system.SystemConfig;
import org.springframework.stereotype.Component;

/**
 * 业务活动 query-public-config-value：按原始标识查询当前 JVM 可取得的配置字符串。
 */
@Component
public class QueryPublicConfigValueActivity {

    public String execute(String key) {
        // A1/A2/A3：SystemConfig 保留原始键、global 键与枚举默认值的既有优先级。
        return SystemConfig.getString(key);
    }
}
