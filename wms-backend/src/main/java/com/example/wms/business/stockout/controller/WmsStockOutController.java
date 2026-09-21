package com.example.wms.business.stockout.controller;

import com.example.wms.business.stockout.dto.req.StockOutAuditReq;
import com.example.wms.business.stockout.dto.req.StockOutPageReq;
import com.example.wms.business.stockout.dto.req.StockOutSaveReq;
import com.example.wms.business.stockout.entity.WmsStockOut;
import com.example.wms.business.stockout.service.WmsStockOutService;
import com.example.wms.common.BatchAuditReq;
import com.example.wms.common.R;
import com.example.wms.common.annotation.OperationLog;
import com.example.wms.common.annotation.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "出库单管理")
@RestController
@RequestMapping("/api/wms/stock-out")
@RequiredArgsConstructor
public class WmsStockOutController {

    private final WmsStockOutService stockOutService;

    @Operation(summary = "出库单分页列表")
    @PreAuthorize(hasAuthority = "wms:stock-out:list")
    @PostMapping("/page")
    public R<?> page(@RequestBody StockOutPageReq req) {
        return R.ok(stockOutService.pageStockOut(req));
    }

    @Operation(summary = "获取出库单详情")
    @PreAuthorize(hasAuthority = "wms:stock-out:list")
    @GetMapping("/{id}")
    public R<?> getById(@PathVariable Long id) {
        return R.ok(stockOutService.getDetailById(id));
    }

    @Operation(summary = "按单号获取出库单")
    @PreAuthorize(hasAuthority = "wms:stock-out:list")
    @GetMapping("/by-no/{stockOutNo}")
    public R<WmsStockOut> getByStockOutNo(@PathVariable String stockOutNo) {
        return R.ok(stockOutService.getByStockOutNo(stockOutNo));
    }

    @PreAuthorize(hasAuthority = "wms:stock-out:add")
    @OperationLog(module = "出库单管理", type = "POST", businessType = 1)
    @Operation(summary = "保存出库单草稿")
    @PostMapping("/save")
    public R<Void> save(@RequestBody StockOutSaveReq req) {
        stockOutService.saveStockOut(req);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:stock-out:edit")
    @OperationLog(module = "出库单管理", type = "PUT", businessType = 2)
    @Operation(summary = "修改出库单")
    @PutMapping("/update")
    public R<Void> update(@RequestBody StockOutSaveReq req) {
        stockOutService.updateStockOut(req);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:stock-out:remove")
    @OperationLog(module = "出库单管理", type = "DELETE", businessType = 3)
    @Operation(summary = "删除出库单草稿")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        stockOutService.deleteStockOut(id);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:stock-out:submit")
    @OperationLog(module = "出库单管理", type = "POST", businessType = 2)
    @Operation(summary = "提交出库单")
    @PostMapping("/submit")
    public R<Void> submit(@RequestParam Long id) {
        stockOutService.submitStockOut(id);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:stock-out:allocate")
    @OperationLog(module = "出库单管理", type = "POST", businessType = 2)
    @Operation(summary = "分配预览(FIFO/FEFO)")
    @PostMapping("/preview-allocation")
    public R<?> previewAllocation(@RequestParam Long id) {
        return R.ok(stockOutService.previewAllocation(id));
    }

    @PreAuthorize(hasAuthority = "wms:stock-out:lock")
    @OperationLog(module = "出库单管理", type = "POST", businessType = 2)
    @Operation(summary = "锁定库存")
    @PostMapping("/lock-inventory")
    public R<Void> lockInventory(@RequestParam Long id) {
        stockOutService.lockInventory(id);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:stock-out:pick")
    @OperationLog(module = "出库单管理", type = "POST", businessType = 2)
    @Operation(summary = "拣货确认")
    @PostMapping("/pick-confirm")
    public R<Void> pickConfirm(@RequestParam Long id) {
        stockOutService.pickConfirm(id);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:stock-out:audit")
    @OperationLog(module = "出库单管理", type = "POST", businessType = 2)
    @Operation(summary = "审核出库单")
    @PostMapping("/audit")
    public R<Void> audit(@RequestBody StockOutAuditReq req) {
        stockOutService.auditStockOut(req);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:stock-out:unaudit")
    @OperationLog(module = "出库单管理", type = "POST", businessType = 2)
    @Operation(summary = "反审核出库单")
    @PostMapping("/unaudit")
    public R<Void> unaudit(@RequestParam Long id, @RequestParam(required = false) String remark) {
        stockOutService.unauditStockOut(id, remark);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:stock-out:void")
    @OperationLog(module = "出库单管理", type = "POST", businessType = 2)
    @Operation(summary = "作废出库单")
    @PostMapping("/void")
    public R<Void> voidOrder(@RequestParam Long id, @RequestParam(required = false) String remark) {
        stockOutService.voidStockOut(id, remark);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:stock-out:audit")
    @OperationLog(module = "出库单管理", type = "POST", businessType = 2)
    @Operation(summary = "批量审核出库单")
    @PostMapping("/batch-audit")
    public R<Void> batchAudit(@RequestBody BatchAuditReq req) {
        stockOutService.batchAudit(req);
        return R.ok();
    }

    @Operation(summary = "导出出库明细（只导出已审核）")
    @GetMapping("/export")
    public void export(StockOutPageReq req, jakarta.servlet.http.HttpServletResponse response) {
        stockOutService.exportStockOutItems(req, response);
    }
}
