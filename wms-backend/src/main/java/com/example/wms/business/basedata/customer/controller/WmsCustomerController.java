package com.example.wms.business.basedata.customer.controller;

import com.example.wms.business.basedata.customer.dto.req.CustomerBatchDeleteReq;
import com.example.wms.business.basedata.customer.dto.req.CustomerPageReq;
import com.example.wms.business.basedata.customer.dto.req.CustomerSaveReq;
import com.example.wms.business.basedata.customer.entity.WmsCustomer;
import com.example.wms.business.basedata.customer.service.WmsCustomerService;
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

@Tag(name = "客户管理")
@RestController
@RequestMapping("/api/wms/customer")
@RequiredArgsConstructor
public class WmsCustomerController {

    private final WmsCustomerService wmsCustomerService;

    @Operation(summary = "客户分页列表")
    @PreAuthorize(hasAuthority = "wms:customer:list")
    @GetMapping("/page")
    public R<?> page(CustomerPageReq req) {
        return R.ok(wmsCustomerService.pageCustomer(req));
    }

    @Operation(summary = "获取全部客户（下拉选择）")
    @GetMapping("/list-all")
    public R<List<WmsCustomer>> listAll() {
        return R.ok(wmsCustomerService.listAll());
    }

    @Operation(summary = "获取客户详情")
    @PreAuthorize(hasAuthority = "wms:customer:list")
    @GetMapping("/{id}")
    public R<WmsCustomer> getById(@PathVariable Long id) {
        return R.ok(wmsCustomerService.getById(id));
    }

    @PreAuthorize(hasAuthority = "wms:customer:add")
    @OperationLog(module = "客户管理", type = "POST", businessType = 1)
    @Operation(summary = "新增客户")
    @PostMapping
    public R<Void> post(@RequestBody CustomerSaveReq req) {
        wmsCustomerService.saveCustomer(req);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:customer:edit")
    @OperationLog(module = "客户管理", type = "PUT", businessType = 2)
    @Operation(summary = "修改客户")
    @PutMapping
    public R<Void> put(@RequestBody CustomerSaveReq req) {
        wmsCustomerService.updateCustomer(req);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:customer:remove")
    @OperationLog(module = "客户管理", type = "DELETE", businessType = 3)
    @Operation(summary = "删除客户")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        wmsCustomerService.deleteCustomer(id);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "wms:customer:remove")
    @OperationLog(module = "客户管理", type = "DELETE", businessType = 3)
    @Operation(summary = "批量删除客户")
    @DeleteMapping("/batch-delete")
    public R<Void> batchDelete(@RequestBody CustomerBatchDeleteReq req) {
        wmsCustomerService.batchDeleteCustomer(req.getIds());
        return R.ok();
    }

    @Operation(summary = "导入客户")
    @PostMapping("/import")
    public R<Void> importData() {
        return R.ok();
    }

    @Operation(summary = "导出客户")
    @GetMapping("/export")
    public R<Void> export() {
        return R.ok();
    }
}
