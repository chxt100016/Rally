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

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/** 运营后台的首页与系统配置读写编排。 */
@Service
@RequiredArgsConstructor
public class HomeConfigAdminAppService {

    private static final String GLOBAL_SCOPE = "global";
    private static final int MAX_CONFIG_LENGTH = 100_000;
    private static final int MAX_POSTERS = 20;
    private static final int MAX_HOME_SECTIONS = 30;
    private static final Set<String> HOME_KEYS = Set.of(
            SystemConfigKey.HOME_LAYOUT_CONFIG.getKey(),
            SystemConfigKey.HOME_TOURNAMENT_POSTER_CONFIG.getKey(),
            SystemConfigKey.HOME_POSTER_CONFIG.getKey()
    );
    private static final Set<String> HOME_SECTION_TYPES = Set.of(
            "MEETUP", "TOURNAMENT_POSTER", "TOUR_MATCH", "COURT_POSTER", "POSTER", "NEWS"
    );

    private final SysConfigRepository sysConfigRepository;

    public HomeConfigDTO get() {
        return new HomeConfigDTO(List.of(
                buildItem(SystemConfigKey.HOME_LAYOUT_CONFIG),
                buildItem(SystemConfigKey.HOME_TOURNAMENT_POSTER_CONFIG),
                buildItem(SystemConfigKey.HOME_POSTER_CONFIG)
        ));
    }

    public HomeConfigDTO getAll() {
        return new HomeConfigDTO(Arrays.stream(SystemConfigKey.values()).map(this::buildItem).toList());
    }

    @Transactional
    public HomeConfigDTO update(HomeConfigUpdateCmd cmd) {
        if (!HOME_KEYS.contains(cmd.getKey())) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, "该配置不允许在首页配置中心修改");
        }
        return save(cmd, true);
    }

    @Transactional
    public HomeConfigDTO updateAny(HomeConfigUpdateCmd cmd) {
        return save(cmd, false);
    }

    private HomeConfigDTO save(HomeConfigUpdateCmd cmd, boolean homeOnly) {
        SystemConfigKey configKey = requireKnownKey(cmd.getKey());
        String normalizedValue = validateAndNormalize(configKey, cmd.getConfigValue());
        SysConfigPO existing = sysConfigRepository.findByKeyAndScope(configKey.getKey(), GLOBAL_SCOPE);
        if (existing == null) {
            if (cmd.getVersion() != 0) {
                throw new BusinessException(BizErrorCode.OPERATION_FAILED, "配置已被其他人修改，请刷新后重试");
            }
            SysConfigPO created = new SysConfigPO();
            created.setBizId(IdWorker.getIdStr());
            created.setConfigKey(configKey.getKey());
            created.setConfigValue(normalizedValue);
            created.setValueType(valueType(configKey));
            created.setScope(GLOBAL_SCOPE);
            created.setDescription(configKey.getDesc());
            created.setEnabled(true);
            created.setVersion(1);
            if (!sysConfigRepository.save(created)) {
                throw new BusinessException(BizErrorCode.OPERATION_FAILED, "配置保存失败");
            }
        } else if (!sysConfigRepository.updateValueIfVersion(existing.getId(), normalizedValue, configKey.getDesc(), cmd.getVersion())) {
            throw new BusinessException(BizErrorCode.OPERATION_FAILED, "配置已被其他人修改，请刷新后重试");
        }
        SystemConfig.init();
        return homeOnly ? get() : getAll();
    }

    private HomeConfigItemDTO buildItem(SystemConfigKey configKey) {
        SysConfigPO stored = sysConfigRepository.findByKeyAndScope(configKey.getKey(), GLOBAL_SCOPE);
        boolean overridden = stored != null && Boolean.TRUE.equals(stored.getEnabled());
        String value = overridden ? stored.getConfigValue() : configKey.getDefaultValue();
        int version = stored == null ? 0 : stored.getVersion();
        return new HomeConfigItemDTO(configKey.getKey(), configKey.getDesc(), value, configKey.getDefaultValue(), version, overridden);
    }

    private SystemConfigKey requireKnownKey(String key) {
        SystemConfigKey configKey = SystemConfigKey.getByKey(key);
        if (configKey == null) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, "未知的系统配置 key");
        }
        return configKey;
    }

    private String validateAndNormalize(SystemConfigKey key, String value) {
        if (value.length() > MAX_CONFIG_LENGTH) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, "配置内容不能超过 100KB");
        }
        try {
            if (key == SystemConfigKey.HOME_LAYOUT_CONFIG) {
                JSONArray sections = JSON.parseArray(value);
                validateHomeSections(sections);
                return JSON.toJSONString(sections);
            }
            if (key == SystemConfigKey.HOME_TOURNAMENT_POSTER_CONFIG) {
                JSONObject section = JSON.parseObject(value);
                requireText(section.getString("title"), "赛事海报区标题不能为空");
                requireText(section.getString("subtitle"), "赛事海报区副标题不能为空");
                validatePosters(section.getJSONArray("posters"));
                return JSON.toJSONString(section);
            }
            if (key == SystemConfigKey.HOME_POSTER_CONFIG) {
                JSONArray posters = JSON.parseArray(value);
                validatePosters(posters);
                return JSON.toJSONString(posters);
            }
            validateScalar(key, value);
            return value;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, "配置格式无效");
        }
    }

    private void validateHomeSections(JSONArray sections) {
        if (sections == null) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, "首页区域配置不能为空");
        }
        if (sections.size() > MAX_HOME_SECTIONS) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, "首页最多配置 30 个区域");
        }
        Set<String> ids = new java.util.HashSet<>();
        Set<String> singletonTypes = new java.util.HashSet<>();
        for (int i = 0; i < sections.size(); i++) {
            JSONObject section = sections.getJSONObject(i);
            if (section == null) {
                throw new BusinessException(BizErrorCode.PARAM_ERROR, "第 " + (i + 1) + " 个首页区域格式错误");
            }
            String id = section.getString("id");
            String type = section.getString("type");
            requireText(id, "第 " + (i + 1) + " 个首页区域 id 不能为空");
            if (id.length() > 64 || !id.matches("[A-Za-z0-9_-]+")) {
                throw new BusinessException(BizErrorCode.PARAM_ERROR, "首页区域 id 只能包含字母、数字、下划线和中划线，且不能超过 64 位");
            }
            if (!ids.add(id)) {
                throw new BusinessException(BizErrorCode.PARAM_ERROR, "首页区域 id 不能重复：" + id);
            }
            if (!HOME_SECTION_TYPES.contains(type)) {
                throw new BusinessException(BizErrorCode.PARAM_ERROR, "不支持的首页区域类型：" + type);
            }
            if (!"POSTER".equals(type) && !singletonTypes.add(type)) {
                throw new BusinessException(BizErrorCode.PARAM_ERROR, "同一种动态首页区域只能配置一次：" + type);
            }
            if ("POSTER".equals(type)) {
                requireText(section.getString("title"), "自定义海报区标题不能为空");
                validatePosters(section.getJSONArray("posters"));
            }
        }
    }

    private void validateScalar(SystemConfigKey key, String value) {
        String defaultValue = key.getDefaultValue();
        if (defaultValue.matches("-?\\d+")) {
            try {
                Long.parseLong(value);
            } catch (NumberFormatException e) {
                throw new BusinessException(BizErrorCode.PARAM_ERROR, key.getDesc() + "必须是整数");
            }
        } else if (defaultValue.matches("-?\\d+\\.\\d+")) {
            try {
                new java.math.BigDecimal(value);
            } catch (NumberFormatException e) {
                throw new BusinessException(BizErrorCode.PARAM_ERROR, key.getDesc() + "必须是数字");
            }
        }
    }

    private String valueType(SystemConfigKey key) {
        if (HOME_KEYS.contains(key.getKey())) {
            return "json";
        }
        String defaultValue = key.getDefaultValue();
        if (defaultValue.matches("-?\\d+")) {
            return "integer";
        }
        if (defaultValue.matches("-?\\d+\\.\\d+")) {
            return "decimal";
        }
        return "string";
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
