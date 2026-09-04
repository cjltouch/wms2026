package com.example.wms.system.service.impl;

import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.wms.common.PageRsp;
import com.example.wms.system.dto.req.LoginLogPageReq;
import com.example.wms.system.entity.SysLoginLog;
import com.example.wms.system.mapper.SysLoginLogMapper;
import com.example.wms.system.service.SysLoginLogService;
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
public class SysLoginLogServiceImpl extends ServiceImpl<SysLoginLogMapper, SysLoginLog> implements SysLoginLogService {

    @Override
    public PageRsp<SysLoginLog> pageList(LoginLogPageReq req) {
        LambdaQueryWrapper<SysLoginLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(req.getUserName())) {
            wrapper.like(SysLoginLog::getUserName, req.getUserName());
        }
        if (StringUtils.hasText(req.getStatus())) {
            wrapper.eq(SysLoginLog::getStatus, req.getStatus());
        }
        if (StringUtils.hasText(req.getIpaddr())) {
            wrapper.like(SysLoginLog::getIpaddr, req.getIpaddr());
        }
        if (req.getDateRangeStart() != null) {
            wrapper.ge(SysLoginLog::getLoginTime, req.getDateRangeStart());
        }
        if (req.getDateRangeEnd() != null) {
            wrapper.le(SysLoginLog::getLoginTime, req.getDateRangeEnd());
        }
        wrapper.orderByDesc(SysLoginLog::getLoginTime);
        IPage<SysLoginLog> page = this.page(new Page<>(req.getPageNum(), req.getPageSize()), wrapper);
        return new PageRsp<>(page.getTotal(), page.getRecords(), req.getPageNum(), req.getPageSize());
    }

    @Async
    @Override
    public void saveLoginLog(String userName, String ipaddr, boolean success, String msg) {
        try {
            SysLoginLog loginLog = new SysLoginLog();
            loginLog.setUserName(userName);
            loginLog.setIpaddr(ipaddr);
            loginLog.setStatus(success ? "0" : "1");
            loginLog.setMsg(msg);
            loginLog.setLoginTime(LocalDateTime.now());

            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String uaStr = request.getHeader("User-Agent");
                if (StringUtils.hasText(uaStr)) {
                    try {
                        UserAgent ua = UserAgentUtil.parse(uaStr);
                        if (ua != null) {
                            loginLog.setBrowser(ua.getBrowser().getName());
                            loginLog.setOs(ua.getOs().getName());
                        }
                    } catch (Exception e) {
                        log.warn("解析UserAgent失败", e);
                    }
                }
            }
            this.save(loginLog);
        } catch (Exception e) {
            log.error("保存登录日志失败", e);
        }
    }
}
