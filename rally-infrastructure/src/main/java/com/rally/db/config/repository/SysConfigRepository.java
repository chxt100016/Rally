package com.rally.db.config.repository;

import com.rally.db.config.entity.SysConfigPO;
import com.rally.db.config.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SysConfigRepository {

    private final SysConfigService sysConfigService;

    public List<SysConfigPO> findAllEnabled() {
        return sysConfigService.findAllEnabled();
    }

    public SysConfigPO findByKeyAndScope(String configKey, String scope) {
        return sysConfigService.findByKeyAndScope(configKey, scope);
    }

    public boolean updateById(SysConfigPO config) {
        return sysConfigService.updateById(config);
    }
}
