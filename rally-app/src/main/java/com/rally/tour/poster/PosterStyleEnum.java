package com.rally.tour.poster;

/**
 * 生图风格模块（可拔插）。
 * 与赛事级别无关，纯粹控制画面渲染风格，调用方按需选择。
 */
public enum PosterStyleEnum {

    /** 写实摄影风格 */
    REALISTIC("超写实摄影风格，真实光影，电影级质感，4K高清体育摄影。"),

    /** App Store 3D 卡通风格 */
    CARTOON_3D("App Store风格的3D卡通渲染，圆润可爱的造型，鲜艳明快的配色，柔和的卡通光影，精致的3D图标质感，清新简洁。");

    private final String desc;

    PosterStyleEnum(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
