package com.example.wms.business.basedata.category.controller;

import com.example.wms.business.basedata.category.dto.req.CategoryBatchDeleteReq;
import com.example.wms.business.basedata.category.dto.req.CategoryPageReq;
import com.example.wms.business.basedata.category.dto.req.CategorySaveReq;
import com.example.wms.business.basedata.category.entity.WmsCategory;
import com.example.wms.business.basedata.category.service.WmsCategoryService;
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

@Tag(name = "分类管理")
@RestController
@RequestMapping("/api/wms/category")
@RequiredArgsConstructor
public class WmsCategoryController {

    private final WmsCategoryService wmsCategoryService;

    @Operation(summary = "分类分页列表")
    @PreAuthorize(hasAuthority = "wms:category:list")
    @GetMapping("/page")
    public R<?> page(CategoryPageReq req) {
        return R.ok(wmsCategoryService.pageCategory(req));
    }

    @Operation(summary = "分类树形列表")
    @GetMapping("/tree")
    public R<List<WmsCategory>> tree(WmsCategory category) {
        return R.ok(wmsCategoryService.categoryTree(category));
    }

    @Operation(summary = "获取全部分类（下拉选择）")
    @GetMapping("/list-all")
    public R<List<WmsCategory>> listAll() {
        return R.ok(wmsCategoryService.listAll());
    }

    @Operation(summary = "获取分类详情")
    @PreAuthorize(hasAuthority = "wms:category:list")
    @GetMapping("/{id}")
    public R<WmsCategory> getById(@PathVariable Long id) {
        return R.ok(wmsCategoryService.getById(id));
    }

    @PreAuthorize(hasAuthority = "wms:category:add")
    @OperationLog(module = "分类管理", type = "POST", businessType = 1)
    @Operation(summary = "新增分类")
    @PostMapping
    public R<Void> post(@RequestBody CategorySaveReq req) {
        wmsCategoryService.saveCategory(req);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:category:edit")
    @OperationLog(module = "分类管理", type = "PUT", businessType = 2)
    @Operation(summary = "修改分类")
    @PutMapping
    public R<Void> put(@RequestBody CategorySaveReq req) {
        wmsCategoryService.updateCategory(req);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:category:remove")
    @OperationLog(module = "分类管理", type = "DELETE", businessType = 3)
    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        wmsCategoryService.deleteCategory(id);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:category:remove")
    @OperationLog(module = "分类管理", type = "DELETE", businessType = 3)
    @Operation(summary = "批量删除分类")
    @DeleteMapping("/batch-delete")
    public R<Void> batchDelete(@RequestBody CategoryBatchDeleteReq req) {
        wmsCategoryService.batchDeleteCategory(req.getIds());
        return R.ok();
    }

    @Operation(summary = "导入分类")
    @PostMapping("/import")
    public R<Void> importData() {
        return R.ok();
    }

    @Operation(summary = "导出分类")
    @GetMapping("/export")
    public R<Void> export() {
        return R.ok();
    }
}
