package com.rally.system;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.db.config.entity.SysConfigPO;
import com.rally.db.config.repository.SysConfigRepository;
import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.system.enums.SystemConfigKey;
import com.rally.domain.system.model.HomeConfigDTO;
import com.rally.domain.system.model.HomeConfigItemDTO;
import com.rally.domain.system.model.HomeConfigUpdateCmd;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/** 首页配置的运营后台读写编排，只开放明确允许修改的首页配置。 */
@Service
@RequiredArgsConstructor
public class HomeConfigAdminAppService {

    private static final String GLOBAL_SCOPE = "global";
    private static final int MAX_CONFIG_LENGTH = 100_000;
    private static final int MAX_POSTERS = 20;
    private static final Set<String> ALLOWED_KEYS = Set.of(SystemConfigKey.HOME_TOURNAMENT_POSTER_CONFIG.getKey(), SystemConfigKey.HOME_POSTER_CONFIG.getKey());

    private final SysConfigRepository sysConfigRepository;

    public HomeConfigDTO get() {
        return new HomeConfigDTO(List.of(buildItem(SystemConfigKey.HOME_TOURNAMENT_POSTER_CONFIG), buildItem(SystemConfigKey.HOME_POSTER_CONFIG)));
    }

    @Transactional
    public HomeConfigDTO update(HomeConfigUpdateCmd cmd) {
        SystemConfigKey configKey = requireAllowedKey(cmd.getKey());
        String normalizedValue = validateAndNormalize(configKey, cmd.getConfigValue());
        SysConfigPO existing = sysConfigRepository.findByKeyAndScope(configKey.getKey(), GLOBAL_SCOPE);
        if (existing == null) {
            if (cmd.getVersion() != 0) {
                throw new BusinessException(BizErrorCode.OPERATION_FAILED, "首页配置已被其他人修改，请刷新后重试");
            }
            SysConfigPO created = new SysConfigPO();
            created.setBizId(IdWorker.getIdStr());
            created.setConfigKey(configKey.getKey());
            created.setConfigValue(normalizedValue);
            created.setValueType("json");
            created.setScope(GLOBAL_SCOPE);
            created.setDescription(configKey.getDesc());
            created.setEnabled(true);
            created.setVersion(1);
            if (!sysConfigRepository.save(created)) {
                throw new BusinessException(BizErrorCode.OPERATION_FAILED, "首页配置保存失败");
            }
        } else if (!sysConfigRepository.updateValueIfVersion(existing.getId(), normalizedValue, configKey.getDesc(), cmd.getVersion())) {
            throw new BusinessException(BizErrorCode.OPERATION_FAILED, "首页配置已被其他人修改，请刷新后重试");
        }
        SystemConfig.init();
        return get();
    }

    private HomeConfigItemDTO buildItem(SystemConfigKey configKey) {
        SysConfigPO stored = sysConfigRepository.findByKeyAndScope(configKey.getKey(), GLOBAL_SCOPE);
        boolean overridden = stored != null && Boolean.TRUE.equals(stored.getEnabled());
        String value = overridden ? stored.getConfigValue() : configKey.getDefaultValue();
        int version = stored == null ? 0 : stored.getVersion();
        return new HomeConfigItemDTO(configKey.getKey(), configKey.getDesc(), value, configKey.getDefaultValue(), version, overridden);
    }

    private SystemConfigKey requireAllowedKey(String key) {
        if (!ALLOWED_KEYS.contains(key)) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, "该配置不允许在首页配置中心修改");
        }
        return SystemConfigKey.getByKey(key);
    }

    private String validateAndNormalize(SystemConfigKey key, String value) {
        if (value.length() > MAX_CONFIG_LENGTH) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, "首页配置内容不能超过 100KB");
        }
        try {
            if (key == SystemConfigKey.HOME_TOURNAMENT_POSTER_CONFIG) {
                JSONObject section = JSON.parseObject(value);
                requireText(section.getString("title"), "赛事海报区标题不能为空");
                requireText(section.getString("subtitle"), "赛事海报区副标题不能为空");
                validatePosters(section.getJSONArray("posters"));
                return JSON.toJSONString(section);
            }
            JSONArray posters = JSON.parseArray(value);
            validatePosters(posters);
            return JSON.toJSONString(posters);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, "首页配置不是有效的 JSON");
        }
    }

    private void validatePosters(JSONArray posters) {
        if (posters == null) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, "海报列表不能为空");
        }
        if (posters.size() > MAX_POSTERS) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, "每个区域最多配置 20 张海报");
        }
        for (int i = 0; i < posters.size(); i++) {
            JSONObject poster = posters.getJSONObject(i);
            if (poster == null) {
                throw new BusinessException(BizErrorCode.PARAM_ERROR, "第 " + (i + 1) + " 张海报格式错误");
            }
            String type = poster.getString("type");
            if (!"NAVIGATE".equals(type) && !"PREVIEW".equals(type)) {
                throw new BusinessException(BizErrorCode.PARAM_ERROR, "第 " + (i + 1) + " 张海报类型只能是 NAVIGATE 或 PREVIEW");
            }
            requireText(poster.getString("image"), "第 " + (i + 1) + " 张海报图片 key 不能为空");
        }
    }

    private void requireText(String value, String message) {
        if (StringUtils.isBlank(value)) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, message);
        }
    }
}
