package com.example.wms.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.wms.system.entity.SysUserRole;
import com.example.wms.system.mapper.SysUserRoleMapper;
import com.example.wms.system.service.SysUserRoleService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class SysUserRoleServiceImpl extends ServiceImpl<SysUserRoleMapper, SysUserRole> implements SysUserRoleService {

    @Override
    public void deleteByUserId(Long userId) {
        this.remove(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId));
    }

    @Override
    public void deleteByUserIds(List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return;
        }
        this.remove(new LambdaQueryWrapper<SysUserRole>()
                .in(SysUserRole::getUserId, userIds));
    }

    @Override
    public void deleteByRoleId(Long roleId) {
        this.remove(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getRoleId, roleId));
    }

    @Override
    public void deleteByRoleIds(List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return;
        }
        this.remove(new LambdaQueryWrapper<SysUserRole>()
                .in(SysUserRole::getRoleId, roleIds));
    }

    @Override
    public void insertBatch(Long userId, List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return;
        }
        List<SysUserRole> list = new ArrayList<>();
        for (Long roleId : roleIds) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            list.add(ur);
        }
        this.saveBatch(list);
    }
}
