package com.example.wms.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.wms.common.PageRsp;
import com.example.wms.system.dto.req.LoginLogPageReq;
import com.example.wms.system.entity.SysLoginLog;

public interface SysLoginLogService extends IService<SysLoginLog> {

    PageRsp<SysLoginLog> pageList(LoginLogPageReq req);

    void saveLoginLog(String userName, String ipaddr, boolean success, String msg);
}
