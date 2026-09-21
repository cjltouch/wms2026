package com.example.wms.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.wms.auth.LoginUser;
import com.example.wms.common.PageRsp;
import com.example.wms.system.dto.req.UserPageReq;
import com.example.wms.system.entity.SysUser;
import com.example.wms.system.mapper.SysUserMapper;
import com.example.wms.system.service.SysMenuService;
import com.example.wms.system.service.SysRoleService;
import com.example.wms.system.service.SysUserRoleService;
import com.example.wms.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Lazy
    @Autowired
    private SysRoleService sysRoleService;

    @Lazy
    @Autowired
    private SysMenuService sysMenuService;

    private final SysUserRoleService sysUserRoleService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginUser getUserWithPerms(Long userId) {
        SysUser user = this.getById(userId);
        if (user == null) {
            return null;
        }
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getUserId());
        loginUser.setUsername(user.getUserName());
        loginUser.setNickname(user.getNickName());
        loginUser.setRealName(user.getRealName());
        List<String> roleKeys = sysRoleService.getRolesByUserId(userId).stream()
                .map(r -> {
                    if (loginUser.getDataScope() == null || (r.getDataScope() != null && r.getDataScope() < loginUser.getDataScope())) {
                        loginUser.setDataScope(r.getDataScope());
                    }
                    return r.getRoleKey();
                })
                .collect(Collectors.toList());
        loginUser.setRoleCodes(roleKeys);
        loginUser.setPerms(sysMenuService.getMenuPermsByUserId(userId));
        loginUser.setWarehouseIds(Collections.emptyList());
        return loginUser;
    }

    @Override
    public LoginUser getLoginUserById(Long userId) {
        return getUserWithPerms(userId);
    }

    @Override
    public SysUser getByUserName(String userName) {
        return this.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserName, userName));
    }

    @Override
    public void resetPwd(Long userId, String password) {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setPassword(passwordEncoder.encode(password));
        user.setPwdUpdateTime(LocalDateTime.now());
        this.updateById(user);
    }

    @Override
    public void changeStatus(Long userId, String status) {
        if (userId != null && userId == 1L && "1".equals(status)) {
            throw new com.example.wms.common.exception.BizException("不允许停用内置管理员账号");
        }
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setStatus(status);
        this.updateById(user);
    }

    @Override
    public PageRsp<SysUser> pageList(UserPageReq req) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(req.getUserName())) {
            wrapper.like(SysUser::getUserName, req.getUserName());
        }
        if (StringUtils.hasText(req.getPhonenumber())) {
            wrapper.like(SysUser::getPhonenumber, req.getPhonenumber());
        }
        if (StringUtils.hasText(req.getStatus())) {
            wrapper.eq(SysUser::getStatus, req.getStatus());
        }
        if (req.getDeptId() != null) {
            wrapper.eq(SysUser::getDeptId, req.getDeptId());
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        IPage<SysUser> page = this.page(new Page<>(req.getPageNum(), req.getPageSize()), wrapper);
        return new PageRsp<>(page.getTotal(), page.getRecords(), req.getPageNum(), req.getPageSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertUser(SysUser user, List<Long> roleIds) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        this.save(user);
        if (!CollectionUtils.isEmpty(roleIds)) {
            sysUserRoleService.insertBatch(user.getUserId(), roleIds);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(SysUser user, List<Long> roleIds) {
        this.updateById(user);
        sysUserRoleService.deleteByUserId(user.getUserId());
        if (!CollectionUtils.isEmpty(roleIds)) {
            sysUserRoleService.insertBatch(user.getUserId(), roleIds);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(List<Long> userIds) {
        if (userIds != null && userIds.contains(1L)) {
            throw new com.example.wms.common.exception.BizException("不允许删除内置管理员账号");
        }
        this.removeByIds(userIds);
        sysUserRoleService.deleteByUserIds(userIds);
    }

    @Override
    public void updateAvatar(Long userId, String avatar) {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setAvatar(avatar);
        this.updateById(user);
    }
}
