package com.example.wms.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.wms.system.entity.SysRoleMenu;

import java.util.List;

public interface SysRoleMenuService extends IService<SysRoleMenu> {

    void deleteByRoleId(Long roleId);

    void deleteByRoleIds(List<Long> roleIds);

    void deleteByMenuId(Long menuId);

    List<Long> getMenuIdsByRoleId(Long roleId);

    void insertBatch(Long roleId, List<Long> menuIds);
}
