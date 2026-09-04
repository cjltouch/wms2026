package com.example.wms.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.wms.system.entity.SysUserRole;

import java.util.List;

public interface SysUserRoleService extends IService<SysUserRole> {

    void deleteByUserId(Long userId);

    void deleteByUserIds(List<Long> userIds);

    void deleteByRoleId(Long roleId);

    void deleteByRoleIds(List<Long> roleIds);

    void insertBatch(Long userId, List<Long> roleIds);
}
