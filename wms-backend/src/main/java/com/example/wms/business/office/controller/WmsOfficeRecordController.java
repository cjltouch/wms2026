package com.example.wms.business.office.controller;

import com.example.wms.business.office.dto.req.OfficeRecordPageReq;
import com.example.wms.business.office.dto.req.OfficeRecordSaveReq;
import com.example.wms.business.office.service.WmsOfficeRecordService;
import com.example.wms.common.R;
import com.example.wms.common.annotation.OperationLog;
import com.example.wms.common.annotation.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

@Tag(name = "用品登记")
@RestController
@RequestMapping("/api/wms/office-record")
@RequiredArgsConstructor
public class WmsOfficeRecordController {

    private final WmsOfficeRecordService wmsOfficeRecordService;

    @Operation(summary = "用品登记分页列表")
    @PreAuthorize(hasAuthority = "wms:office:list")
    @GetMapping("/page")
    public R<?> page(OfficeRecordPageReq req) {
        return R.ok(wmsOfficeRecordService.pageOfficeRecord(req));
    }

    @Operation(summary = "用品名称联想（新增登记时模糊搜索历史名称）")
    @GetMapping("/suggest-name")
    public R<?> suggestName(@org.springframework.web.bind.annotation.RequestParam(defaultValue = "") String keyword) {
        return R.ok(wmsOfficeRecordService.suggestName(keyword));
    }

    @PreAuthorize(hasAuthority = "wms:office:export")
    @Operation(summary = "导出用品登记")
    @GetMapping("/export")
    public void export(OfficeRecordPageReq req, jakarta.servlet.http.HttpServletResponse response) {
        wmsOfficeRecordService.exportOfficeRecord(req, response);
    }

    @PreAuthorize(hasAuthority = "wms:office:add")
    @OperationLog(module = "用品登记", type = "POST", businessType = 1)
    @Operation(summary = "新增用品登记")
    @PostMapping
    public R<Void> post(@Valid @RequestBody OfficeRecordSaveReq req) {
        wmsOfficeRecordService.saveOfficeRecord(req);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:office:edit")
    @OperationLog(module = "用品登记", type = "PUT", businessType = 2)
    @Operation(summary = "修改用品登记")
    @PutMapping
    public R<Void> put(@Valid @RequestBody OfficeRecordSaveReq req) {
        wmsOfficeRecordService.updateOfficeRecord(req);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:office:remove")
    @OperationLog(module = "用品登记", type = "DELETE", businessType = 3)
    @Operation(summary = "删除用品登记")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        wmsOfficeRecordService.deleteOfficeRecord(id);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:office:remove")
    @OperationLog(module = "用品登记", type = "DELETE", businessType = 3)
    @Operation(summary = "批量删除用品登记")
    @DeleteMapping("/batch-delete")
    public R<Void> batchDelete(@RequestBody List<Long> ids) {
        wmsOfficeRecordService.batchDeleteOfficeRecord(ids);
        return R.ok();
    }
}
