package com.example.wms.business.report.controller;

import com.example.wms.business.report.service.ReportService;
import com.example.wms.common.annotation.OperationLog;
import com.example.wms.common.annotation.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@Tag(name = "报表导出")
@RestController
@RequestMapping("/api/report/export")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PreAuthorize(hasAuthority = "report:export:purchase")
    @OperationLog(module = "报表导出", type = "POST", businessType = 5)
    @Operation(summary = "采购对账导出")
    @PostMapping("/purchase-reconcile")
    public void purchaseReconcile(@RequestBody(required = false) Map<String, Object> params, HttpServletResponse response) throws IOException {
        reportService.exportPurchaseReconcile(params, response);
    }

    @PreAuthorize(hasAuthority = "report:export:stock-in")
    @OperationLog(module = "报表导出", type = "POST", businessType = 5)
    @Operation(summary = "入库明细导出")
    @PostMapping("/stock-in-detail")
    public void stockInDetail(@RequestBody(required = false) Map<String, Object> params, HttpServletResponse response) throws IOException {
        reportService.exportStockInDetail(params, response);
    }

    @PreAuthorize(hasAuthority = "report:export:stock-out")
    @OperationLog(module = "报表导出", type = "POST", businessType = 5)
    @Operation(summary = "出库明细导出")
    @PostMapping("/stock-out-detail")
    public void stockOutDetail(@RequestBody(required = false) Map<String, Object> params, HttpServletResponse response) throws IOException {
        reportService.exportStockOutDetail(params, response);
    }

    @PreAuthorize(hasAuthority = "report:export:inventory-balance")
    @OperationLog(module = "报表导出", type = "POST", businessType = 5)
    @Operation(summary = "库存余额导出")
    @PostMapping("/inventory-balance")
    public void inventoryBalance(@RequestBody(required = false) Map<String, Object> params, HttpServletResponse response) throws IOException {
        reportService.exportInventoryBalance(params, response);
    }

    @PreAuthorize(hasAuthority = "report:export:inventory-batch")
    @OperationLog(module = "报表导出", type = "POST", businessType = 5)
    @Operation(summary = "批次库存导出")
    @PostMapping("/inventory-batch")
    public void inventoryBatch(@RequestBody(required = false) Map<String, Object> params, HttpServletResponse response) throws IOException {
        reportService.exportInventoryBatch(params, response);
    }

    @PreAuthorize(hasAuthority = "report:export:expire")
    @OperationLog(module = "报表导出", type = "POST", businessType = 5)
    @Operation(summary = "临期预警导出")
    @PostMapping("/expire-warning")
    public void expireWarning(@RequestBody(required = false) Map<String, Object> params, HttpServletResponse response) throws IOException {
        reportService.exportExpireWarning(params, response);
    }
}
