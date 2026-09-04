package com.example.wms.system.controller;

import com.example.wms.common.R;
import com.example.wms.common.annotation.OperationLog;
import com.example.wms.common.annotation.PreAuthorize;
import com.example.wms.system.dto.req.MenuSaveReq;
import com.example.wms.system.entity.SysMenu;
import com.example.wms.system.service.SysMenuService;
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

@Tag(name = "菜单管理")
@RestController
@RequestMapping("/api/system/menu")
@RequiredArgsConstructor
public class MenuController {

    private final SysMenuService sysMenuService;

    // 角色管理分配菜单权限时也需要读取菜单树
    @PreAuthorize(hasAnyAuthority = {"system:menu:list", "system:role:list", "system:role:edit"})
    @Operation(summary = "菜单树形列表")
    @GetMapping("/tree")
    public R<List<SysMenu>> tree(MenuSaveReq req) {
        return R.ok(sysMenuService.treeList(req));
    }

    @PreAuthorize(hasAuthority = "system:menu:list")
    @Operation(summary = "获取菜单详情")
    @GetMapping("/{id}")
    public R<SysMenu> getById(@PathVariable Long id) {
        return R.ok(sysMenuService.getById(id));
    }

    @PreAuthorize(hasAuthority = "system:menu:add")
    @OperationLog(module = "菜单管理", type = "POST", businessType = 1)
    @Operation(summary = "新增菜单")
    @PostMapping
    public R<Void> post(@Valid @RequestBody MenuSaveReq req) {
        SysMenu menu = new SysMenu();
        BeanUtils.copyProperties(req, menu);
        sysMenuService.save(menu);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "system:menu:edit")
    @OperationLog(module = "菜单管理", type = "PUT", businessType = 2)
    @Operation(summary = "修改菜单")
    @PutMapping
    public R<Void> put(@Valid @RequestBody MenuSaveReq req) {
        SysMenu menu = new SysMenu();
        BeanUtils.copyProperties(req, menu);
        sysMenuService.updateById(menu);
        return R.ok();
    }

    @PreAuthorize(hasAuthority = "system:menu:remove")
    @OperationLog(module = "菜单管理", type = "DELETE", businessType = 3)
    @Operation(summary = "删除菜单")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        sysMenuService.deleteMenu(id);
        return R.ok();
    }
}
