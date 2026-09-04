package com.example.wms.business.basedata.goods.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.wms.business.basedata.goods.dto.req.GoodsSpuPageReq;
import com.example.wms.business.basedata.goods.dto.req.SpuBatchChangeStatusReq;
import com.example.wms.business.basedata.goods.dto.req.SpuSaveReq;
import com.example.wms.business.basedata.goods.entity.WmsGoodsSku;
import com.example.wms.business.basedata.goods.entity.WmsGoodsSpu;
import com.example.wms.common.PageRsp;

import java.util.List;

public interface WmsGoodsSpuService extends IService<WmsGoodsSpu> {

    PageRsp<WmsGoodsSpu> pageSpu(GoodsSpuPageReq req);

    WmsGoodsSpu getSpuDetail(Long spuId);

    Long saveSpuWithSku(SpuSaveReq req);

    void deleteSpu(Long spuId);

    void batchDeleteSpu(List<Long> spuIds);

    void batchChangeStatus(SpuBatchChangeStatusReq req);

    List<WmsGoodsSku> searchSku(String keyword, Long warehouseId, Long supplierId);
}
