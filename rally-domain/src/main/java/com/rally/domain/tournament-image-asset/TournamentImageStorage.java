package com.rally.domain.content.imageasset;

/**
 * 赛事图片对象存储网关；相同对象键必须采用覆盖语义。
 */
public interface TournamentImageStorage {

    /** 将内容覆盖保存到固定对象键。 */
    void overwrite(String key, byte[] content) throws Exception;
}
