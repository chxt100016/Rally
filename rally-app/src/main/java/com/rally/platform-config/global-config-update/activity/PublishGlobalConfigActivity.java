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
    private static final Set<String> HOME_SECTION_TYPES = Set.of(
            "MEETUP", "TOUR_MATCH", "POSTER", "NEWS");
    private static final Set<String> POSTER_ACTION_TYPES = Set.of("NAVIGATE", "PREVIEW");
    private static final Set<String> OPTIONAL_POSTER_TEXT_FIELDS = Set.of("title", "subtitle");
    private static final Set<String> POSTER_URL_FIELDS = Set.of("wechatUrl", "appUrl", "webUrl");
    private static final Set<String> POSTER_URL_PLACEHOLDERS = Set.of(
            "{{cityId}}", "{{cityName}}");

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
            if (key == SystemConfigKey.HOME_PAGE_CONFIG) {
                return validateAndNormalizeHomePage(value);
            }
            validateScalar(key, value);
            return value;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, "配置格式无效");
        }
    }

    private static String validateAndNormalizeHomePage(String value) {
        Object parsed = JSON.parse(value);
        if (!(parsed instanceof JSONObject root)) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, "首页配置必须是 JSON 对象");
        }
        Object sectionsValue = root.get("sections");
        if (!(sectionsValue instanceof JSONArray sections)) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, "首页 sections 必须是数组");
        }
        validateHomeSections(sections);
        return JSON.toJSONString(root);
    }

    private static void validateHomeSections(JSONArray sections) {
        if (sections.size() > MAX_HOME_SECTIONS) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, "首页最多配置 30 个区域");
        }
        Set<String> ids = new HashSet<>();
        Set<String> singletonTypes = new HashSet<>();
        for (int index = 0; index < sections.size(); index++) {
            Object sectionValue = sections.get(index);
            if (!(sectionValue instanceof JSONObject section)) {
                throw new BusinessException(
                        BizErrorCode.PARAM_ERROR,
                        "第 " + (index + 1) + " 个首页区域格式错误");
            }
            String id = requireTextField(
                    section, "id", "第 " + (index + 1) + " 个首页区域 id 不能为空");
            String type = requireTextField(
                    section, "type", "第 " + (index + 1) + " 个首页区域 type 不能为空");
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
            validateOptionalBoolean(section, "enabled", "首页区域 enabled 必须是布尔值");
            if ("POSTER".equals(type)) {
                requireTextField(section, "title", "海报区标题不能为空");
                validateOptionalString(section, "subtitle", false, "海报区 subtitle 必须是字符串");
                Object postersValue = section.get("posters");
                if (!(postersValue instanceof JSONArray posters)) {
                    throw new BusinessException(BizErrorCode.PARAM_ERROR, "海报列表必须是数组");
                }
                validatePosters(posters);
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
        if (key == SystemConfigKey.HOME_PAGE_CONFIG) {
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
        if (posters.size() > MAX_POSTERS) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, "每个区域最多配置 20 张海报");
        }
        for (int index = 0; index < posters.size(); index++) {
            Object posterValue = posters.get(index);
            if (!(posterValue instanceof JSONObject poster)) {
                throw new BusinessException(
                        BizErrorCode.PARAM_ERROR, "第 " + (index + 1) + " 张海报格式错误");
            }
            String actionType = requireTextField(
                    poster,
                    "actionType",
                    "第 " + (index + 1) + " 张海报 actionType 不能为空");
            if (!POSTER_ACTION_TYPES.contains(actionType)) {
                throw new BusinessException(
                        BizErrorCode.PARAM_ERROR,
                        "第 " + (index + 1) + " 张海报 actionType 只能是 NAVIGATE 或 PREVIEW");
            }
            requireTextField(
                    poster,
                    "image",
                    "第 " + (index + 1) + " 张海报图片 key 不能为空");
            for (String field : OPTIONAL_POSTER_TEXT_FIELDS) {
                validateOptionalString(
                        poster,
                        field,
                        false,
                        "第 " + (index + 1) + " 张海报 " + field + " 必须是字符串");
            }
            for (String field : POSTER_URL_FIELDS) {
                validatePosterUrl(poster, field, index);
            }
            validateOptionalString(
                    poster,
                    "cityId",
                    true,
                    "第 " + (index + 1) + " 张海报 cityId 必须是字符串");
        }
    }

    private static void validatePosterUrl(JSONObject poster, String field, int posterIndex) {
        if (!poster.containsKey(field)) {
            return;
        }
        Object value = poster.get(field);
        if (!(value instanceof String url)) {
            throw new BusinessException(
                    BizErrorCode.PARAM_ERROR,
                    "第 " + (posterIndex + 1) + " 张海报 " + field + " 必须是字符串");
        }
        validatePosterUrlPlaceholders(url, posterIndex, field);
    }

    private static void validatePosterUrlPlaceholders(
            String url, int posterIndex, String field) {
        int cursor = 0;
        while (cursor < url.length()) {
            int opening = url.indexOf("{{", cursor);
            int closing = url.indexOf("}}", cursor);
            if (closing >= 0 && (opening < 0 || closing < opening)) {
                throw invalidPosterUrlPlaceholder(posterIndex, field);
            }
            if (opening < 0) {
                return;
            }
            closing = url.indexOf("}}", opening + 2);
            if (closing < 0) {
                throw invalidPosterUrlPlaceholder(posterIndex, field);
            }
            String placeholder = url.substring(opening, closing + 2);
            if (!POSTER_URL_PLACEHOLDERS.contains(placeholder)) {
                throw invalidPosterUrlPlaceholder(posterIndex, field);
            }
            cursor = closing + 2;
        }
    }

    private static BusinessException invalidPosterUrlPlaceholder(int posterIndex, String field) {
        return new BusinessException(
                BizErrorCode.PARAM_ERROR,
                "第 " + (posterIndex + 1) + " 张海报 " + field + " 包含无效占位符");
    }

    private static String requireTextField(JSONObject object, String field, String message) {
        Object value = object.get(field);
        if (!(value instanceof String text) || StringUtils.isBlank(text)) {
            throw new BusinessException(BizErrorCode.PARAM_ERROR, message);
        }
        return text;
    }

    private static void validateOptionalString(
            JSONObject object,
            String field,
            boolean allowNull,
            String message) {
        if (!object.containsKey(field)) {
            return;
        }
        Object value = object.get(field);
        if ((value == null && allowNull) || value instanceof String) {
            return;
        }
        throw new BusinessException(BizErrorCode.PARAM_ERROR, message);
    }

    private static void validateOptionalBoolean(JSONObject object, String field, String message) {
        if (object.containsKey(field) && !(object.get(field) instanceof Boolean)) {
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
