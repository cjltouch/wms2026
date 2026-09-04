package com.example.wms.business.basedata.warehouse.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.wms.business.basedata.warehouse.dto.req.WarehousePageReq;
import com.example.wms.business.basedata.warehouse.dto.req.WarehouseSaveReq;
import com.example.wms.business.basedata.warehouse.entity.WmsWarehouse;
import com.example.wms.common.PageRsp;

import java.util.List;

public interface WmsWarehouseService extends IService<WmsWarehouse> {

    PageRsp<WmsWarehouse> pageWarehouse(WarehousePageReq req);

    List<WmsWarehouse> listAll();

    void saveWarehouse(WarehouseSaveReq req);

    void updateWarehouse(WarehouseSaveReq req);

    void deleteWarehouse(Long warehouseId);

    void batchDeleteWarehouse(List<Long> warehouseIds);
}
