package com.example.wms.business.sale.controller;

import com.example.wms.business.sale.dto.req.SaleAuditReq;
import com.example.wms.business.sale.dto.req.SalePageReq;
import com.example.wms.business.sale.dto.req.SaleSaveReq;
import com.example.wms.business.sale.service.WmsSaleOrderService;
import com.example.wms.common.BatchAuditReq;
import com.example.wms.common.R;
import com.example.wms.common.annotation.OperationLog;
import com.example.wms.common.annotation.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Tag(name = "销售单管理")
@RestController
@RequestMapping("/api/wms/sale")
@RequiredArgsConstructor
public class WmsSaleOrderController {

    private final WmsSaleOrderService saleOrderService;

    @Operation(summary = "销售单分页列表")
    @PreAuthorize(hasAuthority = "wms:sale:list")
    @PostMapping("/page")
    public R<?> page(@RequestBody SalePageReq req) {
        return R.ok(saleOrderService.pageSale(req));
    }

    @Operation(summary = "获取销售单详情")
    @PreAuthorize(hasAuthority = "wms:sale:list")
    @GetMapping("/{id}")
    public R<?> getById(@PathVariable Long id) {
        return R.ok(saleOrderService.getDetailById(id));
    }

    @PreAuthorize(hasAuthority = "wms:sale:add")
    @OperationLog(module = "销售单管理", type = "POST", businessType = 1)
    @Operation(summary = "保存销售单草稿")
    @PostMapping("/save")
    public R<Void> save(@RequestBody SaleSaveReq req) {
        saleOrderService.saveSale(req);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:sale:edit")
    @OperationLog(module = "销售单管理", type = "PUT", businessType = 2)
    @Operation(summary = "修改销售单")
    @PutMapping("/update")
    public R<Void> update(@RequestBody SaleSaveReq req) {
        saleOrderService.updateSale(req);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:sale:remove")
    @OperationLog(module = "销售单管理", type = "DELETE", businessType = 3)
    @Operation(summary = "删除销售单草稿")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        saleOrderService.deleteSale(id);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:sale:submit")
    @OperationLog(module = "销售单管理", type = "POST", businessType = 2)
    @Operation(summary = "提交销售单")
    @PostMapping("/submit")
    public R<Void> submit(@RequestParam Long id) {
        saleOrderService.submitSale(id);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:sale:audit")
    @OperationLog(module = "销售单管理", type = "POST", businessType = 2)
    @Operation(summary = "审核销售单")
    @PostMapping("/audit")
    public R<Void> audit(@RequestBody SaleAuditReq req) {
        saleOrderService.auditSale(req);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:sale:out")
    @OperationLog(module = "销售单管理", type = "POST", businessType = 2)
    @Operation(summary = "确认出库")
    @PostMapping("/confirm-out")
    public R<Void> confirmOut(@RequestParam Long id) {
        saleOrderService.confirmOut(id);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:sale:pay")
    @OperationLog(module = "销售单管理", type = "POST", businessType = 2)
    @Operation(summary = "确认收款")
    @PostMapping("/confirm-pay")
    public R<Void> confirmPay(@RequestParam Long id, @RequestParam BigDecimal receivedAmount) {
        saleOrderService.confirmPay(id, receivedAmount);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:sale:complete")
    @OperationLog(module = "销售单管理", type = "POST", businessType = 2)
    @Operation(summary = "完成订单")
    @PostMapping("/complete")
    public R<Void> complete(@RequestParam Long id) {
        saleOrderService.completeSale(id);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:sale:void")
    @OperationLog(module = "销售单管理", type = "POST", businessType = 2)
    @Operation(summary = "作废销售单")
    @PostMapping("/void")
    public R<Void> voidOrder(@RequestParam Long id, @RequestParam(required = false) String remark) {
        saleOrderService.voidSale(id, remark);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:sale:audit")
    @OperationLog(module = "销售单管理", type = "POST", businessType = 2)
    @Operation(summary = "批量审核销售单")
    @PostMapping("/batch-audit")
    public R<Void> batchAudit(@RequestBody BatchAuditReq req) {
        saleOrderService.batchAudit(req);
        return R.ok();
    }

    @Operation(summary = "导出销售单")
    @GetMapping("/export")
    public void export(SalePageReq req, HttpServletResponse response) throws IOException {
        byte[] data = saleOrderService.export(req);
        String fileName = URLEncoder.encode("销售单列表.xlsx", StandardCharsets.UTF_8);
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        response.getOutputStream().write(data);
    }

    @Operation(summary = "利润分析")
    @GetMapping("/profit-analysis")
    public R<?> profitAnalysis(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
                              @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {
        return R.ok(saleOrderService.profitAnalysis(startDate, endDate));
    }
}
