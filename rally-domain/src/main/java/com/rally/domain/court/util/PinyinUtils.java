package com.rally.domain.court.util;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import org.apache.commons.lang3.StringUtils;

/**
 * 球场名称拼音生成工具。
 * 汉字转全拼与首字母，非汉字字符原样保留。
 */
public class PinyinUtils {

    private static final HanyuPinyinOutputFormat FORMAT = new HanyuPinyinOutputFormat();

    static {
        FORMAT.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        FORMAT.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
    }

    private PinyinUtils() {
    }

    /** 全拼，如「西湖网球场」→「xihuwangqiuchang」 */
    public static String toPinyin(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (char ch : text.toCharArray()) {
            String[] items = pinyinOf(ch);
            if (items == null) {
                sb.append(ch);
            } else {
                sb.append(items[0]);
            }
        }
        return sb.toString();
    }

    /** 拼音首字母大写，如「西湖网球场」→「XHWQC」 */
    public static String toPinyinInitial(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (char ch : text.toCharArray()) {
            String[] items = pinyinOf(ch);
            if (items == null) {
                sb.append(ch);
            } else {
                sb.append(items[0].charAt(0));
            }
        }
        return sb.toString().toUpperCase();
    }

    private static String[] pinyinOf(char ch) {
        try {
            String[] items = PinyinHelper.toHanyuPinyinStringArray(ch, FORMAT);
            return items == null || items.length == 0 ? null : items;
        } catch (Exception e) {
            return null;
        }
    }
}
