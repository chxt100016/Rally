package com.rally.domain.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddressUtils {

    private final static Pattern DISTRICT_PATTERN = Pattern.compile("市([\\u4e00-\\u9fa5]{1,8}?区)");

    public static String getDistrict(String address) {


        Matcher matcher = DISTRICT_PATTERN.matcher(address);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
