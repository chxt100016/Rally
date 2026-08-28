package com.rally.web.system;

import com.rally.config.OptionalAuth;
import com.rally.domain.system.model.HomeConfigDTO;
import com.rally.domain.system.model.HomeConfigUpdateCmd;
import com.rally.domain.tour.model.Result;
import com.rally.platformconfig.allconfigquery.activity.QueryAllConfigViewActivity;
import com.rally.platformconfig.globalconfigupdate.activity.PublishGlobalConfigActivity;
import com.rally.platformconfig.groupchatentryquery.activity.IssueGroupChatEntryUrlActivity;
import com.rally.platformconfig.publicconfigquery.activity.QueryPublicConfigMapActivity;
import com.rally.platformconfig.publicconfigquery.activity.QueryPublicConfigValueActivity;
import com.rally.platformconfig.splashcoverquery.activity.IssueSplashCoverUrlActivity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 系统配置查询接口。
 * 前端传入 key，返回对应的配置值。
 */
@RestController
@RequestMapping("/system")
@RequiredArgsConstructor
public class SystemController {

    private final PublishGlobalConfigActivity publishGlobalConfigActivity;
    private final QueryAllConfigViewActivity queryAllConfigViewActivity;
    private final QueryPublicConfigValueActivity queryPublicConfigValueActivity;
    private final QueryPublicConfigMapActivity queryPublicConfigMapActivity;
    private final IssueGroupChatEntryUrlActivity issueGroupChatEntryUrlActivity;
    private final IssueSplashCoverUrlActivity issueSplashCoverUrlActivity;

    /**
     * 根据 key 查询配置值
     *
     * @param key 配置项 key
     * @return 配置值字符串
     */
    @GetMapping("/config")
    public Result<String> getConfig(@RequestParam("key") String key) {
        return Result.ok(queryPublicConfigValueActivity.execute(key));
    }

    /**
     * 获取群聊二维码（base64）
     *
     * @return { qrcode: "data:image/png;base64,..." }
     */
    @GetMapping("/qrcode")
    public Result<String> getQrcode() {
        return Result.ok(issueGroupChatEntryUrlActivity.execute());
    }

    /**
     * 获取启动页封面图 URL
     *
     * @return 启动页封面图的签名 URL
     */
    @GetMapping("/splash-cover")
    @OptionalAuth
    public Result<String> getSplashCover() {
        return Result.ok(issueSplashCoverUrlActivity.execute());
    }

    /**
     * 批量查询配置值
     *
     * @param keys 配置项 key 列表
     * @return key -> value 映射，不存在的 key 不返回
     */
    @PostMapping("/config/batch")
    public Result<Map<String, String>> batchGetConfig(@RequestBody List<String> keys) {
        return Result.ok(queryPublicConfigMapActivity.execute(keys));
    }

    /** 获取运营后台可编辑的全部系统配置。 */
    @GetMapping("/admin/config")
    public Result<HomeConfigDTO> getAllConfig() {
        return Result.ok(queryAllConfigViewActivity.execute());
    }

    /** 更新任意已登记的系统配置并立即刷新当前实例缓存。 */
    @PostMapping("/admin/config/update")
    public Result<HomeConfigDTO> updateConfig(@Valid @RequestBody HomeConfigUpdateCmd cmd) {
        publishGlobalConfigActivity.execute(cmd);
        return Result.ok(queryAllConfigViewActivity.execute());
    }


}
