package com.example.wms.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.wms.common.PageRsp;
import com.example.wms.system.dto.req.RoleDataScopeReq;
import com.example.wms.system.dto.req.RoleSaveReq;
import com.example.wms.system.entity.SysRole;

import java.util.List;
import java.util.Set;

public interface SysRoleService extends IService<SysRole> {

    List<SysRole> getRolesByUserId(Long userId);

    Set<String> getRoleKeysByUserId(Long userId);

    List<Long> getRoleMenuIds(Long roleId);

    void assignMenu(Long roleId, List<Long> menuIds);

    void dataScope(RoleDataScopeReq req);

    PageRsp<SysRole> pageList(RoleSaveReq req);

    void insertRole(SysRole role, List<Long> menuIds);

    void updateRole(SysRole role, List<Long> menuIds);

    void deleteRole(List<Long> roleIds);
}
