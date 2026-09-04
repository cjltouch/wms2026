package com.example.wms.auth;

import com.example.wms.common.ResultCode;
import com.example.wms.common.annotation.PreAuthorize;
import com.example.wms.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

@Slf4j
@Aspect
@Component
public class PreAuthorizeAspect {

    @Around("@annotation(preAuthorize)")
    public Object around(ProceedingJoinPoint pjp, PreAuthorize preAuthorize) throws Throwable {
        String authority = preAuthorize.hasAuthority();
        String[] anyAuthorities = preAuthorize.hasAnyAuthority();
        if ((authority == null || authority.isEmpty()) && anyAuthorities.length == 0) {
            return pjp.proceed();
        }
        LoginUser loginUser = AuthContextHolder.get();
        if (loginUser == null) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        Set<String> perms = loginUser.getPerms();
        if (perms == null || perms.isEmpty()) {
            log.warn("用户 {} 缺少权限: {}", loginUser.getUserId(), authority);
            throw new BizException(ResultCode.FORBIDDEN);
        }
        if (authority != null && !authority.isEmpty() && !hasPermission(perms, authority)) {
            log.warn("用户 {} 缺少权限: {}", loginUser.getUserId(), authority);
            throw new BizException(ResultCode.FORBIDDEN);
        }
        if (anyAuthorities.length > 0 && Arrays.stream(anyAuthorities).noneMatch(a -> hasPermission(perms, a))) {
            log.warn("用户 {} 缺少权限: {}", loginUser.getUserId(), String.join(",", anyAuthorities));
            throw new BizException(ResultCode.FORBIDDEN);
        }
        return pjp.proceed();
    }

    private boolean hasPermission(Set<String> perms, String authority) {
        if (perms.contains(authority)) {
            return true;
        }
        for (String perm : perms) {
            if (perm == null || perm.isEmpty()) {
                continue;
            }
            // 通配符 * 或 *:*:* 等价于所有权限
            if ("*".equals(perm) || "*:*:*".equals(perm)) {
                return true;
            }
            // 模块通配符：如 module:* 匹配 module:任意
            if (perm.endsWith(":*")) {
                String prefix = perm.substring(0, perm.length() - 1);
                if (authority.startsWith(prefix)) {
                    return true;
                }
            }
        }
        return false;
    }
}
