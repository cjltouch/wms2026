package com.example.wms.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.wms.system.dto.req.MenuSaveReq;
import com.example.wms.system.dto.rsp.RouterVo;
import com.example.wms.system.entity.SysMenu;
import com.example.wms.system.entity.SysRole;
import com.example.wms.system.mapper.SysMenuMapper;
import com.example.wms.system.service.SysMenuService;
import com.example.wms.system.service.SysRoleMenuService;
import com.example.wms.system.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    @Lazy
    @Autowired
    private SysRoleService sysRoleService;

    private final SysRoleMenuService sysRoleMenuService;

    @Override
    public List<SysMenu> buildMenuTree(List<SysMenu> menus, Long parentId) {
        List<SysMenu> returnList = new ArrayList<>();
        for (Iterator<SysMenu> iterator = menus.iterator(); iterator.hasNext(); ) {
            SysMenu menu = iterator.next();
            if (menu.getParentId() != null && menu.getParentId().equals(parentId)) {
                recursionFn(menus, menu);
                returnList.add(menu);
            }
        }
        if (returnList.isEmpty()) {
            returnList = menus;
        }
        return returnList;
    }

    private void recursionFn(List<SysMenu> list, SysMenu menu) {
        List<SysMenu> childList = getChildList(list, menu);
        menu.setChildren(childList);
        for (SysMenu child : childList) {
            if (hasChild(list, child)) {
                recursionFn(list, child);
            }
        }
    }

    private List<SysMenu> getChildList(List<SysMenu> list, SysMenu menu) {
        return list.stream()
                .filter(m -> m.getParentId() != null && m.getParentId().equals(menu.getMenuId()))
                .collect(Collectors.toList());
    }

    private boolean hasChild(List<SysMenu> list, SysMenu menu) {
        return list.stream().anyMatch(m -> m.getParentId() != null && m.getParentId().equals(menu.getMenuId()));
    }

    @Override
    public List<RouterVo> buildRouterVo(List<SysMenu> menus) {
        List<RouterVo> routers = new LinkedList<>();
        for (SysMenu menu : menus) {
            RouterVo router = new RouterVo();
            router.setName(StringUtils.hasText(menu.getRouteName()) ? menu.getRouteName() : menu.getPath());
            router.setPath(getRouterPath(menu));
            router.setComponent(getComponent(menu));
            router.setMeta(new RouterVo.Meta());
            router.getMeta().setTitle(menu.getMenuName());
            router.getMeta().setIcon(menu.getIcon());
            router.getMeta().setNoCache(menu.getIsCache() != null && menu.getIsCache() == 1);
            router.getMeta().setLink(menu.getIsFrame() != null && menu.getIsFrame() == 0);
            List<SysMenu> children = menu.getChildren();
            if (!CollectionUtils.isEmpty(children) && "M".equals(menu.getMenuType())) {
                router.setAlwaysShow(true);
                router.setRedirect("noRedirect");
                router.setChildren(buildRouterVo(children));
            } else if (isMenuFrame(menu)) {
                router.setMeta(null);
                List<RouterVo> childrenList = new ArrayList<>();
                RouterVo childRouter = new RouterVo();
                childRouter.setPath(menu.getPath());
                childRouter.setComponent(menu.getComponent());
                childRouter.setName(StringUtils.capitalize(menu.getPath()));
                childRouter.setMeta(new RouterVo.Meta());
                childRouter.getMeta().setTitle(menu.getMenuName());
                childRouter.getMeta().setIcon(menu.getIcon());
                childRouter.getMeta().setNoCache(menu.getIsCache() != null && menu.getIsCache() == 1);
                childrenList.add(childRouter);
                router.setChildren(childrenList);
            } else if (menu.getParentId() == null || menu.getParentId() == 0L) {
                router.getMeta().setLink(menu.getIsFrame() != null && menu.getIsFrame() == 0);
            }
            routers.add(router);
        }
        return routers;
    }

    public String getRouterPath(SysMenu menu) {
        String routerPath = menu.getPath();
        if (menu.getParentId() == null || menu.getParentId() == 0L) {
            if ("M".equals(menu.getMenuType()) && menu.getIsFrame() != null && menu.getIsFrame() == 1) {
                routerPath = "/" + menu.getPath();
            }
        }
        return routerPath;
    }

    public String getComponent(SysMenu menu) {
        String component = "Layout";
        if (StringUtils.hasText(menu.getComponent()) && !isMenuFrame(menu)) {
            component = menu.getComponent();
        } else if (!StringUtils.hasText(menu.getComponent()) && menu.getParentId() != null && menu.getParentId() != 0L && "C".equals(menu.getMenuType())) {
            component = "ParentView";
        }
        return component;
    }

    public boolean isMenuFrame(SysMenu menu) {
        return menu.getParentId() != null && menu.getParentId() == 0L && "C".equals(menu.getMenuType()) && menu.getIsFrame() != null && menu.getIsFrame() == 1;
    }

    @Override
    public List<RouterVo> getUserMenuTree(Long userId) {
        List<SysMenu> menus;
        if (isAdmin(userId)) {
            menus = this.list(new LambdaQueryWrapper<SysMenu>()
                    .in(SysMenu::getMenuType, "M", "C")
                    .eq(SysMenu::getStatus, "0")
                    .orderByAsc(SysMenu::getOrderNum));
        } else {
            List<SysRole> roles = sysRoleService.getRolesByUserId(userId);
            Set<Long> menuIds = new HashSet<>();
            for (SysRole role : roles) {
                menuIds.addAll(sysRoleMenuService.getMenuIdsByRoleId(role.getRoleId()));
            }
            if (CollectionUtils.isEmpty(menuIds)) {
                return Collections.emptyList();
            }
            menus = this.list(new LambdaQueryWrapper<SysMenu>()
                    .in(SysMenu::getMenuId, menuIds)
                    .in(SysMenu::getMenuType, "M", "C")
                    .eq(SysMenu::getStatus, "0")
                    .orderByAsc(SysMenu::getOrderNum));
        }
        return buildRouterVo(buildMenuTree(menus, 0L));
    }

    private boolean isAdmin(Long userId) {
        return userId != null && userId == 1L;
    }

    @Override
    public Set<String> getMenuPermsByUserId(Long userId) {
        if (isAdmin(userId)) {
            return Collections.singleton("*:*:*");
        }
        List<SysRole> roles = sysRoleService.getRolesByUserId(userId);
        Set<Long> menuIds = new HashSet<>();
        for (SysRole role : roles) {
            menuIds.addAll(sysRoleMenuService.getMenuIdsByRoleId(role.getRoleId()));
        }
        if (CollectionUtils.isEmpty(menuIds)) {
            return Collections.emptySet();
        }
        List<SysMenu> menus = this.list(new LambdaQueryWrapper<SysMenu>()
                .in(SysMenu::getMenuId, menuIds)
                .isNotNull(SysMenu::getPerms));
        return menus.stream()
                .map(SysMenu::getPerms)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }

    @Override
    public List<SysMenu> listByUserId(Long userId) {
        if (isAdmin(userId)) {
            return this.list(new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getOrderNum));
        }
        List<SysRole> roles = sysRoleService.getRolesByUserId(userId);
        Set<Long> menuIds = new HashSet<>();
        for (SysRole role : roles) {
            menuIds.addAll(sysRoleMenuService.getMenuIdsByRoleId(role.getRoleId()));
        }
        if (CollectionUtils.isEmpty(menuIds)) {
            return Collections.emptyList();
        }
        return this.list(new LambdaQueryWrapper<SysMenu>()
                .in(SysMenu::getMenuId, menuIds)
                .orderByAsc(SysMenu::getOrderNum));
    }

    @Override
    public List<SysMenu> treeList(MenuSaveReq req) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(req.getMenuName())) {
            wrapper.like(SysMenu::getMenuName, req.getMenuName());
        }
        if (StringUtils.hasText(req.getStatus())) {
            wrapper.eq(SysMenu::getStatus, req.getStatus());
        }
        wrapper.orderByAsc(SysMenu::getOrderNum);
        List<SysMenu> menus = this.list(wrapper);
        return buildMenuTree(menus, 0L);
    }

    @Override
    public void deleteMenu(Long menuId) {
        sysRoleMenuService.deleteByMenuId(menuId);
        this.removeById(menuId);
    }
}
