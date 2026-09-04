package com.example.wms.business.basedata.warehouse.controller;

import com.example.wms.business.basedata.warehouse.dto.req.WarehouseBatchDeleteReq;
import com.example.wms.business.basedata.warehouse.dto.req.WarehousePageReq;
import com.example.wms.business.basedata.warehouse.dto.req.WarehouseSaveReq;
import com.example.wms.business.basedata.warehouse.entity.WmsWarehouse;
import com.example.wms.business.basedata.warehouse.service.WmsWarehouseService;
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

@Tag(name = "仓库管理")
@RestController
@RequestMapping("/api/wms/warehouse")
@RequiredArgsConstructor
public class WmsWarehouseController {

    private final WmsWarehouseService wmsWarehouseService;

    @Operation(summary = "仓库分页列表")
    @PreAuthorize(hasAuthority = "wms:warehouse:list")
    @GetMapping("/page")
    public R<?> page(WarehousePageReq req) {
        return R.ok(wmsWarehouseService.pageWarehouse(req));
    }

    @Operation(summary = "获取全部仓库（下拉选择）")
    @GetMapping("/list-all")
    public R<List<WmsWarehouse>> listAll() {
        return R.ok(wmsWarehouseService.listAll());
    }

    @Operation(summary = "获取仓库详情")
    @PreAuthorize(hasAuthority = "wms:warehouse:list")
    @GetMapping("/{id}")
    public R<WmsWarehouse> getById(@PathVariable Long id) {
        return R.ok(wmsWarehouseService.getById(id));
    }

    @PreAuthorize(hasAuthority = "wms:warehouse:add")
    @OperationLog(module = "仓库管理", type = "POST", businessType = 1)
    @Operation(summary = "新增仓库")
    @PostMapping
    public R<Void> post(@RequestBody WarehouseSaveReq req) {
        wmsWarehouseService.saveWarehouse(req);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:warehouse:edit")
    @OperationLog(module = "仓库管理", type = "PUT", businessType = 2)
    @Operation(summary = "修改仓库")
    @PutMapping
    public R<Void> put(@RequestBody WarehouseSaveReq req) {
        wmsWarehouseService.updateWarehouse(req);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:warehouse:remove")
    @OperationLog(module = "仓库管理", type = "DELETE", businessType = 3)
    @Operation(summary = "删除仓库")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        wmsWarehouseService.deleteWarehouse(id);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:warehouse:remove")
    @OperationLog(module = "仓库管理", type = "DELETE", businessType = 3)
    @Operation(summary = "批量删除仓库")
    @DeleteMapping("/batch-delete")
    public R<Void> batchDelete(@RequestBody WarehouseBatchDeleteReq req) {
        wmsWarehouseService.batchDeleteWarehouse(req.getIds());
        return R.ok();
    }

    @Operation(summary = "导入仓库")
    @PostMapping("/import")
    public R<Void> importData() {
        return R.ok();
    }

    @Operation(summary = "导出仓库")
    @GetMapping("/export")
    public R<Void> export() {
        return R.ok();
    }
}
