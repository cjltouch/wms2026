package com.example.wms.business.basedata.location.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.wms.business.basedata.location.dto.req.LocationBatchGenerateReq;
import com.example.wms.business.basedata.location.dto.req.LocationPageReq;
import com.example.wms.business.basedata.location.dto.req.LocationSaveReq;
import com.example.wms.business.basedata.location.entity.WmsLocation;
import com.example.wms.common.PageRsp;

import java.util.List;

public interface WmsLocationService extends IService<WmsLocation> {

    PageRsp<WmsLocation> pageLocation(LocationPageReq req);

    List<WmsLocation> listAll();

    List<WmsLocation> listByAreaId(Long areaId);

    void saveLocation(LocationSaveReq req);

    void updateLocation(LocationSaveReq req);

    void deleteLocation(Long locationId);

    void batchDeleteLocation(List<Long> locationIds);

    int batchGenerate(LocationBatchGenerateReq req);
}
