package com.example.wms.system.controller;

import com.example.wms.common.R;
import com.example.wms.common.annotation.OperationLog;
import com.example.wms.common.annotation.PreAuthorize;
import com.example.wms.system.dto.req.AssignMenuReq;
import com.example.wms.system.dto.req.RoleDataScopeReq;
import com.example.wms.system.dto.req.RoleSaveReq;
import com.example.wms.system.entity.SysRole;
import com.example.wms.system.service.SysRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "角色管理")
@RestController
@RequestMapping("/api/system/role")
@RequiredArgsConstructor
public class RoleController {

    private final SysRoleService sysRoleService;

    @PreAuthorize(hasAuthority = "system:role:list")
    @Operation(summary = "角色分页列表")
    @GetMapping("/page")
    public R<?> page(RoleSaveReq req) {
        return R.ok(sysRoleService.pageList(req));
    }

    // 用户管理页面分配角色时也需要读取角色下拉列表
    @PreAuthorize(hasAnyAuthority = {"system:role:list", "system:user:list"})
    @Operation(summary = "角色列表全部")
    @GetMapping("/list")
    public R<List<SysRole>> list() {
        return R.ok(sysRoleService.list());
    }

    @PreAuthorize(hasAuthority = "system:role:list")
    @Operation(summary = "获取角色详情")
    @GetMapping("/{id}")
    public R<SysRole> getById(@PathVariable Long id) {
        return R.ok(sysRoleService.getById(id));
    }

    @PreAuthorize(hasAnyAuthority = {"system:role:list", "system:role:edit"})
    @Operation(summary = "获取角色菜单ID")
    @GetMapping("/{id}/menuIds")
    public R<List<Long>> menuIds(@PathVariable Long id) {
        return R.ok(sysRoleService.getRoleMenuIds(id));
    }

    @PreAuthorize(hasAuthority = "system:role:add")
    @OperationLog(module = "角色管理", type = "POST", businessType = 1)
    @Operation(summary = "新增角色")
    @PostMapping
    public R<Void> post(@Valid @RequestBody RoleSaveReq req) {
        SysRole role = new SysRole();
        BeanUtils.copyProperties(req, role);
        sysRoleService.insertRole(role, req.getMenuIds());
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "system:role:edit")
    @OperationLog(module = "角色管理", type = "PUT", businessType = 2)
    @Operation(summary = "修改角色")
    @PutMapping
    public R<Void> put(@Valid @RequestBody RoleSaveReq req) {
        SysRole role = new SysRole();
        BeanUtils.copyProperties(req, role);
        sysRoleService.updateRole(role, req.getMenuIds());
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "system:role:remove")
    @OperationLog(module = "角色管理", type = "DELETE", businessType = 3)
    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        sysRoleService.deleteRole(List.of(id));
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "system:role:remove")
    @OperationLog(module = "角色管理", type = "DELETE", businessType = 3)
    @Operation(summary = "批量删除角色")
    @DeleteMapping("/batch-delete")
    public R<Void> batchDelete(@RequestBody List<Long> ids) {
        sysRoleService.deleteRole(ids);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "system:role:edit")
    @OperationLog(module = "角色管理", type = "PUT", businessType = 2)
    @Operation(summary = "分配菜单权限")
    @PutMapping("/assign-menu")
    public R<Void> assignMenu(@Valid @RequestBody AssignMenuReq req) {
        sysRoleService.assignMenu(req.getRoleId(), req.getMenuIds());
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "system:role:edit")
    @OperationLog(module = "角色管理", type = "PUT", businessType = 2)
    @Operation(summary = "数据权限")
    @PutMapping("/data-scope")
    public R<Void> dataScope(@Valid @RequestBody RoleDataScopeReq req) {
        sysRoleService.dataScope(req);
        return R.ok();
    }
}
