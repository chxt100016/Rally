package com.rally.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 日出日落时间计算工具（基于 NOAA 天文简化算法）
 * 返回时间均为上海时区（UTC+8）
 */
public class SunUtils {

    // 天顶角修正值：标准 90° + 大气折射 0.567° + 太阳视半径 0.267° ≈ 90.833°
    private static final double ZENITH = 90.833;

    /**
     * 计算日出时间（上海时区）
     * @param dateTime  日期时间（取其日期部分参与计算）
     * @param latitude  纬度（北纬为正，南纬为负）
     * @param longitude 经度（东经为正，西经为负）
     * @return 日出 LocalDateTime（上海时区）；极夜（太阳永不升起）返回 null
     */
    public static LocalDateTime sunrise(LocalDateTime dateTime, double latitude, double longitude) {
        return calculate(dateTime.toLocalDate(), latitude, longitude, true);
    }

    /**
     * 计算日落时间（上海时区）
     * @param dateTime  日期时间（取其日期部分参与计算）
     * @param latitude  纬度（北纬为正，南纬为负）
     * @param longitude 经度（东经为正，西经为负）
     * @return 日落 LocalDateTime（上海时区）；极昼（太阳永不落下）返回 null
     */
    public static LocalDateTime sunset(LocalDateTime dateTime, double latitude, double longitude) {
        return calculate(dateTime.toLocalDate(), latitude, longitude, false);
    }

    private static LocalDateTime calculate(LocalDate date, double latitude, double longitude, boolean isSunrise) {
        int dayOfYear = date.getDayOfYear();

        // 经度换算为时区小时偏移（每 15° 对应 1 小时）
        double lngHour = longitude / 15.0;

        // 近似计算时刻：日出取 6 时，日落取 18 时
        double t = dayOfYear + ((isSunrise ? 6.0 : 18.0) - lngHour) / 24.0;

        // 太阳平均近点角（Mean Anomaly）
        double M = (0.9856 * t) - 3.289;

        // 太阳真黄经（True Longitude）
        double L = M + (1.916 * Math.sin(Math.toRadians(M))) + (0.020 * Math.sin(Math.toRadians(2 * M))) + 282.634;
        L = normalizeAngle(L);

        // 太阳赤经（Right Ascension），先以度计算再转为小时
        double RA = Math.toDegrees(Math.atan(0.91764 * Math.tan(Math.toRadians(L))));
        RA = normalizeAngle(RA);

        // 确保 RA 与 L 落在同一象限，再转为小时单位
        double Lquadrant = Math.floor(L / 90) * 90;
        double RAquadrant = Math.floor(RA / 90) * 90;
        RA = (RA + (Lquadrant - RAquadrant)) / 15.0;

        // 太阳赤纬正弦与余弦（Declination）
        double sinDec = 0.39782 * Math.sin(Math.toRadians(L));
        double cosDec = Math.cos(Math.asin(sinDec));

        // 时角余弦（Hour Angle cosine）；超出 [-1, 1] 说明极昼或极夜
        double cosH = (Math.cos(Math.toRadians(ZENITH)) - sinDec * Math.sin(Math.toRadians(latitude))) / (cosDec * Math.cos(Math.toRadians(latitude)));
        if (cosH > 1 || cosH < -1) {
            return null;
        }

        // 时角（小时）：日出取西半（360 - arccos），日落取东半（arccos）
        double H = isSunrise ? (360.0 - Math.toDegrees(Math.acos(cosH))) / 15.0 : Math.toDegrees(Math.acos(cosH)) / 15.0;

        // 地方平均时（Local Mean Time）
        double T = H + RA - (0.06571 * t) - 6.622;

        // 转为 UTC 小时
        double utHour = normalizeHour(T - lngHour);

        int hours = (int) utHour;
        int minutes = (int) ((utHour - hours) * 60);
        int seconds = (int) (((utHour - hours) * 60 - minutes) * 60);

        LocalDateTime utcDateTime = LocalDateTime.of(date, LocalTime.of(hours, minutes, seconds));

        // 转换为上海时区（UTC+8），跨日时自动进位到次日
        return utcDateTime.plusHours(8);
    }

    private static double normalizeAngle(double angle) {
        angle = angle % 360;
        return angle < 0 ? angle + 360 : angle;
    }

    private static double normalizeHour(double hour) {
        hour = hour % 24;
        return hour < 0 ? hour + 24 : hour;
    }

    public static void main(String[] args) {
        System.out.println(LocalDateTime.now());
        System.out.println(sunrise(LocalDateTime.now(), 30.29, 120.02));
        System.out.println(sunset(LocalDateTime.now(), 30.29, 120.02));
    }
}
