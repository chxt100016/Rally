package com.rally.platformconfig.publicconfigquery.activity;

import com.rally.domain.system.SystemConfig;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务活动 query-public-config-map：批量查询当前 JVM 可取得的公共配置值。
 */
@Component
public class QueryPublicConfigMapActivity {

    public Map<String, String> execute(List<String> keys) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String key : keys) {
            // A1/A2：保留 SystemConfig 的原始键、global 键与枚举默认值优先级。
            String value = SystemConfig.getString(key);
            // A3：只交付非 null 值；LinkedHashMap 保留首次插入顺序并允许重复键覆盖。
            if (value != null) {
                result.put(key, value);
            }
        }
        return result;
    }
}
