package com.rally.domain.media.assetstorage;

/**
 * 上传授权的对象键约束模式。
 */
public enum UploadScopeMode {
    /** 初始 policy scope 是一个完整对象键。 */
    EXACT_KEY,
    /** 初始 policy scope 是前缀并写入 isPrefixalScope；最终 scope 仍可能被 SDK 重建。 */
    KEY_PREFIX
}
