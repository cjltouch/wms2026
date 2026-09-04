package com.example.wms.business.loss.controller;

import com.example.wms.business.loss.dto.req.LossAuditReq;
import com.example.wms.business.loss.dto.req.LossHandleReq;
import com.example.wms.business.loss.dto.req.LossPageReq;
import com.example.wms.business.loss.dto.req.LossSaveReq;
import com.example.wms.business.loss.entity.WmsLossOrder;
import com.example.wms.business.loss.service.WmsLossOrderService;
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

@Tag(name = "报损单管理")
@RestController
@RequestMapping("/api/wms/loss")
@RequiredArgsConstructor
public class WmsLossOrderController {

    private final WmsLossOrderService lossService;

    @Operation(summary = "报损单分页列表")
    @PreAuthorize(hasAuthority = "wms:loss:list")
    @PostMapping("/page")
    public R<?> page(@RequestBody LossPageReq req) {
        return R.ok(lossService.pageLoss(req));
    }

    @Operation(summary = "获取报损单详情")
    @PreAuthorize(hasAuthority = "wms:loss:list")
    @GetMapping("/{id}")
    public R<?> getById(@PathVariable Long id) {
        return R.ok(lossService.getDetailById(id));
    }

    @Operation(summary = "按单号获取报损单")
    @PreAuthorize(hasAuthority = "wms:loss:list")
    @GetMapping("/by-no/{lossNo}")
    public R<WmsLossOrder> getByLossNo(@PathVariable String lossNo) {
        return R.ok(lossService.getByLossNo(lossNo));
    }

    @PreAuthorize(hasAuthority = "wms:loss:add")
    @OperationLog(module = "报损单管理", type = "POST", businessType = 1)
    @Operation(summary = "保存报损单草稿")
    @PostMapping("/save")
    public R<Void> save(@RequestBody LossSaveReq req) {
        lossService.saveLoss(req);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:loss:edit")
    @OperationLog(module = "报损单管理", type = "PUT", businessType = 2)
    @Operation(summary = "修改报损单")
    @PutMapping("/update")
    public R<Void> update(@RequestBody LossSaveReq req) {
        lossService.updateLoss(req);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:loss:remove")
    @OperationLog(module = "报损单管理", type = "DELETE", businessType = 3)
    @Operation(summary = "删除报损单草稿")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        lossService.deleteLoss(id);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:loss:submit")
    @OperationLog(module = "报损单管理", type = "POST", businessType = 2)
    @Operation(summary = "提交报损单")
    @PostMapping("/submit")
    public R<Void> submit(@RequestParam Long id) {
        lossService.submitLoss(id);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:loss:audit")
    @OperationLog(module = "报损单管理", type = "POST", businessType = 2)
    @Operation(summary = "审核报损单")
    @PostMapping("/audit")
    public R<Void> audit(@RequestBody LossAuditReq req) {
        lossService.auditLoss(req);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:loss:handle")
    @OperationLog(module = "报损单管理", type = "POST", businessType = 2)
    @Operation(summary = "处理报损单（扣减库存）")
    @PostMapping("/handle")
    public R<Void> handle(@RequestBody LossHandleReq req) {
        lossService.handleLoss(req);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:loss:void")
    @OperationLog(module = "报损单管理", type = "POST", businessType = 2)
    @Operation(summary = "作废报损单")
    @PostMapping("/void")
    public R<Void> voidOrder(@RequestParam Long id, @RequestParam(required = false) String remark) {
        lossService.voidLoss(id, remark);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:loss:audit")
    @OperationLog(module = "报损单管理", type = "POST", businessType = 2)
    @Operation(summary = "批量审核报损单")
    @PostMapping("/batch-audit")
    public R<Void> batchAudit(@RequestBody BatchAuditReq req) {
        lossService.batchAudit(req);
        return R.ok();
    }
}
