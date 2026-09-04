package com.example.wms.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.wms.system.dto.req.MenuSaveReq;
import com.example.wms.system.dto.rsp.RouterVo;
import com.example.wms.system.entity.SysMenu;

import java.util.List;
import java.util.Set;

public interface SysMenuService extends IService<SysMenu> {

    List<SysMenu> buildMenuTree(List<SysMenu> menus, Long parentId);

    List<RouterVo> buildRouterVo(List<SysMenu> menus);

    List<RouterVo> getUserMenuTree(Long userId);

    Set<String> getMenuPermsByUserId(Long userId);

    List<SysMenu> listByUserId(Long userId);

    List<SysMenu> treeList(MenuSaveReq req);

    void deleteMenu(Long menuId);
}
