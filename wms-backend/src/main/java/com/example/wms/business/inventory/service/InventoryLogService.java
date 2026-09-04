package com.example.wms.business.inventory.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.wms.business.inventory.dto.req.InventoryLogPageReq;
import com.example.wms.business.inventory.entity.WmsInventoryLog;
import com.example.wms.common.PageRsp;

public interface InventoryLogService extends IService<WmsInventoryLog> {

    PageRsp<WmsInventoryLog> pageLog(InventoryLogPageReq req);
}
