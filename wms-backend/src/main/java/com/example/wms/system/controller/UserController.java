package com.example.wms.system.controller;

import com.example.wms.common.R;
import com.example.wms.common.annotation.OperationLog;
import com.example.wms.common.annotation.PreAuthorize;
import com.example.wms.system.dto.req.UserPageReq;
import com.example.wms.system.dto.req.UserSaveReq;
import com.example.wms.system.entity.SysUser;
import com.example.wms.system.entity.SysUserRole;
import com.example.wms.system.service.SysUserRoleService;
import com.example.wms.system.service.SysUserService;
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
import java.util.stream.Collectors;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/system/user")
@RequiredArgsConstructor
public class UserController {

    private final SysUserService sysUserService;
    private final SysUserRoleService sysUserRoleService;

    @PreAuthorize(hasAuthority = "system:user:list")
    @Operation(summary = "用户分页列表")
    @GetMapping("/page")
    public R<?> page(UserPageReq req) {
        return R.ok(sysUserService.pageList(req));
    }

    @PreAuthorize(hasAuthority = "system:user:list")
    @Operation(summary = "获取用户详情")
    @GetMapping("/{id}")
    public R<SysUser> getById(@PathVariable Long id) {
        SysUser user = sysUserService.getById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return R.ok(user);
    }

    @PreAuthorize(hasAuthority = "system:user:list")
    @Operation(summary = "获取用户角色ID列表")
    @GetMapping("/{id}/roleIds")
    public R<List<Long>> getRoleIds(@PathVariable Long id) {
        return R.ok(sysUserRoleService.lambdaQuery()
                .eq(SysUserRole::getUserId, id)
                .list()
                .stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList()));
    }

    @PreAuthorize(hasAuthority = "system:user:add")
    @OperationLog(module = "用户管理", type = "POST", businessType = 1)
    @Operation(summary = "新增用户")
    @PostMapping
    public R<Void> post(@RequestBody UserSaveReq req) {
        sysUserService.insertUser(req.getUser(), req.getRoleIds());
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "system:user:edit")
    @OperationLog(module = "用户管理", type = "PUT", businessType = 2)
    @Operation(summary = "修改用户")
    @PutMapping
    public R<Void> put(@RequestBody UserSaveReq req) {
        sysUserService.updateUser(req.getUser(), req.getRoleIds());
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "system:user:remove")
    @OperationLog(module = "用户管理", type = "DELETE", businessType = 3)
    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        sysUserService.deleteUser(List.of(id));
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "system:user:remove")
    @OperationLog(module = "用户管理", type = "DELETE", businessType = 3)
    @Operation(summary = "批量删除用户")
    @DeleteMapping("/batch-delete")
    public R<Void> batchDelete(@RequestBody List<Long> ids) {
        sysUserService.deleteUser(ids);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "system:user:resetPwd")
    @OperationLog(module = "用户管理", type = "PUT", businessType = 2)
    @Operation(summary = "重置密码")
    @PutMapping("/reset-pwd")
    public R<Void> resetPwd(@RequestBody UserSaveReq req) {
        sysUserService.resetPwd(req.getUserId(), req.getPassword());
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "system:user:edit")
    @OperationLog(module = "用户管理", type = "PUT", businessType = 2)
    @Operation(summary = "修改用户状态")
    @PutMapping("/change-status")
    public R<Void> changeStatus(@RequestBody UserSaveReq req) {
        sysUserService.changeStatus(req.getUserId(), req.getStatus());
        return R.ok();
    }

    @Operation(summary = "导入用户")
    @PostMapping("/import")
    public R<Void> importData() {
        return R.ok();
    }

    @Operation(summary = "导出用户")
    @GetMapping("/export")
    public R<Void> export() {
        return R.ok();
    }

    @Operation(summary = "下载导入模板")
    @GetMapping("/template")
    public R<Void> template() {
        return R.ok();
    }
}
