package com.example.wms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    private String secret;
    private Integer accessTokenExpireMinutes;
    private Integer refreshTokenExpireDays;
}
