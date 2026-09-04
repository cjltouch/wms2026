package com.example.wms.business.purchase.controller;

import com.example.wms.business.purchase.dto.req.PurchaseAuditReq;
import com.example.wms.business.purchase.dto.req.PurchasePageReq;
import com.example.wms.business.purchase.dto.req.PurchaseSaveReq;
import com.example.wms.business.purchase.entity.WmsPurchaseOrder;
import com.example.wms.business.purchase.service.WmsPurchaseOrderService;
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

@Tag(name = "采购单管理")
@RestController
@RequestMapping("/api/wms/purchase")
@RequiredArgsConstructor
public class WmsPurchaseOrderController {

    private final WmsPurchaseOrderService purchaseOrderService;

    @Operation(summary = "采购单分页列表")
    @PreAuthorize(hasAuthority = "wms:purchase:list")
    @PostMapping("/page")
    public R<?> page(@RequestBody PurchasePageReq req) {
        return R.ok(purchaseOrderService.pagePurchase(req));
    }

    @Operation(summary = "获取采购单详情")
    @PreAuthorize(hasAuthority = "wms:purchase:list")
    @GetMapping("/{id}")
    public R<?> getById(@PathVariable Long id) {
        return R.ok(purchaseOrderService.getDetailById(id));
    }

    @Operation(summary = "按单号获取采购单")
    @PreAuthorize(hasAuthority = "wms:purchase:list")
    @GetMapping("/by-no/{purchaseNo}")
    public R<WmsPurchaseOrder> getByPurchaseNo(@PathVariable String purchaseNo) {
        return R.ok(purchaseOrderService.getByPurchaseNo(purchaseNo));
    }

    @PreAuthorize(hasAuthority = "wms:purchase:add")
    @OperationLog(module = "采购单管理", type = "POST", businessType = 1)
    @Operation(summary = "保存采购单草稿")
    @PostMapping("/save")
    public R<Void> save(@RequestBody PurchaseSaveReq req) {
        purchaseOrderService.savePurchase(req);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:purchase:edit")
    @OperationLog(module = "采购单管理", type = "PUT", businessType = 2)
    @Operation(summary = "修改采购单")
    @PutMapping("/update")
    public R<Void> update(@RequestBody PurchaseSaveReq req) {
        purchaseOrderService.updatePurchase(req);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:purchase:remove")
    @OperationLog(module = "采购单管理", type = "DELETE", businessType = 3)
    @Operation(summary = "删除采购单草稿")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        purchaseOrderService.deletePurchase(id);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:purchase:submit")
    @OperationLog(module = "采购单管理", type = "POST", businessType = 2)
    @Operation(summary = "提交审核")
    @PostMapping("/submit")
    public R<Void> submit(@RequestParam Long id) {
        purchaseOrderService.submitPurchase(id);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:purchase:audit")
    @OperationLog(module = "采购单管理", type = "POST", businessType = 2)
    @Operation(summary = "审核采购单")
    @PostMapping("/audit")
    public R<Void> audit(@RequestBody PurchaseAuditReq req) {
        purchaseOrderService.auditPurchase(req);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:purchase:unaudit")
    @OperationLog(module = "采购单管理", type = "POST", businessType = 2)
    @Operation(summary = "反审核采购单")
    @PostMapping("/unaudit")
    public R<Void> unaudit(@RequestParam Long id, @RequestParam(required = false) String remark) {
        purchaseOrderService.unauditPurchase(id, remark);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:purchase:void")
    @OperationLog(module = "采购单管理", type = "POST", businessType = 2)
    @Operation(summary = "作废采购单")
    @PostMapping("/void")
    public R<Void> voidOrder(@RequestParam Long id, @RequestParam(required = false) String remark) {
        purchaseOrderService.voidPurchase(id, remark);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:purchase:audit")
    @OperationLog(module = "采购单管理", type = "POST", businessType = 2)
    @Operation(summary = "批量审核采购单")
    @PostMapping("/batch-audit")
    public R<Void> batchAudit(@RequestBody BatchAuditReq req) {
        purchaseOrderService.batchAudit(req);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:purchase:void")
    @OperationLog(module = "采购单管理", type = "POST", businessType = 2)
    @Operation(summary = "批量作废采购单")
    @PostMapping("/batch-void")
    public R<Void> batchVoid(@RequestBody BatchAuditReq req) {
        purchaseOrderService.batchVoid(req);
        return R.ok();
    }

    @Operation(summary = "采购对账分页")
    @PreAuthorize(hasAuthority = "wms:purchase:list")
    @GetMapping("/reconcile/page")
    public R<?> reconcilePage(PurchasePageReq req) {
        return R.ok(purchaseOrderService.pageReconcile(req));
    }

    @Operation(summary = "导出采购单")
    @GetMapping("/export")
    public R<Void> export(PurchasePageReq req) {
        return R.ok();
    }

    @Operation(summary = "导出采购对账")
    @GetMapping("/reconcile/export")
    public R<Void> reconcileExport(PurchasePageReq req) {
        return R.ok();
    }
}
