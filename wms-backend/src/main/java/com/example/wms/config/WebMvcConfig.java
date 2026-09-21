package com.example.wms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /** 上传根目录（相对路径解析为应用启动目录） */
    @Value("${wms.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 把磁盘上的 uploads/ 目录映射为 /uploads/** 的可访问静态资源。
     * 这样前端可以通过 http://host:port/wms-api/uploads/avatars/xxx.png 直接访问头像文件。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absPath = new java.io.File(uploadDir).getAbsolutePath();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absPath + java.io.File.separator);
    }

    /**
     * 注册全局 String → LocalDateTime / LocalDate 转换器。
     * 解决 GET query 参数 / 表单参数绑定到 LocalDateTime 时的类型转换问题
     * （Spring 默认只识别 ISO 格式 2026-09-07T00:00:00）。
     * 支持：yyyy-MM-dd HH:mm:ss、yyyy-MM-dd、2026-09-07T00:00:00（ISO）。
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter dFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        registry.addConverter(String.class, LocalDateTime.class, source -> {
            if (source == null || source.isBlank()) return null;
            String s = source.trim();
            try {
                if (s.length() <= 10) {
                    return LocalDate.parse(s, dFmt).atStartOfDay();
                }
                if (s.contains("T")) {
                    return LocalDateTime.parse(s);
                }
                return LocalDateTime.parse(s, dtFmt);
            } catch (Exception e) {
                throw new IllegalArgumentException("无法解析日期时间: " + s, e);
            }
        });

        registry.addConverter(String.class, LocalDate.class, source -> {
            if (source == null || source.isBlank()) return null;
            String s = source.trim();
            try {
                if (s.length() <= 10) {
                    return LocalDate.parse(s, dFmt);
                }
                return LocalDate.parse(s.substring(0, 10), dFmt);
            } catch (Exception e) {
                throw new IllegalArgumentException("无法解析日期: " + s, e);
            }
        });
    }
}
