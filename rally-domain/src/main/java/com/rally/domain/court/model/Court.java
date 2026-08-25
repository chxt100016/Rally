package com.rally.domain.court.model;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.court.enums.CourtEnvironmentEnum;
import com.rally.domain.court.enums.CourtSourceEnum;
import com.rally.domain.court.enums.CourtStatusEnum;
import com.rally.domain.court.enums.CourtSurfaceEnum;
import com.rally.domain.court.util.PinyinUtils;
import com.rally.domain.utils.Assert;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 球场聚合根。守护一处球场的身份、归属与可用状态在一次事务内始终自洽。
 * 状态承载在 CourtData 上，所有状态变更只经由本类的命令方法。
 */
public class Court {

    /** 别名字段长度上限，超出按上限截断 */
    private static final int ALIAS_MAX_LENGTH = 128;
    /** 标签字段长度上限，超出按上限截断 */
    private static final int TAGS_MAX_LENGTH = 512;

    private static final String EXT_PINYIN = "pinyin";
    private static final String EXT_PINYIN_INITIAL = "pinyinInitial";
    private static final String EXT_RATING = "rating";
    private static final String EXT_COST = "cost";
    private static final String EXT_OPENTIME = "opentime";
    private static final String EXT_TEL = "tel";

    private final CourtData state;

    private Court(CourtData state) {
        this.state = state;
    }

    /**
     * 从已落库的球场重建聚合。
     * 历史数据可能缺拼音、或区域编码与名称不成对，载入时先归一，
     * 让 I1 与 I4 始终是聚合自己维持的不变量，而不是拿来卡住运营操作的门槛。
     */
    public static Court of(CourtData data) {
        Assert.notNull(data, BizErrorCode.COURT_NOT_FOUND);
        Court court = new Court(data);
        court.normalize();
        return court;
    }

    /** 载入归一：补齐拼音，抹平区域编码与名称的单边缺失 */
    private void normalize() {
        if (StringUtils.isBlank(state.getDistrictCode()) || StringUtils.isBlank(state.getDistrictName())) {
            state.setDistrictCode(null);
            state.setDistrictName(null);
        }
        refreshPinyin();
    }

    /** 聚合当前状态，交给仓储落库 */
    public CourtData state() {
        return state;
    }

    public String bizId() {
        return state.getBizId();
    }

    public CourtStatusEnum status() {
        return state.getStatus();
    }

    /**
     * C1 录入球场：运营手工录入，来源为系统录入，不带三方来源编号。
     */
    public static Court create(CourtCreateCmd cmd) {
        Assert.notNull(cmd, BizErrorCode.PARAM_ERROR);
        Assert.notBlank(cmd.getName(), BizErrorCode.PARAM_ERROR);
        CourtLocation location = cmd.getLocation();
        Assert.notNull(location, BizErrorCode.PARAM_ERROR);
        Assert.notBlank(location.getCityCode(), BizErrorCode.PARAM_ERROR);

        CourtData data = new CourtData();
        data.setBizId(IdWorker.getIdStr());
        data.setName(cmd.getName());
        data.setAlias(joinAlias(cmd.getAlias()));
        data.setAddress(cmd.getAddress());
        data.setLng(cmd.getLng());
        data.setLat(cmd.getLat());
        data.setRemark(cmd.getRemark());
        data.setType(cmd.getType());
        data.setSurface(cmd.getSurface());
        data.setTags(joinTags(cmd.getTags()));
        data.setSource(CourtSourceEnum.SYSTEM);
        data.setStatus(cmd.getStatus() == null ? CourtStatusEnum.ACTIVE : cmd.getStatus());
        data.setMeetupCount(0);

        Court court = new Court(data);
        court.applyLocation(location);
        court.applyProfile(cmd.getProfile());
        court.refreshPinyin();
        court.checkInvariants();
        return court;
    }

    /**
     * C2 收录球场：抓取新收录一处球场，来源为系统录入，必带三方来源编号，状态为可用。
     */
    public static Court collect(CourtCollectCmd cmd) {
        Assert.notNull(cmd, BizErrorCode.PARAM_ERROR);
        Assert.notBlank(cmd.getSourceId(), BizErrorCode.PARAM_ERROR);
        Assert.notBlank(cmd.getName(), BizErrorCode.PARAM_ERROR);
        CourtLocation location = cmd.getLocation();
        Assert.notNull(location, BizErrorCode.PARAM_ERROR);
        Assert.notBlank(location.getCityCode(), BizErrorCode.PARAM_ERROR);

        CourtData data = new CourtData();
        data.setBizId(IdWorker.getIdStr());
        data.setSourceId(cmd.getSourceId());
        data.setName(cmd.getName());
        data.setAlias(joinAlias(cmd.getAlias()));
        data.setAddress(cmd.getAddress());
        data.setLng(cmd.getLng());
        data.setLat(cmd.getLat());
        data.setType(cmd.getType());
        data.setTags(joinTags(cmd.getTags()));
        data.setSource(CourtSourceEnum.SYSTEM);
        data.setStatus(CourtStatusEnum.ACTIVE);
        data.setMeetupCount(0);

        Court court = new Court(data);
        court.applyLocation(location);
        court.applyProfile(cmd.getProfile());
        court.refreshPinyin();
        court.checkInvariants();
        return court;
    }

    /**
     * C3 改写球场资料：给了才改，没给保持原值。
     * alias 与 tags 传空列表表示清空，传 null 表示不改。
     */
    public void updateProfile(CourtUpdateCmd cmd) {
        Assert.notNull(cmd, BizErrorCode.PARAM_ERROR);
        if (cmd.getName() != null) {
            Assert.notBlank(cmd.getName(), BizErrorCode.PARAM_ERROR);
            state.setName(cmd.getName());
            refreshPinyin();
        }
        if (cmd.getAlias() != null) {
            state.setAlias(joinAlias(cmd.getAlias()));
        }
        if (cmd.getAddress() != null) {
            state.setAddress(cmd.getAddress());
        }
        if (cmd.getLng() != null) {
            state.setLng(cmd.getLng());
        }
        if (cmd.getLat() != null) {
            state.setLat(cmd.getLat());
        }
        if (cmd.getRemark() != null) {
            state.setRemark(cmd.getRemark());
        }
        if (cmd.getType() != null) {
            state.setType(cmd.getType());
        }
        if (cmd.getSurface() != null) {
            state.setSurface(cmd.getSurface());
        }
        if (cmd.getTags() != null) {
            state.setTags(joinTags(cmd.getTags()));
        }
        if (cmd.getLocation() != null) {
            mergeLocation(cmd.getLocation());
        }
        if (cmd.getProfile() != null) {
            applyProfile(cmd.getProfile());
        }
        if (cmd.getStatus() != null) {
            state.setStatus(cmd.getStatus());
        }
        checkInvariants();
    }

    /**
     * C4 停用球场：置为已停用。已是停用状态时不产生变更。
     *
     * @return true 表示本次发生了状态变更
     */
    public boolean disable() {
        if (CourtStatusEnum.DISABLED.equals(state.getStatus())) {
            return false;
        }
        state.setStatus(CourtStatusEnum.DISABLED);
        checkInvariants();
        return true;
    }

    /**
     * C5 按抓取结果覆盖球场：只覆盖高德能提供的字段，备注、场地材质、约球次数保持原值。
     */
    public void overwriteByCollect(CourtCollectCmd cmd) {
        Assert.notNull(cmd, BizErrorCode.PARAM_ERROR);
        Assert.isTrue(CourtSourceEnum.SYSTEM.equals(state.getSource()), BizErrorCode.COURT_SOURCE_CONFLICT);
        Assert.notBlank(state.getSourceId(), BizErrorCode.COURT_SOURCE_CONFLICT);
        Assert.notBlank(cmd.getName(), BizErrorCode.PARAM_ERROR);
        CourtLocation location = cmd.getLocation();
        Assert.notNull(location, BizErrorCode.PARAM_ERROR);
        Assert.notBlank(location.getCityCode(), BizErrorCode.PARAM_ERROR);

        state.setName(cmd.getName());
        state.setAlias(joinAlias(cmd.getAlias()));
        state.setAddress(cmd.getAddress());
        state.setLng(cmd.getLng());
        state.setLat(cmd.getLat());
        state.setType(cmd.getType());
        state.setTags(joinTags(cmd.getTags()));
        state.setStatus(CourtStatusEnum.ACTIVE);
        applyLocation(location);
        applyProfile(cmd.getProfile());
        refreshPinyin();
        checkInvariants();
    }

    /**
     * I1 I2 I3 I4：每个命令执行后校验全部不变量。
     */
    private void checkInvariants() {
        // I3 球场业务编号必须非空且不可变更
        Assert.notBlank(state.getBizId(), BizErrorCode.COURT_BIZ_ID_IMMUTABLE);
        // I4 城市编码非空；区域编码与区域名称必须同时为空或同时非空
        Assert.notBlank(state.getCityCode(), BizErrorCode.COURT_LOCATION_INCOMPLETE);
        boolean districtCodeBlank = StringUtils.isBlank(state.getDistrictCode());
        boolean districtNameBlank = StringUtils.isBlank(state.getDistrictName());
        Assert.isTrue(districtCodeBlank == districtNameBlank, BizErrorCode.COURT_LOCATION_INCOMPLETE);
        // I2 有三方来源编号的必须是系统录入；用户发布的不得持有三方来源编号
        if (StringUtils.isNotBlank(state.getSourceId())) {
            Assert.isTrue(CourtSourceEnum.SYSTEM.equals(state.getSource()), BizErrorCode.COURT_SOURCE_CONFLICT);
        }
        if (CourtSourceEnum.USER_PUBLISH.equals(state.getSource())) {
            Assert.isTrue(StringUtils.isBlank(state.getSourceId()), BizErrorCode.COURT_SOURCE_CONFLICT);
        }
        // I1 展示资料里的拼音必须与当前球场名称一致
        JSONObject ext = readExt();
        Assert.isTrue(Objects.equals(ext.getString(EXT_PINYIN), PinyinUtils.toPinyin(state.getName())), BizErrorCode.COURT_PINYIN_STALE);
        Assert.isTrue(Objects.equals(ext.getString(EXT_PINYIN_INITIAL), PinyinUtils.toPinyinInitial(state.getName())), BizErrorCode.COURT_PINYIN_STALE);
    }

    /** 名称变更后重算拼音，保证 I1 */
    private void refreshPinyin() {
        JSONObject ext = readExt();
        putIfPresent(ext, EXT_PINYIN, PinyinUtils.toPinyin(state.getName()));
        putIfPresent(ext, EXT_PINYIN_INITIAL, PinyinUtils.toPinyinInitial(state.getName()));
        writeExt(ext);
    }

    /** C3 地理归属按城市、区域两部分分别改写，给了哪部分改哪部分，保证 I4 的编码与名称成对 */
    private void mergeLocation(CourtLocation location) {
        if (location.getCityCode() != null) {
            state.setCityCode(location.getCityCode());
            state.setCityName(location.getCityName());
        }
        if (location.getDistrictCode() != null) {
            state.setDistrictCode(location.getDistrictCode());
            state.setDistrictName(location.getDistrictName());
        }
    }

    /** 地理归属整体替换，保证 I4 的编码与名称成对 */
    private void applyLocation(CourtLocation location) {
        state.setCityCode(location.getCityCode());
        state.setCityName(location.getCityName());
        state.setDistrictCode(location.getDistrictCode());
        state.setDistrictName(location.getDistrictName());
    }

    /** 展示资料逐项改写，没给的项保持原值 */
    private void applyProfile(CourtProfile profile) {
        if (profile == null) {
            return;
        }
        JSONObject ext = readExt();
        putIfPresent(ext, EXT_RATING, profile.getRating());
        putIfPresent(ext, EXT_COST, profile.getCost());
        putIfPresent(ext, EXT_OPENTIME, profile.getOpentime());
        putIfPresent(ext, EXT_TEL, profile.getTel());
        writeExt(ext);
    }

    private JSONObject readExt() {
        if (StringUtils.isBlank(state.getExtData())) {
            return new JSONObject();
        }
        try {
            JSONObject json = JSON.parseObject(state.getExtData());
            return json == null ? new JSONObject() : json;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private void writeExt(JSONObject ext) {
        state.setExtData(ext.isEmpty() ? null : ext.toJSONString());
    }

    private static void putIfPresent(JSONObject ext, String key, String value) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        ext.put(key, value);
    }

    private static String joinAlias(List<String> alias) {
        return truncate(join(alias), ALIAS_MAX_LENGTH);
    }

    private static String joinTags(List<String> tags) {
        return truncate(join(tags), TAGS_MAX_LENGTH);
    }

    private static String join(List<String> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        String joined = items.stream().filter(StringUtils::isNotBlank).collect(Collectors.joining(","));
        return StringUtils.isBlank(joined) ? null : joined;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
