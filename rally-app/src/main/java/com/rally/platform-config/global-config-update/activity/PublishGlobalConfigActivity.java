package com.rally.platformconfig.globalconfigupdate.activity;

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
import com.rally.domain.system.model.HomeConfigUpdateCmd;
import com.rally.domain.system.platformconfig.ConfigIdentity;
import com.rally.domain.system.platformconfig.PlatformConfig;
import com.rally.domain.system.platformconfig.PlatformConfigDefinition;
import com.rally.domain.system.platformconfig.PlatformConfigDomainException;
import com.rally.domain.system.platformconfig.PlatformConfigInsertResult;
import com.rally.domain.system.platformconfig.PlatformConfigPersistence;
import com.rally.domain.system.platformconfig.PlatformConfigState;
import com.rally.domain.system.platformconfig.PublishPlatformConfigCommand;
import com.rally.domain.system.platformconfig.TypedConfigValue;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 业务活动 publish-global-config：发布一项 global 配置并重建当前 JVM 缓存。
 */
@Component
@RequiredArgsConstructor
public class PublishGlobalConfigActivity {

    private static final String GLOBAL_SCOPE = "global";
    private static final int MAX_CONFIG_LENGTH = 100_000;
    private static final int MAX_POSTERS = 20;
    private static final int MAX_HOME_SECTIONS = 30;
    private static final Set<String> HOME_KEYS = Set.of(
            SystemConfigKey.HOME_LAYOUT_CONFIG.getKey(),
            SystemConfigKey.HOME_TOURNAMENT_POSTER_CONFIG.getKey(),
            SystemConfigKey.HOME_POSTER_CONFIG.getKey());
    private static final Set<String> HOME_SECTION_TYPES = Set.of(
            "MEETUP", "TOURNAMENT_POSTER", "TOUR_MATCH", "COURT_POSTER", "POSTER", "NEWS");

    private final SysConfigRepository sysConfigRepository;

    @Transactional
    public void execute(HomeConfigUpdateCmd cmd) {
        SystemConfigKey configKey = requireKnownKey(cmd.getKey());

        // A1 先按当前名录校验并规范化，保留 main 的 100000 字符上限与错误提示。
        String normalizedValue = validateAndNormalize(configKey, cmd.getConfigValue());
        PlatformConfigDefinition definition = new CatalogDefinition(configKey);
        PublishPlatformConfigCommand command = prepareCommand(
                definition, normalizedValue, configKey.getDesc(), cmd.getVersion());

        // A2 不存在时由聚合建立 version=1 的新记录；已存在时按 id+version CAS 发布。
        PlatformConfigPersistence persistence = persistence();
        SysConfigPO existing = sysConfigRepository.findByKeyAndScope(configKey.getKey(), GLOBAL_SCOPE);
        try {
            if (existing == null) {
                PlatformConfig.firstPublish(command, IdWorker::getIdStr, persistence);
            } else {
                PlatformConfig.restore(toState(existing), definition).publish(command, persistence);
            }
        } catch (PlatformConfigDomainException exception) {
            throw toBusinessException(exception);
        }

        // A3 写库后仍在同一事务中全量重建当前 JVM，不广播也不做回滚补偿。
        SystemConfig.init();
    }

    private PublishPlatformConfigCommand prepareCommand(
            PlatformConfigDefinition definition,
            String normalizedValue,
            String description,
            Integer expectedVersion) {
        try {
            return PublishPlatformConfigCommand.prepare(
                    definition, GLOBAL_SCOPE, normalizedValue, description, expectedVersion);
        } catch (PlatformConfigDomainException exception) {
            throw toBusinessException(exception);
        }
    }

    private PlatformConfigPersistence persistence() {
        return new PlatformConfigPersistence() {
            @Override
            public PlatformConfigInsertResult insert(PlatformConfigState state) {
                SysConfigPO created = toPO(state);
                if (!sysConfigRepository.save(created)) {
                    throw new BusinessException(BizErrorCode.OPERATION_FAILED, "配置保存失败");
                }
                return PlatformConfigInsertResult.created(created.getId());
            }

            @Override
            public boolean publishIfVersion(
                    long id,
                    int expectedVersion,
                    String normalizedValue,
                    String description) {
                // 既有 SQL 只改值、说明、enabled 和版本，不重写 value_type。
                return sysConfigRepository.updateValueIfVersion(
                        id, normalizedValue, description, expectedVersion);
            }

            @Override
            public boolean disableIfVersion(long id, int expectedVersion) {
                throw new UnsupportedOperationException("发布活动不执行停用命令");
            }
        };
    }

    private SysConfigPO toPO(PlatformConfigState state) {
        SysConfigPO po = new SysConfigPO();
        po.setBizId(state.bizId());
        po.setConfigKey(state.configKey());
        po.setConfigValue(state.configValue());
        po.setValueType(state.valueType());
        po.setScope(state.scope());
        po.setDescription(state.description());
        po.setEnabled(state.enabled());
        po.setVersion(state.version());
        return po;
    }

    private PlatformConfigState toState(SysConfigPO po) {
        return new PlatformConfigState(
                po.getId(),
                po.getBizId(),
                new ConfigIdentity(po.getConfigKey(), po.getScope()),
                new TypedConfigValue(po.getValueType(), po.getConfigValue()),
                po.getDescription(),
                Boolean.TRUE.equals(po.getEnabled()),
                po.getVersion());
    }

    private BusinessException toBusinessException(PlatformConfigDomainException exception) {
        return switch (exception.getErrorIdentifier()) {
            case PlatformConfig.CONFIG_VALUE_INVALID, PlatformConfig.CONFIG_VALUE_TOO_LONG ->
                    new BusinessException(BizErrorCode.PARAM_ERROR, exception.getMessage());
            case PlatformConfig.CONFIG_VERSION_CONFLICT, PlatformConfig.CONFIG_IDENTITY_CONFLICT ->
                    new BusinessException(
                            BizErrorCode.OPERATION_FAILED,
                            "配置已被其他人修改，请刷新后重试");
            default -> throw exception;
        };
    }

    private SystemConfigKey requireKnownKey(String key) {
        SystemConfigKey configKey = SystemConfigKey.getByKey(key);
        if (configKey == null) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, "未知的系统配置 key");
        }
        return configKey;
    }

    private static String validateAndNormalize(SystemConfigKey key, String value) {
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
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, "配置格式无效");
        }
    }

    private static void validateHomeSections(JSONArray sections) {
        if (sections == null) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, "首页区域配置不能为空");
        }
        if (sections.size() > MAX_HOME_SECTIONS) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, "首页最多配置 30 个区域");
        }
        Set<String> ids = new HashSet<>();
        Set<String> singletonTypes = new HashSet<>();
        for (int index = 0; index < sections.size(); index++) {
            JSONObject section = sections.getJSONObject(index);
            if (section == null) {
                throw new BusinessException(
                        BizErrorCode.PARAM_ERROR,
                        "第 " + (index + 1) + " 个首页区域格式错误");
            }
            String id = section.getString("id");
            String type = section.getString("type");
            requireText(id, "第 " + (index + 1) + " 个首页区域 id 不能为空");
            if (id.length() > 64 || !id.matches("[A-Za-z0-9_-]+")) {
                throw new BusinessException(
                        BizErrorCode.PARAM_ERROR,
                        "首页区域 id 只能包含字母、数字、下划线和中划线，且不能超过 64 位");
            }
            if (!ids.add(id)) {
                throw new BusinessException(BizErrorCode.PARAM_ERROR, "首页区域 id 不能重复：" + id);
            }
            if (!HOME_SECTION_TYPES.contains(type)) {
                throw new BusinessException(BizErrorCode.PARAM_ERROR, "不支持的首页区域类型：" + type);
            }
            if (!"POSTER".equals(type) && !singletonTypes.add(type)) {
                throw new BusinessException(
                        BizErrorCode.PARAM_ERROR,
                        "同一种动态首页区域只能配置一次：" + type);
            }
            if ("POSTER".equals(type)) {
                requireText(section.getString("title"), "自定义海报区标题不能为空");
                validatePosters(section.getJSONArray("posters"));
            }
        }
    }

    private static void validateScalar(SystemConfigKey key, String value) {
        String defaultValue = key.getDefaultValue();
        if (defaultValue.matches("-?\\d+")) {
            try {
                Long.parseLong(value);
            } catch (NumberFormatException exception) {
                throw new BusinessException(
                        BizErrorCode.PARAM_ERROR, key.getDesc() + "必须是整数");
            }
        } else if (defaultValue.matches("-?\\d+\\.\\d+")) {
            try {
                new BigDecimal(value);
            } catch (NumberFormatException exception) {
                throw new BusinessException(
                        BizErrorCode.PARAM_ERROR, key.getDesc() + "必须是数字");
            }
        }
    }

    private static String valueType(SystemConfigKey key) {
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

    private static void validatePosters(JSONArray posters) {
        if (posters == null) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, "海报列表不能为空");
        }
        if (posters.size() > MAX_POSTERS) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, "每个区域最多配置 20 张海报");
        }
        for (int index = 0; index < posters.size(); index++) {
            JSONObject poster = posters.getJSONObject(index);
            if (poster == null) {
                throw new BusinessException(
                        BizErrorCode.PARAM_ERROR, "第 " + (index + 1) + " 张海报格式错误");
            }
            String type = poster.getString("type");
            if (!"NAVIGATE".equals(type) && !"PREVIEW".equals(type)) {
                throw new BusinessException(
                        BizErrorCode.PARAM_ERROR,
                        "第 " + (index + 1) + " 张海报类型只能是 NAVIGATE 或 PREVIEW");
            }
            requireText(
                    poster.getString("image"),
                    "第 " + (index + 1) + " 张海报图片 key 不能为空");
        }
    }

    private static void requireText(String value, String message) {
        if (StringUtils.isBlank(value)) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, message);
        }
    }

    /** 当前应用版本携带的配置名录定义。 */
    private record CatalogDefinition(SystemConfigKey key) implements PlatformConfigDefinition {

        @Override
        public String configKey() {
            return key.getKey();
        }

        @Override
        public String valueType() {
            return PublishGlobalConfigActivity.valueType(key);
        }

        @Override
        public String normalize(String rawValue) {
            return validateAndNormalize(key, rawValue);
        }

        @Override
        public boolean accepts(String normalizedValue) {
            try {
                return Objects.equals(normalizedValue, normalize(normalizedValue));
            } catch (RuntimeException exception) {
                return false;
            }
        }
    }
}
