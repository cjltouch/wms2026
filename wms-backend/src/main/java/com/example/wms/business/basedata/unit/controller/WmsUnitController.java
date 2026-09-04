package com.example.wms.business.basedata.unit.controller;

import com.example.wms.business.basedata.unit.dto.req.UnitBatchDeleteReq;
import com.example.wms.business.basedata.unit.dto.req.UnitPageReq;
import com.example.wms.business.basedata.unit.dto.req.UnitSaveReq;
import com.example.wms.business.basedata.unit.entity.WmsUnit;
import com.example.wms.business.basedata.unit.service.WmsUnitService;
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

@Tag(name = "单位管理")
@RestController
@RequestMapping("/api/wms/unit")
@RequiredArgsConstructor
public class WmsUnitController {

    private final WmsUnitService wmsUnitService;

    @Operation(summary = "单位分页列表")
    @PreAuthorize(hasAuthority = "wms:unit:list")
    @GetMapping("/page")
    public R<?> page(UnitPageReq req) {
        return R.ok(wmsUnitService.pageUnit(req));
    }

    @Operation(summary = "获取全部单位（下拉选择）")
    @GetMapping("/list-all")
    public R<List<WmsUnit>> listAll() {
        return R.ok(wmsUnitService.listAll());
    }

    @Operation(summary = "获取单位详情")
    @PreAuthorize(hasAuthority = "wms:unit:list")
    @GetMapping("/{id}")
    public R<WmsUnit> getById(@PathVariable Long id) {
        return R.ok(wmsUnitService.getById(id));
    }

    @PreAuthorize(hasAuthority = "wms:unit:add")
    @OperationLog(module = "单位管理", type = "POST", businessType = 1)
    @Operation(summary = "新增单位")
    @PostMapping
    public R<Void> post(@RequestBody UnitSaveReq req) {
        wmsUnitService.saveUnit(req);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:unit:edit")
    @OperationLog(module = "单位管理", type = "PUT", businessType = 2)
    @Operation(summary = "修改单位")
    @PutMapping
    public R<Void> put(@RequestBody UnitSaveReq req) {
        wmsUnitService.updateUnit(req);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:unit:remove")
    @OperationLog(module = "单位管理", type = "DELETE", businessType = 3)
    @Operation(summary = "删除单位")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        wmsUnitService.deleteUnit(id);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:unit:remove")
    @OperationLog(module = "单位管理", type = "DELETE", businessType = 3)
    @Operation(summary = "批量删除单位")
    @DeleteMapping("/batch-delete")
    public R<Void> batchDelete(@RequestBody UnitBatchDeleteReq req) {
        wmsUnitService.batchDeleteUnit(req.getIds());
        return R.ok();
    }

    @Operation(summary = "导入单位")
    @PostMapping("/import")
    public R<Void> importData() {
        return R.ok();
    }

    @Operation(summary = "导出单位")
    @GetMapping("/export")
    public R<Void> export() {
        return R.ok();
    }
}
