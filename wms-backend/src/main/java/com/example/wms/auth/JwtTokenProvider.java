package com.example.wms.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.wms.common.exception.AuthException;
import com.example.wms.config.JwtConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtConfig jwtConfig;

    public String generateAccessToken(Map<String, Object> claims) {
        Algorithm algorithm = Algorithm.HMAC512(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expireDate = new Date(now.getTime() +
                TimeUnit.MINUTES.toMillis(jwtConfig.getAccessTokenExpireMinutes()));
        return JWT.create()
                .withPayload(claims)
                .withIssuedAt(now)
                .withExpiresAt(expireDate)
                .withJWTId(UUID.randomUUID().toString())
                .sign(algorithm);
    }

    public String generateRefreshToken(Long userId) {
        Algorithm algorithm = Algorithm.HMAC512(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expireDate = new Date(now.getTime() +
                TimeUnit.DAYS.toMillis(jwtConfig.getRefreshTokenExpireDays()));
        return JWT.create()
                .withClaim("userId", userId)
                .withClaim("uuid", UUID.randomUUID().toString())
                .withIssuedAt(now)
                .withExpiresAt(expireDate)
                .sign(algorithm);
    }

    public DecodedJWT validateAndGetClaims(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC512(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
            return JWT.require(algorithm).build().verify(token);
        } catch (TokenExpiredException e) {
            throw new AuthException("Token已过期");
        } catch (SignatureVerificationException | JWTDecodeException e) {
            throw new AuthException("Token签名无效");
        } catch (Exception e) {
            throw new AuthException("Token验证失败");
        }
    }
}
