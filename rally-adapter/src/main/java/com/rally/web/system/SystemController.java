package com.rally.web.system;

import com.rally.config.OptionalAuth;
import com.rally.config.property.QiniuConfiguration;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.system.enums.SystemConfigKey;
import com.rally.domain.system.model.HomeConfigDTO;
import com.rally.domain.system.model.HomeConfigUpdateCmd;
import com.rally.domain.tour.model.Result;
import com.rally.system.HomeConfigAdminAppService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置查询接口。
 * 前端传入 key，返回对应的配置值。
 */
@RestController
@RequestMapping({"/system", "/wechat/system"})
@RequiredArgsConstructor
public class SystemController {

    private final HomeConfigAdminAppService homeConfigAdminAppService;

    /**
     * 根据 key 查询配置值
     *
     * @param key 配置项 key
     * @return 配置值字符串
     */
    @GetMapping("/config")
    public Result<String> getConfig(@RequestParam("key") String key) {
        String value = SystemConfig.getString(key);
        return Result.ok(value);
    }

    /**
     * 获取群聊二维码（base64）
     *
     * @return { qrcode: "data:image/png;base64,..." }
     */
    @GetMapping("/qrcode")
    public Result<String> getQrcode() {
        String url = QiniuConfiguration.buildSignedUrl("default/qrcode.jpg");
        return Result.ok(url);
    }

    /**
     * 获取启动页封面图 URL
     *
     * @return 启动页封面图的签名 URL
     */
    @GetMapping("/splash-cover")
    @OptionalAuth
    public Result<String> getSplashCover() {
        String key = SystemConfig.getString(SystemConfigKey.SYSTEM_SPLASH_COVER_KEY.getKey());
        String url = QiniuConfiguration.buildSignedUrl(key);
        return Result.ok(url);
    }

    /**
     * 批量查询配置值
     *
     * @param keys 配置项 key 列表
     * @return key -> value 映射，不存在的 key 不返回
     */
    @PostMapping("/config/batch")
    public Result<Map<String, String>> batchGetConfig(@RequestBody List<String> keys) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String key : keys) {
            String value = SystemConfig.getString(key);
            if (value != null) {
                result.put(key, value);
            }
        }
        return Result.ok(result);
    }

    /** 获取运营后台可编辑的首页配置。 */
    @GetMapping("/admin/home/config")
    public Result<HomeConfigDTO> getHomeConfig() {
        return Result.ok(homeConfigAdminAppService.get());
    }

    /** 更新首页配置并立即刷新当前实例的配置缓存。 */
    @PostMapping("/admin/home/config/update")
    public Result<HomeConfigDTO> updateHomeConfig(@Valid @RequestBody HomeConfigUpdateCmd cmd) {
        return Result.ok(homeConfigAdminAppService.update(cmd));
    }

    /** 获取运营后台可编辑的全部系统配置。 */
    @GetMapping("/admin/config")
    public Result<HomeConfigDTO> getAllConfig() {
        return Result.ok(homeConfigAdminAppService.getAll());
    }

    /** 更新任意已登记的系统配置并立即刷新当前实例缓存。 */
    @PostMapping("/admin/config/update")
    public Result<HomeConfigDTO> updateConfig(@Valid @RequestBody HomeConfigUpdateCmd cmd) {
        return Result.ok(homeConfigAdminAppService.updateAny(cmd));
    }


}
