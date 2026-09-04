package com.example.wms.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.wms.common.PageRsp;
import com.example.wms.system.dto.req.RoleDataScopeReq;
import com.example.wms.system.dto.req.RoleSaveReq;
import com.example.wms.system.entity.SysRole;
import com.example.wms.system.entity.SysUserRole;
import com.example.wms.system.mapper.SysRoleMapper;
import com.example.wms.system.service.SysRoleMenuService;
import com.example.wms.system.service.SysRoleService;
import com.example.wms.system.service.SysUserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    private final SysUserRoleService sysUserRoleService;
    private final SysRoleMenuService sysRoleMenuService;

    @Override
    public List<SysRole> getRolesByUserId(Long userId) {
        List<Long> roleIds = sysUserRoleService.list(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId))
                .stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        return this.listByIds(roleIds).stream()
                .filter(r -> "0".equals(r.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public Set<String> getRoleKeysByUserId(Long userId) {
        return getRolesByUserId(userId).stream()
                .map(SysRole::getRoleKey)
                .collect(Collectors.toSet());
    }

    @Override
    public List<Long> getRoleMenuIds(Long roleId) {
        return sysRoleMenuService.getMenuIdsByRoleId(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignMenu(Long roleId, List<Long> menuIds) {
        sysRoleMenuService.deleteByRoleId(roleId);
        if (!CollectionUtils.isEmpty(menuIds)) {
            sysRoleMenuService.insertBatch(roleId, menuIds);
        }
    }

    @Override
    public void dataScope(RoleDataScopeReq req) {
        SysRole role = new SysRole();
        role.setRoleId(req.getRoleId());
        role.setDataScope(req.getDataScope());
        this.updateById(role);
    }

    @Override
    public PageRsp<SysRole> pageList(RoleSaveReq req) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(req.getRoleName())) {
            wrapper.like(SysRole::getRoleName, req.getRoleName());
        }
        if (StringUtils.hasText(req.getRoleKey())) {
            wrapper.like(SysRole::getRoleKey, req.getRoleKey());
        }
        if (StringUtils.hasText(req.getStatus())) {
            wrapper.eq(SysRole::getStatus, req.getStatus());
        }
        wrapper.orderByAsc(SysRole::getRoleSort);
        IPage<SysRole> page = this.page(new Page<>(req.getPageNum() != null ? req.getPageNum() : 1,
                req.getPageSize() != null ? req.getPageSize() : 10), wrapper);
        return new PageRsp<>(page.getTotal(), page.getRecords(),
                req.getPageNum() != null ? req.getPageNum() : 1,
                req.getPageSize() != null ? req.getPageSize() : 10);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertRole(SysRole role, List<Long> menuIds) {
        this.save(role);
        if (!CollectionUtils.isEmpty(menuIds)) {
            sysRoleMenuService.insertBatch(role.getRoleId(), menuIds);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(SysRole role, List<Long> menuIds) {
        this.updateById(role);
        // menuIds为null表示本次不涉及菜单授权调整，避免编辑角色基本信息时误清空授权
        if (menuIds != null) {
            sysRoleMenuService.deleteByRoleId(role.getRoleId());
            if (!CollectionUtils.isEmpty(menuIds)) {
                sysRoleMenuService.insertBatch(role.getRoleId(), menuIds);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(List<Long> roleIds) {
        if (roleIds != null && roleIds.contains(1L)) {
            throw new com.example.wms.common.exception.BizException("不允许删除内置超级管理员角色");
        }
        this.removeByIds(roleIds);
        sysUserRoleService.deleteByRoleIds(roleIds);
        sysRoleMenuService.deleteByRoleIds(roleIds);
    }
}
