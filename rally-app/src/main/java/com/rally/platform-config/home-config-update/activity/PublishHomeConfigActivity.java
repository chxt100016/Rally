package com.rally.platformconfig.homeconfigupdate.activity;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.system.enums.SystemConfigKey;
import com.rally.domain.system.model.HomeConfigUpdateCmd;
import com.rally.platformconfig.globalconfigupdate.activity.PublishGlobalConfigActivity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * 业务活动 publish-home-config：限定首页配置范围后复用 global 发布流程。
 */
@Component
@RequiredArgsConstructor
public class PublishHomeConfigActivity {

    private static final Set<String> HOME_KEYS = Set.of(
            SystemConfigKey.HOME_LAYOUT_CONFIG.getKey(),
            SystemConfigKey.HOME_TOURNAMENT_POSTER_CONFIG.getKey(),
            SystemConfigKey.HOME_POSTER_CONFIG.getKey());

    private final PublishGlobalConfigActivity publishGlobalConfigActivity;

    @Transactional
    public void execute(HomeConfigUpdateCmd cmd) {
        // A1 首页入口只允许三项配置；JSON 规则和紧凑化复用全局发布同一实现。
        if (!HOME_KEYS.contains(cmd.getKey())) {
            throw new BusinessException(
                    BizErrorCode.PARAM_ERROR,
                    "该配置不允许在首页配置中心修改");
        }

        // A2/A3 保持首次发布、id+version CAS 更新与当前 JVM 缓存重建语义。
        publishGlobalConfigActivity.execute(cmd);
    }
}
