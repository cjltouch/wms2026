package com.example.wms.business.transfer.controller;

import com.example.wms.business.transfer.dto.req.TransferAuditReq;
import com.example.wms.business.transfer.dto.req.TransferPageReq;
import com.example.wms.business.transfer.dto.req.TransferSaveReq;
import com.example.wms.business.transfer.service.WmsTransferOrderService;
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

@Tag(name = "调拨单管理")
@RestController
@RequestMapping("/api/wms/transfer")
@RequiredArgsConstructor
public class WmsTransferOrderController {

    private final WmsTransferOrderService transferOrderService;

    @Operation(summary = "调拨单分页列表")
    @PreAuthorize(hasAuthority = "wms:transfer:list")
    @PostMapping("/page")
    public R<?> page(@RequestBody TransferPageReq req) {
        return R.ok(transferOrderService.pageTransfer(req));
    }

    @Operation(summary = "获取调拨单详情")
    @PreAuthorize(hasAuthority = "wms:transfer:list")
    @GetMapping("/{id}")
    public R<?> getById(@PathVariable Long id) {
        return R.ok(transferOrderService.getDetailById(id));
    }

    @PreAuthorize(hasAuthority = "wms:transfer:add")
    @OperationLog(module = "调拨单管理", type = "POST", businessType = 1)
    @Operation(summary = "保存调拨单草稿")
    @PostMapping("/save")
    public R<Void> save(@RequestBody TransferSaveReq req) {
        transferOrderService.saveTransfer(req);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:transfer:edit")
    @OperationLog(module = "调拨单管理", type = "PUT", businessType = 2)
    @Operation(summary = "修改调拨单")
    @PutMapping("/")
    public R<Void> update(@RequestBody TransferSaveReq req) {
        transferOrderService.updateTransfer(req);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:transfer:remove")
    @OperationLog(module = "调拨单管理", type = "DELETE", businessType = 3)
    @Operation(summary = "删除调拨单草稿")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        transferOrderService.deleteTransfer(id);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:transfer:submit")
    @OperationLog(module = "调拨单管理", type = "POST", businessType = 2)
    @Operation(summary = "提交调拨单")
    @PostMapping("/submit")
    public R<Void> submit(@RequestParam Long id) {
        transferOrderService.submitTransfer(id);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:transfer:audit")
    @OperationLog(module = "调拨单管理", type = "POST", businessType = 2)
    @Operation(summary = "审核调拨单")
    @PostMapping("/audit")
    public R<Void> audit(@RequestBody TransferAuditReq req) {
        transferOrderService.auditTransfer(req);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:transfer:unaudit")
    @OperationLog(module = "调拨单管理", type = "POST", businessType = 2)
    @Operation(summary = "反审核调拨单")
    @PostMapping("/unaudit")
    public R<Void> unaudit(@RequestParam Long id, @RequestParam(required = false) String remark) {
        transferOrderService.unauditTransfer(id, remark);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:transfer:out")
    @OperationLog(module = "调拨单管理", type = "POST", businessType = 2)
    @Operation(summary = "确认出库")
    @PostMapping("/confirm-out")
    public R<Void> confirmOut(@RequestParam Long id) {
        transferOrderService.confirmOut(id);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:transfer:in")
    @OperationLog(module = "调拨单管理", type = "POST", businessType = 2)
    @Operation(summary = "确认入库")
    @PostMapping("/confirm-in")
    public R<Void> confirmIn(@RequestParam Long id) {
        transferOrderService.confirmIn(id);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:transfer:void")
    @OperationLog(module = "调拨单管理", type = "POST", businessType = 2)
    @Operation(summary = "作废调拨单")
    @PostMapping("/void")
    public R<Void> voidOrder(@RequestParam Long id, @RequestParam(required = false) String remark) {
        transferOrderService.voidTransfer(id, remark);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:transfer:audit")
    @OperationLog(module = "调拨单管理", type = "POST", businessType = 2)
    @Operation(summary = "批量审核调拨单")
    @PostMapping("/batch-audit")
    public R<Void> batchAudit(@RequestBody BatchAuditReq req) {
        transferOrderService.batchAudit(req);
        return R.ok();
    }

    @Operation(summary = "导出调拨单")
    @GetMapping("/export")
    public void export(TransferPageReq req, HttpServletResponse response) throws IOException {
        byte[] data = transferOrderService.export(req);
        String fileName = URLEncoder.encode("调拨单列表.xlsx", StandardCharsets.UTF_8);
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        response.getOutputStream().write(data);
    }

    @Operation(summary = "生成下一个调拨单号（DB + yyyyMMdd + 3位序号）")
    @PreAuthorize(hasAuthority = "wms:transfer:add")
    @GetMapping("/generate-no")
    public R<String> generateNo() {
        return R.ok(transferOrderService.generateTransferNo());
    }
}
