package com.example.wms.system.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.wms.common.PageRsp;
import com.example.wms.system.dto.req.OperLogPageReq;
import com.example.wms.system.entity.SysOperLog;
import com.example.wms.system.mapper.SysOperLogMapper;
import com.example.wms.system.service.SysOperLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Slf4j
@Service
public class SysOperLogServiceImpl extends ServiceImpl<SysOperLogMapper, SysOperLog> implements SysOperLogService {

    @Async
    @Override
    public void saveLog(String module, String type, String detail, int businessType,
                        Long userId, String username, Long costTime, boolean success, String errorMsg) {
        try {
            SysOperLog operLog = new SysOperLog();
            operLog.setTitle(module);
            operLog.setBusinessType(businessType);
            operLog.setOperatorType(1);
            operLog.setOperName(username);
            operLog.setStatus(success ? 1 : 0);
            operLog.setCostTime(costTime);
            operLog.setOperTime(LocalDateTime.now());
            operLog.setErrorMsg(StrUtil.sub(errorMsg, 0, 2000));
            operLog.setJsonResult(StrUtil.sub(detail, 0, 2000));
            operLog.setMethod(type);

            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ip = getClientIp(request);
                operLog.setOperIp(ip);
                operLog.setOperUrl(request.getRequestURI());
                operLog.setRequestMethod(request.getMethod());
                String uaStr = request.getHeader("User-Agent");
                if (StringUtils.hasText(uaStr)) {
                    try {
                        UserAgent ua = UserAgentUtil.parse(uaStr);
                        if (ua != null) {
                            operLog.setOperLocation(ua.getOs().getName());
                        }
                    } catch (Exception e) {
                        log.warn("解析UserAgent失败", e);
                    }
                }
            }
            this.save(operLog);
        } catch (Exception e) {
            log.error("保存操作日志失败", e);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    @Override
    public PageRsp<SysOperLog> pageList(OperLogPageReq req) {
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(req.getTitle())) {
            wrapper.like(SysOperLog::getTitle, req.getTitle());
        }
        if (req.getBusinessType() != null) {
            wrapper.eq(SysOperLog::getBusinessType, req.getBusinessType());
        }
        if (StringUtils.hasText(req.getOperName())) {
            wrapper.like(SysOperLog::getOperName, req.getOperName());
        }
        if (req.getStatus() != null) {
            wrapper.eq(SysOperLog::getStatus, req.getStatus());
        }
        if (req.getDateRangeStart() != null) {
            wrapper.ge(SysOperLog::getOperTime, req.getDateRangeStart());
        }
        if (req.getDateRangeEnd() != null) {
            wrapper.le(SysOperLog::getOperTime, req.getDateRangeEnd());
        }
        wrapper.orderByDesc(SysOperLog::getOperTime);
        IPage<SysOperLog> page = this.page(new Page<>(req.getPageNum(), req.getPageSize()), wrapper);
        return new PageRsp<>(page.getTotal(), page.getRecords(), req.getPageNum(), req.getPageSize());
    }

    @Override
    public void clean() {
        this.remove(new LambdaQueryWrapper<>());
    }
}
