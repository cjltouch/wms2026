package com.example.wms.business.basedata.area.controller;

import com.example.wms.business.basedata.area.dto.req.AreaBatchDeleteReq;
import com.example.wms.business.basedata.area.dto.req.AreaPageReq;
import com.example.wms.business.basedata.area.dto.req.AreaSaveReq;
import com.example.wms.business.basedata.area.entity.WmsArea;
import com.example.wms.business.basedata.area.service.WmsAreaService;
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

import java.util.List;

@Tag(name = "库区管理")
@RestController
@RequestMapping("/api/wms/area")
@RequiredArgsConstructor
public class WmsAreaController {

    private final WmsAreaService wmsAreaService;

    @Operation(summary = "库区分页列表")
    @PreAuthorize(hasAuthority = "wms:area:list")
    @GetMapping("/page")
    public R<?> page(AreaPageReq req) {
        return R.ok(wmsAreaService.pageArea(req));
    }

    @Operation(summary = "获取全部库区（下拉选择）")
    @GetMapping("/list-all")
    public R<List<WmsArea>> listAll() {
        return R.ok(wmsAreaService.listAll());
    }

    @Operation(summary = "根据仓库ID获取库区列表")
    @GetMapping("/list-by-warehouse")
    public R<List<WmsArea>> listByWarehouseId(@RequestParam Long warehouseId) {
        return R.ok(wmsAreaService.listByWarehouseId(warehouseId));
    }

    @Operation(summary = "获取库区详情")
    @PreAuthorize(hasAuthority = "wms:area:list")
    @GetMapping("/{id}")
    public R<WmsArea> getById(@PathVariable Long id) {
        return R.ok(wmsAreaService.getById(id));
    }

    @PreAuthorize(hasAuthority = "wms:area:add")
    @OperationLog(module = "库区管理", type = "POST", businessType = 1)
    @Operation(summary = "新增库区")
    @PostMapping
    public R<Void> post(@RequestBody AreaSaveReq req) {
        wmsAreaService.saveArea(req);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:area:edit")
    @OperationLog(module = "库区管理", type = "PUT", businessType = 2)
    @Operation(summary = "修改库区")
    @PutMapping
    public R<Void> put(@RequestBody AreaSaveReq req) {
        wmsAreaService.updateArea(req);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:area:remove")
    @OperationLog(module = "库区管理", type = "DELETE", businessType = 3)
    @Operation(summary = "删除库区")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        wmsAreaService.deleteArea(id);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:area:remove")
    @OperationLog(module = "库区管理", type = "DELETE", businessType = 3)
    @Operation(summary = "批量删除库区")
    @DeleteMapping("/batch-delete")
    public R<Void> batchDelete(@RequestBody AreaBatchDeleteReq req) {
        wmsAreaService.batchDeleteArea(req.getIds());
        return R.ok();
    }

    @Operation(summary = "导入库区")
    @PostMapping("/import")
    public R<Void> importData() {
        return R.ok();
    }

    @Operation(summary = "导出库区")
    @GetMapping("/export")
    public R<Void> export() {
        return R.ok();
    }
}
