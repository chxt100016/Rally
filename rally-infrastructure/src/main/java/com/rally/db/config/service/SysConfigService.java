package com.rally.db.config.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rally.db.config.entity.SysConfigPO;
import com.rally.db.config.mapper.SysConfigMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysConfigService extends ServiceImpl<SysConfigMapper, SysConfigPO> {

    public List<SysConfigPO> findAllEnabled() {
        return this.lambdaQuery()
                .eq(SysConfigPO::getEnabled, true)
                .list();
    }

    public SysConfigPO findByKeyAndScope(String configKey, String scope) {
        return this.lambdaQuery()
                .eq(SysConfigPO::getConfigKey, configKey)
                .eq(SysConfigPO::getScope, scope)
                .last("LIMIT 1")
                .one();
    }

    public boolean updateValueIfVersion(Long id, String configValue, String description, int expectedVersion) {
        return this.lambdaUpdate()
                .eq(SysConfigPO::getId, id)
                .eq(SysConfigPO::getVersion, expectedVersion)
                .set(SysConfigPO::getConfigValue, configValue)
                .set(SysConfigPO::getDescription, description)
                .set(SysConfigPO::getEnabled, true)
                .set(SysConfigPO::getVersion, expectedVersion + 1)
                .update();
    }
}
