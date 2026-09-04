package com.example.wms.auth;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.wms.system.service.SysUserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Lazy
    @Autowired
    private SysUserService sysUserService;

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = header.substring(7);
        try {
            DecodedJWT jwt = jwtTokenProvider.validateAndGetClaims(token);
            Long userId = jwt.getClaim("userId").asLong();
            if (userId != null) {
                LoginUser loginUser = sysUserService.getLoginUserById(userId);
                if (loginUser != null) {
                    AuthContextHolder.set(loginUser);
                    var authorities = loginUser.getPerms() == null ? Collections.<SimpleGrantedAuthority>emptyList() :
                            loginUser.getPerms().stream()
                                    .map(SimpleGrantedAuthority::new)
                                    .collect(Collectors.toList());
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(loginUser, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception e) {
            log.debug("JWT解析失败，放行: {}", e.getMessage());
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            AuthContextHolder.clear();
            SecurityContextHolder.clearContext();
        }
    }
}
