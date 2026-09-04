package com.example.wms.business.basedata.unit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.wms.business.basedata.unit.dto.req.UnitPageReq;
import com.example.wms.business.basedata.unit.dto.req.UnitSaveReq;
import com.example.wms.business.basedata.unit.entity.WmsUnit;
import com.example.wms.common.PageRsp;

import java.util.List;

public interface WmsUnitService extends IService<WmsUnit> {

    PageRsp<WmsUnit> pageUnit(UnitPageReq req);

    List<WmsUnit> listAll();

    void saveUnit(UnitSaveReq req);

    void updateUnit(UnitSaveReq req);

    void deleteUnit(Long unitId);

    void batchDeleteUnit(List<Long> unitIds);
}
