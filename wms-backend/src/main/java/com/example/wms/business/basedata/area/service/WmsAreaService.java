package com.example.wms.business.basedata.area.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.wms.business.basedata.area.dto.req.AreaPageReq;
import com.example.wms.business.basedata.area.dto.req.AreaSaveReq;
import com.example.wms.business.basedata.area.entity.WmsArea;
import com.example.wms.common.PageRsp;

import java.util.List;

public interface WmsAreaService extends IService<WmsArea> {

    PageRsp<WmsArea> pageArea(AreaPageReq req);

    List<WmsArea> listAll();

    List<WmsArea> listByWarehouseId(Long warehouseId);

    void saveArea(AreaSaveReq req);

    void updateArea(AreaSaveReq req);

    void deleteArea(Long areaId);

    void batchDeleteArea(List<Long> areaIds);
}
