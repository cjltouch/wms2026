package com.example.wms.business.basedata.location.controller;

import com.example.wms.business.basedata.location.dto.req.LocationBatchDeleteReq;
import com.example.wms.business.basedata.location.dto.req.LocationBatchGenerateReq;
import com.example.wms.business.basedata.location.dto.req.LocationPageReq;
import com.example.wms.business.basedata.location.dto.req.LocationSaveReq;
import com.example.wms.business.basedata.location.entity.WmsLocation;
import com.example.wms.business.basedata.location.service.WmsLocationService;
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

@Tag(name = "库位管理")
@RestController
@RequestMapping("/api/wms/location")
@RequiredArgsConstructor
public class WmsLocationController {

    private final WmsLocationService wmsLocationService;

    @Operation(summary = "库位分页列表")
    @PreAuthorize(hasAuthority = "wms:location:list")
    @GetMapping("/page")
    public R<?> page(LocationPageReq req) {
        return R.ok(wmsLocationService.pageLocation(req));
    }

    @Operation(summary = "获取全部库位（下拉选择）")
    @GetMapping("/list-all")
    public R<List<WmsLocation>> listAll() {
        return R.ok(wmsLocationService.listAll());
    }

    @Operation(summary = "根据库区ID获取库位列表")
    @GetMapping("/list-by-area")
    public R<List<WmsLocation>> listByAreaId(@RequestParam Long areaId) {
        return R.ok(wmsLocationService.listByAreaId(areaId));
    }

    @Operation(summary = "获取库位详情")
    @PreAuthorize(hasAuthority = "wms:location:list")
    @GetMapping("/{id}")
    public R<WmsLocation> getById(@PathVariable Long id) {
        return R.ok(wmsLocationService.getById(id));
    }

    @PreAuthorize(hasAuthority = "wms:location:add")
    @OperationLog(module = "库位管理", type = "POST", businessType = 1)
    @Operation(summary = "新增库位")
    @PostMapping
    public R<Void> post(@RequestBody LocationSaveReq req) {
        wmsLocationService.saveLocation(req);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:location:edit")
    @OperationLog(module = "库位管理", type = "PUT", businessType = 2)
    @Operation(summary = "修改库位")
    @PutMapping
    public R<Void> put(@RequestBody LocationSaveReq req) {
        wmsLocationService.updateLocation(req);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:location:remove")
    @OperationLog(module = "库位管理", type = "DELETE", businessType = 3)
    @Operation(summary = "删除库位")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        wmsLocationService.deleteLocation(id);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:location:remove")
    @OperationLog(module = "库位管理", type = "DELETE", businessType = 3)
    @Operation(summary = "批量删除库位")
    @DeleteMapping("/batch-delete")
    public R<Void> batchDelete(@RequestBody LocationBatchDeleteReq req) {
        wmsLocationService.batchDeleteLocation(req.getIds());
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:location:add")
    @OperationLog(module = "库位管理", type = "POST", businessType = 1)
    @Operation(summary = "按模板批量生成库位")
    @PostMapping("/batch-generate")
    public R<Integer> batchGenerate(@RequestBody LocationBatchGenerateReq req) {
        return R.ok(wmsLocationService.batchGenerate(req));
    }

    @Operation(summary = "导入库位")
    @PostMapping("/import")
    public R<Void> importData() {
        return R.ok();
    }

    @Operation(summary = "导出库位")
    @GetMapping("/export")
    public R<Void> export() {
        return R.ok();
    }
}
