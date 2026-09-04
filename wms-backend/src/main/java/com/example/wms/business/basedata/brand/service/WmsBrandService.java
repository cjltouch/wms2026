package com.example.wms.business.basedata.brand.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.wms.business.basedata.brand.dto.req.BrandPageReq;
import com.example.wms.business.basedata.brand.dto.req.BrandSaveReq;
import com.example.wms.business.basedata.brand.entity.WmsBrand;
import com.example.wms.common.PageRsp;

import java.util.List;

public interface WmsBrandService extends IService<WmsBrand> {

    PageRsp<WmsBrand> pageBrand(BrandPageReq req);

    List<WmsBrand> listAll();

    void saveBrand(BrandSaveReq req);

    void updateBrand(BrandSaveReq req);

    void deleteBrand(Long brandId);

    void batchDeleteBrand(List<Long> brandIds);
}
