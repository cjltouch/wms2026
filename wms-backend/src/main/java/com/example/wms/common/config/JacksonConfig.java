package com.example.wms.common.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 全局 Jackson 日期时间格式配置。
 * 统一将 LocalDateTime 序列化为 yyyy-MM-dd HH:mm:ss（避免前端出现 2026-09-04T00:14:34 的 T 分隔符），
 * LocalDate 序列化为 yyyy-MM-dd；
 * 反序列化兼容三种入参：yyyy-MM-dd HH:mm:ss、ISO 格式(带T)、纯日期(yyyy-MM-dd)。
 */
@Configuration
public class JacksonConfig {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE_PATTERN = "yyyy-MM-dd";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);

    /**
     * 定制 Spring Boot 自动配置的 ObjectMapper，注册 Java8 时间类型的统一序列化/反序列化器。
     *
     * @return Jackson 构建定制器
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule();
            module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DATE_TIME_FORMATTER));
            module.addSerializer(LocalDate.class, new LocalDateSerializer(DATE_FORMATTER));
            module.addDeserializer(LocalDateTime.class, new LenientLocalDateTimeDeserializer());
            module.addDeserializer(LocalDate.class, new LenientLocalDateDeserializer());
            builder.modulesToInstall(module);
        };
    }

    /**
     * LocalDateTime 宽松反序列化器：
     * 兼容 yyyy-MM-dd HH:mm:ss、yyyy-MM-ddTHH:mm:ss（ISO）、yyyy-MM-dd（按当天 00:00:00）。
     */
    private static class LenientLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
        private final LocalDateTimeDeserializer iso = LocalDateTimeDeserializer.INSTANCE;

        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getValueAsString();
            if (text == null || text.isBlank()) {
                return null;
            }
            text = text.trim();
            try {
                if (text.length() <= 10) {
                    // 纯日期 yyyy-MM-dd，按当天起始时间处理
                    return LocalDate.parse(text, DATE_FORMATTER).atStartOfDay();
                }
                if (text.contains("T")) {
                    return LocalDateTime.parse(text);
                }
                return LocalDateTime.parse(text, DATE_TIME_FORMATTER);
            } catch (Exception e) {
                // 兜底交给标准 ISO 解析器
                return iso.deserialize(p, ctxt);
            }
        }
    }

    /**
     * LocalDate 宽松反序列化器：兼容 yyyy-MM-dd 与 ISO 日期时间（取日期部分）。
     */
    private static class LenientLocalDateDeserializer extends JsonDeserializer<LocalDate> {
        private final LocalDateDeserializer iso = LocalDateDeserializer.INSTANCE;

        @Override
        public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getValueAsString();
            if (text == null || text.isBlank()) {
                return null;
            }
            text = text.trim();
            try {
                if (text.length() <= 10) {
                    return LocalDate.parse(text, DATE_FORMATTER);
                }
                // 日期时间字符串（含T或空格）取前10位日期
                return LocalDate.parse(text.substring(0, 10), DATE_FORMATTER);
            } catch (Exception e) {
                return iso.deserialize(p, ctxt);
            }
        }
    }
}
