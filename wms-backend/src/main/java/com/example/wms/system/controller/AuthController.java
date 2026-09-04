package com.example.wms.system.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.wms.auth.AuthContextHolder;
import com.example.wms.auth.JwtTokenProvider;
import com.example.wms.auth.LoginUser;
import com.example.wms.common.R;
import com.example.wms.common.exception.AuthException;
import com.example.wms.config.JwtConfig;
import com.example.wms.system.dto.req.LoginReq;
import com.example.wms.system.dto.req.RefreshTokenReq;
import com.example.wms.system.dto.rsp.CaptchaRsp;
import com.example.wms.system.dto.rsp.LoginRsp;
import com.example.wms.system.dto.rsp.LoginUserInfoRsp;
import com.example.wms.system.entity.SysUser;
import com.example.wms.system.service.SysLoginLogService;
import com.example.wms.system.service.SysMenuService;
import com.example.wms.system.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Tag(name = "认证管理")
@RestController
@RequestMapping("/api/system/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService sysUserService;
    private final SysMenuService sysMenuService;
    private final SysLoginLogService sysLoginLogService;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtConfig jwtConfig;
    private final PasswordEncoder passwordEncoder;

    @Operation(summary = "登录")
    @PostMapping("/login")
    public R<LoginRsp> login(@Valid @RequestBody LoginReq req, HttpServletRequest request) {
        SysUser user = sysUserService.getByUserName(req.getUsername());
        String ip = getClientIp(request);
        if (user == null) {
            sysLoginLogService.saveLoginLog(req.getUsername(), ip, false, "用户不存在");
            throw new AuthException("用户名或密码错误");
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            sysLoginLogService.saveLoginLog(req.getUsername(), ip, false, "密码错误");
            throw new AuthException("用户名或密码错误");
        }
        if ("1".equals(user.getStatus())) {
            sysLoginLogService.saveLoginLog(req.getUsername(), ip, false, "账号已停用");
            throw new AuthException("账号已停用");
        }

        user.setLoginIp(ip);
        user.setLoginTime(java.time.LocalDateTime.now());
        sysUserService.updateById(user);

        sysLoginLogService.saveLoginLog(req.getUsername(), ip, true, "登录成功");

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId());
        claims.put("username", user.getUserName());
        String accessToken = jwtTokenProvider.generateAccessToken(claims);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId());

        LoginRsp rsp = new LoginRsp();
        rsp.setAccessToken(accessToken);
        rsp.setRefreshToken(refreshToken);
        rsp.setExpiresIn(TimeUnit.MINUTES.toMillis(jwtConfig.getAccessTokenExpireMinutes()));
        return R.ok(rsp);
    }

    @Operation(summary = "刷新Token")
    @PostMapping("/refresh-token")
    public R<LoginRsp> refreshToken(@Valid @RequestBody RefreshTokenReq req) {
        DecodedJWT jwt = jwtTokenProvider.validateAndGetClaims(req.getRefreshToken());
        Long userId = jwt.getClaim("userId").asLong();
        if (userId == null) {
            throw new AuthException("RefreshToken无效");
        }
        SysUser user = sysUserService.getById(userId);
        if (user == null || "1".equals(user.getStatus())) {
            throw new AuthException("用户不存在或已停用");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId());
        claims.put("username", user.getUserName());
        String accessToken = jwtTokenProvider.generateAccessToken(claims);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId());

        LoginRsp rsp = new LoginRsp();
        rsp.setAccessToken(accessToken);
        rsp.setRefreshToken(newRefreshToken);
        rsp.setExpiresIn(TimeUnit.MINUTES.toMillis(jwtConfig.getAccessTokenExpireMinutes()));
        return R.ok(rsp);
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public R<Void> logout() {
        AuthContextHolder.clear();
        return R.ok();
    }

    @Operation(summary = "修改当前用户密码")
    @PostMapping("/change-password")
    public R<Void> changePassword(@Valid @RequestBody com.example.wms.system.dto.req.ChangePasswordReq req) {
        LoginUser loginUser = AuthContextHolder.get();
        if (loginUser == null) {
            throw new AuthException("未登录");
        }
        SysUser user = sysUserService.getById(loginUser.getUserId());
        if (user == null) {
            throw new AuthException("用户不存在");
        }
        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new AuthException("原密码错误");
        }
        if (passwordEncoder.matches(req.getNewPassword(), user.getPassword())) {
            throw new com.example.wms.common.exception.BizException("新密码不能与原密码相同");
        }
        sysUserService.resetPwd(user.getUserId(), req.getNewPassword());
        return R.ok();
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/user-info")
    public R<LoginUserInfoRsp> userInfo() {
        LoginUser loginUser = AuthContextHolder.get();
        if (loginUser == null) {
            throw new AuthException("未登录");
        }
        SysUser user = sysUserService.getById(loginUser.getUserId());
        LoginUserInfoRsp rsp = new LoginUserInfoRsp();
        rsp.setUserId(loginUser.getUserId());
        rsp.setUsername(loginUser.getUsername());
        rsp.setNickname(loginUser.getNickname());
        rsp.setAvatar(user != null ? user.getAvatar() : null);
        rsp.setRoles(loginUser.getRoleCodes());
        rsp.setPerms(loginUser.getPerms());
        rsp.setRouters(sysMenuService.getUserMenuTree(loginUser.getUserId()));
        return R.ok(rsp);
    }

    @Operation(summary = "获取验证码")
    @GetMapping("/captcha")
    public R<CaptchaRsp> captcha() {
        CaptchaRsp rsp = new CaptchaRsp();
        rsp.setEnabled(true);
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(111, 36, 4, 4);
        rsp.setUuid(IdUtil.fastSimpleUUID());
        rsp.setImg("data:image/svg+xml;base64," + captcha.getImageBase64Data());
        return R.ok(rsp);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (!org.springframework.util.StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (!org.springframework.util.StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (!org.springframework.util.StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (!org.springframework.util.StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
