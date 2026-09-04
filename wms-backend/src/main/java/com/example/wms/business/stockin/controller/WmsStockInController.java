package com.example.wms.business.stockin.controller;

import com.example.wms.business.stockin.dto.req.StockInAuditReq;
import com.example.wms.business.stockin.dto.req.StockInPageReq;
import com.example.wms.business.stockin.dto.req.StockInSaveReq;
import com.example.wms.business.stockin.entity.WmsStockIn;
import com.example.wms.business.stockin.service.WmsStockInService;
import com.example.wms.common.BatchAuditReq;
import com.example.wms.common.R;
import com.example.wms.common.annotation.OperationLog;
import com.example.wms.common.annotation.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Tag(name = "入库单管理")
@RestController
@RequestMapping("/api/wms/stock-in")
@RequiredArgsConstructor
public class WmsStockInController {

    private final WmsStockInService stockInService;

    @Operation(summary = "入库单分页列表")
    @PreAuthorize(hasAuthority = "wms:stock-in:list")
    @PostMapping("/page")
    public R<?> page(@RequestBody StockInPageReq req) {
        return R.ok(stockInService.pageStockIn(req));
    }

    @Operation(summary = "获取入库单详情")
    @PreAuthorize(hasAuthority = "wms:stock-in:list")
    @GetMapping("/{id}")
    public R<?> getById(@PathVariable Long id) {
        return R.ok(stockInService.getDetailById(id));
    }

    @Operation(summary = "按单号获取入库单")
    @PreAuthorize(hasAuthority = "wms:stock-in:list")
    @GetMapping("/by-no/{stockInNo}")
    public R<WmsStockIn> getByStockInNo(@PathVariable String stockInNo) {
        return R.ok(stockInService.getByStockInNo(stockInNo));
    }

    @Operation(summary = "按源单号拉取待入库数据")
    @PostMapping("/from-source")
    public R<?> fromSource(@RequestParam String sourceBillNo) {
        return R.ok(stockInService.fromSource(sourceBillNo));
    }

    @PreAuthorize(hasAuthority = "wms:stock-in:add")
    @OperationLog(module = "入库单管理", type = "POST", businessType = 1)
    @Operation(summary = "保存入库单草稿")
    @PostMapping("/save")
    public R<Void> save(@RequestBody StockInSaveReq req) {
        stockInService.saveStockIn(req);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:stock-in:edit")
    @OperationLog(module = "入库单管理", type = "PUT", businessType = 2)
    @Operation(summary = "修改入库单")
    @PutMapping("/update")
    public R<Void> update(@RequestBody StockInSaveReq req) {
        stockInService.updateStockIn(req);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:stock-in:remove")
    @OperationLog(module = "入库单管理", type = "DELETE", businessType = 3)
    @Operation(summary = "删除入库单草稿")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        stockInService.deleteStockIn(id);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:stock-in:submit")
    @OperationLog(module = "入库单管理", type = "POST", businessType = 2)
    @Operation(summary = "提交入库单")
    @PostMapping("/submit")
    public R<Void> submit(@RequestParam Long id) {
        stockInService.submitStockIn(id);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:stock-in:audit")
    @OperationLog(module = "入库单管理", type = "POST", businessType = 2)
    @Operation(summary = "上架审核入库单")
    @PostMapping("/audit")
    public R<Void> audit(@RequestBody StockInAuditReq req) {
        stockInService.auditStockIn(req);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:stock-in:unaudit")
    @OperationLog(module = "入库单管理", type = "POST", businessType = 2)
    @Operation(summary = "反审核入库单(5分钟内)")
    @PostMapping("/unaudit")
    public R<Void> unaudit(@RequestParam Long id, @RequestParam(required = false) String remark) {
        stockInService.unauditStockIn(id, remark);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:stock-in:void")
    @OperationLog(module = "入库单管理", type = "POST", businessType = 2)
    @Operation(summary = "作废入库单")
    @PostMapping("/void")
    public R<Void> voidOrder(@RequestParam Long id, @RequestParam(required = false) String remark) {
        stockInService.voidStockIn(id, remark);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:stock-in:audit")
    @OperationLog(module = "入库单管理", type = "POST", businessType = 2)
    @Operation(summary = "批量上架审核")
    @PostMapping("/batch-audit")
    public R<Void> batchAudit(@RequestBody BatchAuditReq req) {
        stockInService.batchAudit(req);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:stock-in:allocate")
    @OperationLog(module = "入库单管理", type = "POST", businessType = 2)
    @Operation(summary = "自动分配库位")
    @PostMapping("/allocate-location")
    public R<Void> allocateLocation(@RequestParam Long id) {
        stockInService.allocateLocation(id);
        return R.ok();
    }

    @Operation(summary = "导出入库单")
    @GetMapping("/export")
    public void export(StockInPageReq req, HttpServletResponse response) throws IOException {
        byte[] data = stockInService.export(req);
        String fileName = URLEncoder.encode("入库单列表.xlsx", StandardCharsets.UTF_8);
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        response.getOutputStream().write(data);
    }
}
