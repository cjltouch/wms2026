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
import com.example.wms.common.exception.BizException;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Tag(name = "认证管理")
@RestController
@RequestMapping("/api/system/auth")
@RequiredArgsConstructor
public class AuthController {

    /** 上传根目录（相对于应用运行目录），头像会存到该目录下的 avatars/ 子目录 */
    @Value("${wms.upload-dir:uploads}")
    private String uploadDir;

    /** 允许上传的图片 MIME 类型 */
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/png", "image/jpeg", "image/jpg", "image/gif", "image/webp", "image/svg+xml"
    );

    /** 单文件最大 5MB */
    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024L;

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
        rsp.setRealName(loginUser.getRealName());
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

    @Operation(summary = "上传头像（同时更新当前用户头像）")
    @PostMapping("/upload-avatar")
    public R<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        LoginUser loginUser = AuthContextHolder.get();
        if (loginUser == null) {
            throw new AuthException("未登录");
        }
        if (file.isEmpty()) {
            throw new BizException("请选择要上传的图片");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new BizException("头像文件大小不能超过 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new BizException("仅支持 PNG、JPG、GIF、WEBP、SVG 格式的图片");
        }

        // 构造安全的文件名：userId + 时间戳 + 原始扩展名
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf('.'));
            if (ext.length() > 8) ext = ext.substring(0, 9); // 防超长扩展名
        }
        String safeFilename = "avatar_" + loginUser.getUserId() + "_" + System.currentTimeMillis() + ext;

        // 保存到 upload-dir/avatars/（确保用绝对路径，避免 Tomcat 临时目录漂移）
        File avatarDir = new File(uploadDir, "avatars");
        if (!avatarDir.isAbsolute()) {
            avatarDir = avatarDir.getAbsoluteFile();
        }
        if (!avatarDir.exists() && !avatarDir.mkdirs()) {
            throw new BizException("无法创建头像存储目录：" + avatarDir.getAbsolutePath());
        }
        File destFile = new File(avatarDir, safeFilename);
        try {
            file.transferTo(destFile);
        } catch (IOException e) {
            throw new BizException("头像保存失败：" + e.getMessage());
        }

        // 构造可访问的相对路径（由 WebMvcConfig 资源映射暴露）
        String avatarUrl = "/uploads/avatars/" + safeFilename;
        sysUserService.updateAvatar(loginUser.getUserId(), avatarUrl);

        Map<String, String> result = new HashMap<>();
        result.put("avatar", avatarUrl);
        return R.ok(result);
    }

    @Operation(summary = "更新当前用户头像（使用指定 URL）")
    @PutMapping("/update-avatar")
    public R<Void> updateAvatar(@RequestBody Map<String, String> body) {
        LoginUser loginUser = AuthContextHolder.get();
        if (loginUser == null) {
            throw new AuthException("未登录");
        }
        String avatar = body.get("avatar");
        if (avatar == null || avatar.isBlank()) {
            throw new BizException("头像地址不能为空");
        }
        sysUserService.updateAvatar(loginUser.getUserId(), avatar);
        return R.ok();
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
