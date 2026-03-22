package top.bulgat.migration.admin.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import top.bulgat.common.base.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Configuration
public class DateConverterConfig {

    private static final DateTimeFormatter FORMAT_YMD = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FORMAT_YMD_HMS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 全局 String -> LocalDate 转换器：兼容两种格式
     */
    @Bean
    public Converter<String, LocalDate> stringToLocalDateConverter() {
        return new Converter<String, LocalDate>() {
            @Override
            public LocalDate convert(String source) {
                if (StringUtils.isBlank(source)) {
                    return null;
                }
                try {
                    return LocalDate.parse(source.trim(), FORMAT_YMD);
                } catch (DateTimeParseException e) {
                    LocalDateTime dateTime = LocalDateTime.parse(source.trim(), FORMAT_YMD_HMS);
                    return dateTime.toLocalDate();
                }
            }
        };
    }
}