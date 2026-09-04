package com.example.wms.business.check.controller;

import com.example.wms.business.check.dto.req.CheckAuditReq;
import com.example.wms.business.check.dto.req.CheckItemInputReq;
import com.example.wms.business.check.dto.req.CheckPageReq;
import com.example.wms.business.check.dto.req.CheckSaveReq;
import com.example.wms.business.check.service.WmsCheckOrderService;
import com.example.wms.common.BatchAuditReq;
import com.example.wms.common.R;
import com.example.wms.common.annotation.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Tag(name = "盘点单管理")
@RestController
@RequestMapping("/api/wms/check")
@RequiredArgsConstructor
public class WmsCheckOrderController {

    private final WmsCheckOrderService checkOrderService;

    @Operation(summary = "盘点单分页列表")
    @PreAuthorize(hasAuthority = "wms:check:list")
    @PostMapping("/page")
    public R<?> page(@RequestBody CheckPageReq req) {
        return R.ok(checkOrderService.pageCheck(req));
    }

    @Operation(summary = "获取盘点单详情")
    @PreAuthorize(hasAuthority = "wms:check:list")
    @GetMapping("/{id}")
    public R<?> getById(@PathVariable Long id) {
        return R.ok(checkOrderService.getDetailById(id));
    }

    @Operation(summary = "保存盘点单草稿")
    @PreAuthorize(hasAuthority = "wms:check:add")
    @PostMapping("/save")
    public R<Void> save(@RequestBody CheckSaveReq req) {
        checkOrderService.saveCheck(req);
        return R.ok();
    }

    @Operation(summary = "加载库存数据生成盘点明细")
    @PreAuthorize(hasAuthority = "wms:check:add")
    @PostMapping("/load-inventory")
    public R<?> loadInventory(@RequestParam Long checkId,
                              @RequestParam(required = false) Long warehouseId,
                              @RequestParam(required = false) Long areaId) {
        return R.ok(checkOrderService.loadInventory(checkId, warehouseId, areaId));
    }

    @Operation(summary = "开始盘点")
    @PreAuthorize(hasAuthority = "wms:check:submit")
    @PostMapping("/start-check")
    public R<Void> startCheck(@RequestParam Long id) {
        checkOrderService.startCheck(id);
        return R.ok();
    }

    @Operation(summary = "录入实盘数据")
    @PreAuthorize(hasAuthority = "wms:check:edit")
    @PostMapping("/input-actual")
    public R<Void> inputActual(@RequestBody CheckItemInputReq req) {
        checkOrderService.inputActual(req);
        return R.ok();
    }

    @Operation(summary = "完成盘点")
    @PreAuthorize(hasAuthority = "wms:check:submit")
    @PostMapping("/finish-check")
    public R<Void> finishCheck(@RequestParam Long id) {
        checkOrderService.finishCheck(id);
        return R.ok();
    }

    @Operation(summary = "审核盘点单")
    @PreAuthorize(hasAuthority = "wms:check:audit")
    @PostMapping("/audit")
    public R<Void> audit(@RequestBody CheckAuditReq req) {
        checkOrderService.auditCheck(req);
        return R.ok();
    }

    @Operation(summary = "处理盘点单(调整库存)")
    @PreAuthorize(hasAuthority = "wms:check:handle")
    @PostMapping("/handle")
    public R<Void> handle(@RequestParam Long id, @RequestParam(required = false) String remark) {
        checkOrderService.handleCheck(id, remark);
        return R.ok();
    }

    @Operation(summary = "作废盘点单")
    @PreAuthorize(hasAuthority = "wms:check:void")
    @PostMapping("/void")
    public R<Void> voidOrder(@RequestParam Long id, @RequestParam(required = false) String remark) {
        checkOrderService.voidCheck(id, remark);
        return R.ok();
    }

    @Operation(summary = "批量审核盘点单")
    @PreAuthorize(hasAuthority = "wms:check:audit")
    @PostMapping("/batch-audit")
    public R<Void> batchAudit(@RequestBody BatchAuditReq req) {
        checkOrderService.batchAudit(req);
        return R.ok();
    }

    @Operation(summary = "导出盘点单")
    @GetMapping("/export")
    public void export(CheckPageReq req, HttpServletResponse response) throws IOException {
        byte[] data = checkOrderService.export(req);
        String fileName = URLEncoder.encode("盘点单列表.xlsx", StandardCharsets.UTF_8);
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        response.getOutputStream().write(data);
    }

    @Operation(summary = "盘点差异汇总")
    @PreAuthorize(hasAuthority = "wms:check:list")
    @GetMapping("/diff-summary")
    public R<?> diffSummary(@RequestParam(required = false) Long warehouseId) {
        return R.ok(checkOrderService.diffSummary(warehouseId));
    }
}
