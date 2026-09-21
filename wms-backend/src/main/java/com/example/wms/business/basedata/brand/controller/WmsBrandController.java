package com.example.wms.business.basedata.brand.controller;

import com.example.wms.business.basedata.brand.dto.req.BrandBatchDeleteReq;
import com.example.wms.business.basedata.brand.dto.req.BrandPageReq;
import com.example.wms.business.basedata.brand.dto.req.BrandSaveReq;
import com.example.wms.business.basedata.brand.entity.WmsBrand;
import com.example.wms.business.basedata.brand.service.WmsBrandService;
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

@Tag(name = "品牌管理")
@RestController
@RequestMapping("/api/wms/brand")
@RequiredArgsConstructor
public class WmsBrandController {

    private final WmsBrandService wmsBrandService;

    @Operation(summary = "品牌分页列表")
    @PreAuthorize(hasAuthority = "wms:brand:list")
    @GetMapping("/page")
    public R<?> page(BrandPageReq req) {
        return R.ok(wmsBrandService.pageBrand(req));
    }

    @Operation(summary = "获取全部品牌（下拉选择）")
    @GetMapping("/list-all")
    public R<List<WmsBrand>> listAll() {
        return R.ok(wmsBrandService.listAll());
    }

    @Operation(summary = "获取品牌详情")
    @PreAuthorize(hasAuthority = "wms:brand:list")
    @GetMapping("/{id}")
    public R<WmsBrand> getById(@PathVariable Long id) {
        return R.ok(wmsBrandService.getById(id));
    }

    @PreAuthorize(hasAuthority = "wms:brand:add")
    @OperationLog(module = "品牌管理", type = "POST", businessType = 1)
    @Operation(summary = "新增品牌")
    @PostMapping
    public R<Void> post(@RequestBody BrandSaveReq req) {
        wmsBrandService.saveBrand(req);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:brand:edit")
    @OperationLog(module = "品牌管理", type = "PUT", businessType = 2)
    @Operation(summary = "修改品牌")
    @PutMapping
    public R<Void> put(@RequestBody BrandSaveReq req) {
        wmsBrandService.updateBrand(req);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:brand:remove")
    @OperationLog(module = "品牌管理", type = "DELETE", businessType = 3)
    @Operation(summary = "删除品牌")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        wmsBrandService.deleteBrand(id);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:brand:remove")
    @OperationLog(module = "品牌管理", type = "DELETE", businessType = 3)
    @Operation(summary = "批量删除品牌")
    @DeleteMapping("/batch-delete")
    public R<Void> batchDelete(@RequestBody BrandBatchDeleteReq req) {
        wmsBrandService.batchDeleteBrand(req.getIds());
        return R.ok();
    }

    @Operation(summary = "导入品牌")
    @PostMapping("/import")
    public R<Void> importData() {
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:brand:export")
    @Operation(summary = "导出品牌")
    @GetMapping("/export")
    public R<Void> export() {
        return R.ok();
    }
}
