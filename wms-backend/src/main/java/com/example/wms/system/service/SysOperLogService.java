package com.example.wms.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.wms.common.PageRsp;
import com.example.wms.system.dto.req.OperLogPageReq;
import com.example.wms.system.entity.SysOperLog;

public interface SysOperLogService extends IService<SysOperLog> {

    void saveLog(String module, String type, String detail, int businessType,
                 Long userId, String username, Long costTime, boolean success, String errorMsg);

    PageRsp<SysOperLog> pageList(OperLogPageReq req);

    void clean();
}
