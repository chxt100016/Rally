package com.rally.domain.content.imageasset;

/**
 * 赛事图片编解码网关，由基础设施适配具体图片库。
 */
public interface TournamentImageCodec {

    /** 判断原始二进制是否能被图片解码器识别。 */
    boolean canDecode(byte[] sourceImage) throws Exception;

    /** 直接从原图按指定质量生成 JPEG。 */
    byte[] encodeJpeg(byte[] sourceImage, float quality) throws Exception;

    /** 直接从原图生成以指定 KB 数为压缩目标的 JPEG。 */
    byte[] compressJpeg(byte[] sourceImage, int targetKb) throws Exception;
}
