package com.example.wms.business.basedata.supplier.controller;

import com.example.wms.business.basedata.supplier.dto.req.SupplierBatchDeleteReq;
import com.example.wms.business.basedata.supplier.dto.req.SupplierPageReq;
import com.example.wms.business.basedata.supplier.dto.req.SupplierSaveReq;
import com.example.wms.business.basedata.supplier.entity.WmsSupplier;
import com.example.wms.business.basedata.supplier.service.WmsSupplierService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "供应商管理")
@RestController
@RequestMapping("/api/wms/supplier")
@RequiredArgsConstructor
public class WmsSupplierController {

    private final WmsSupplierService wmsSupplierService;

    @Operation(summary = "供应商分页列表")
    @PreAuthorize(hasAuthority = "wms:supplier:list")
    @GetMapping("/page")
    public R<?> page(SupplierPageReq req) {
        return R.ok(wmsSupplierService.pageSupplier(req));
    }

    @Operation(summary = "获取全部供应商（下拉选择）")
    @GetMapping("/list-all")
    public R<List<WmsSupplier>> listAll() {
        return R.ok(wmsSupplierService.listAll());
    }

    @Operation(summary = "获取供应商详情")
    @PreAuthorize(hasAuthority = "wms:supplier:list")
    @GetMapping("/{id}")
    public R<WmsSupplier> getById(@PathVariable Long id) {
        return R.ok(wmsSupplierService.getById(id));
    }

    @PreAuthorize(hasAuthority = "wms:supplier:add")
    @OperationLog(module = "供应商管理", type = "POST", businessType = 1)
    @Operation(summary = "新增供应商")
    @PostMapping
    public R<Void> post(@RequestBody SupplierSaveReq req) {
        wmsSupplierService.saveSupplier(req);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:supplier:edit")
    @OperationLog(module = "供应商管理", type = "PUT", businessType = 2)
    @Operation(summary = "修改供应商")
    @PutMapping
    public R<Void> put(@RequestBody SupplierSaveReq req) {
        wmsSupplierService.updateSupplier(req);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:supplier:remove")
    @OperationLog(module = "供应商管理", type = "DELETE", businessType = 3)
    @Operation(summary = "删除供应商")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        wmsSupplierService.deleteSupplier(id);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:supplier:remove")
    @OperationLog(module = "供应商管理", type = "DELETE", businessType = 3)
    @Operation(summary = "批量删除供应商")
    @DeleteMapping("/batch-delete")
    public R<Void> batchDelete(@RequestBody SupplierBatchDeleteReq req) {
        wmsSupplierService.batchDeleteSupplier(req.getIds());
        return R.ok();
    }

    @Operation(summary = "导入供应商")
    @PostMapping("/import")
    public R<Void> importData() {
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:supplier:export")
    @Operation(summary = "导出供应商")
    @GetMapping("/export")
    public R<Void> export() {
        return R.ok();
    }
}
