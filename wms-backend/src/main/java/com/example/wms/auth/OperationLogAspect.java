package com.example.wms.auth;

import com.example.wms.common.annotation.OperationLog;
import com.example.wms.system.service.SysOperLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    @Lazy
    @Autowired
    private SysOperLogService sysOperLogService;

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint pjp, OperationLog opLog) throws Throwable {
        long start = System.currentTimeMillis();
        boolean success = true;
        String errorMsg = null;
        Object result = null;
        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable e) {
            success = false;
            errorMsg = e.getMessage();
            throw e;
        } finally {
            long cost = System.currentTimeMillis() - start;
            try {
                Long userId = 1L;
                String username = "system";
                LoginUser loginUser = AuthContextHolder.get();
                if (loginUser != null) {
                    userId = loginUser.getUserId();
                    username = loginUser.getUsername();
                }
                if (sysOperLogService != null) {
                    sysOperLogService.saveLog(
                            opLog.module(),
                            opLog.type(),
                            opLog.detail(),
                            opLog.businessType(),
                            userId,
                            username,
                            cost,
                            success,
                            errorMsg
                    );
                }
            } catch (Exception ex) {
                log.error("保存操作日志失败", ex);
            }
        }
    }
}
