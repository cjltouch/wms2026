package com.example.wms.system.controller;

import com.example.wms.common.R;
import com.example.wms.common.annotation.OperationLog;
import com.example.wms.common.annotation.PreAuthorize;
import com.example.wms.system.dto.req.LoginLogPageReq;
import com.example.wms.system.dto.req.OperLogPageReq;
import com.example.wms.system.entity.SysLoginLog;
import com.example.wms.system.entity.SysOperLog;
import com.example.wms.system.service.SysLoginLogService;
import com.example.wms.system.service.SysOperLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "日志管理")
@RestController
@RequestMapping("/api/system/log")
@RequiredArgsConstructor
public class LogController {

    private final SysOperLogService sysOperLogService;
    private final SysLoginLogService sysLoginLogService;

    @Operation(summary = "操作日志分页")
    @GetMapping("/oper-page")
    public R<?> operPage(OperLogPageReq req) {
        return R.ok(sysOperLogService.pageList(req));
    }

    @Operation(summary = "操作日志详情")
    @GetMapping("/oper/{id}")
    public R<SysOperLog> operDetail(@PathVariable Long id) {
        return R.ok(sysOperLogService.getById(id));
    }

    @Operation(summary = "操作日志导出")
    @GetMapping("/oper-export")
    public R<Void> operExport() {
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "system:operlog:remove")
    @OperationLog(module = "操作日志", type = "DELETE", businessType = 9)
    @Operation(summary = "清空操作日志")
    @DeleteMapping("/oper-clean")
    public R<Void> operClean() {
        sysOperLogService.clean();
        return R.ok();
    }

    @Operation(summary = "登录日志分页")
    @GetMapping("/login-page")
    public R<?> loginPage(LoginLogPageReq req) {
        return R.ok(sysLoginLogService.pageList(req));
    }

    @Operation(summary = "登录日志导出")
    @GetMapping("/login-export")
    public R<Void> loginExport() {
        return R.ok();
    }
}
