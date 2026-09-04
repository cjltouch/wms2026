package com.example.wms.business.basedata.supplier.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.wms.business.basedata.supplier.dto.req.SupplierPageReq;
import com.example.wms.business.basedata.supplier.dto.req.SupplierSaveReq;
import com.example.wms.business.basedata.supplier.entity.WmsSupplier;
import com.example.wms.common.PageRsp;

import java.util.List;

public interface WmsSupplierService extends IService<WmsSupplier> {

    PageRsp<WmsSupplier> pageSupplier(SupplierPageReq req);

    List<WmsSupplier> listAll();

    void saveSupplier(SupplierSaveReq req);

    void updateSupplier(SupplierSaveReq req);

    void deleteSupplier(Long supplierId);

    void batchDeleteSupplier(List<Long> supplierIds);
}
