package com.example.wms.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.wms.auth.LoginUser;
import com.example.wms.common.PageRsp;
import com.example.wms.system.dto.req.UserPageReq;
import com.example.wms.system.entity.SysUser;

import java.util.List;

public interface SysUserService extends IService<SysUser> {

    LoginUser getUserWithPerms(Long userId);

    LoginUser getLoginUserById(Long userId);

    SysUser getByUserName(String userName);

    void resetPwd(Long userId, String password);

    void changeStatus(Long userId, String status);

    PageRsp<SysUser> pageList(UserPageReq req);

    void insertUser(SysUser user, List<Long> roleIds);

    void updateUser(SysUser user, List<Long> roleIds);

    void deleteUser(List<Long> userIds);
}
