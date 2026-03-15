package top.bulgat.migration.admin.domain.model;

import lombok.Getter;

/**
 * 统计粒度枚举。
 */
@Getter
public enum StatisticsGranularity {
    MINUTE(60, "yyyy-MM-dd HH:mm:00"),
    HOUR(3600, "yyyy-MM-dd HH:00:00"),
    DAY(86400, "yyyy-MM-dd 00:00:00");

    private final int seconds;
    private final String format;

    StatisticsGranularity(int seconds, String format) {
        this.seconds = seconds;
        this.format = format;
    }

    public static StatisticsGranularity of(String value) {
        if (value == null) {
            return HOUR;
        }
        try {
            return StatisticsGranularity.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return HOUR;
        }
    }
}
