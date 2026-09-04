package com.example.wms.business.inventory.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.wms.business.inventory.dto.req.InventoryPageReq;
import com.example.wms.business.inventory.entity.WmsInventory;
import com.example.wms.common.PageRsp;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.Map;

public interface InventoryService extends IService<WmsInventory> {

    PageRsp<WmsInventory> pageInventory(InventoryPageReq req);

    /**
     * 按查询条件导出库存Excel
     */
    void exportInventory(InventoryPageReq req, HttpServletResponse response);

    List<WmsInventory> listBySkuId(Long skuId);

    PageRsp<WmsInventory> pageByBatch(InventoryPageReq req);

    PageRsp<WmsInventory> warningExpire(InventoryPageReq req, Integer thresholdDays);

    PageRsp<WmsInventory> warningBelowMin(InventoryPageReq req);

    PageRsp<Map<String, Object>> turnoverAnalytics(InventoryPageReq req);
}
