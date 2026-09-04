package com.example.wms.system.controller;

import com.example.wms.common.R;
import com.example.wms.common.annotation.OperationLog;
import com.example.wms.common.annotation.PreAuthorize;
import com.example.wms.system.entity.SysDept;
import com.example.wms.system.service.SysDeptService;
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

@Tag(name = "部门管理")
@RestController
@RequestMapping("/api/system/dept")
@RequiredArgsConstructor
public class DeptController {

    private final SysDeptService sysDeptService;

    @Operation(summary = "部门树形列表")
    @GetMapping("/tree")
    public R<List<SysDept>> tree(SysDept dept) {
        return R.ok(sysDeptService.deptTree(dept));
    }

    @Operation(summary = "获取部门详情")
    @GetMapping("/{id}")
    public R<SysDept> getById(@PathVariable Long id) {
        return R.ok(sysDeptService.getById(id));
    }

    @PreAuthorize(hasAuthority = "system:dept:add")
    @OperationLog(module = "部门管理", type = "POST", businessType = 1)
    @Operation(summary = "新增部门")
    @PostMapping
    public R<Void> post(@RequestBody SysDept dept) {
        sysDeptService.save(dept);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "system:dept:edit")
    @OperationLog(module = "部门管理", type = "PUT", businessType = 2)
    @Operation(summary = "修改部门")
    @PutMapping
    public R<Void> put(@RequestBody SysDept dept) {
        sysDeptService.updateById(dept);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "system:dept:remove")
    @OperationLog(module = "部门管理", type = "DELETE", businessType = 3)
    @Operation(summary = "删除部门")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        sysDeptService.deleteDept(id);
        return R.ok();
    }
}
