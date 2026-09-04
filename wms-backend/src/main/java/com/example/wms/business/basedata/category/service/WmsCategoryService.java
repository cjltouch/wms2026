package com.example.wms.business.basedata.category.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.wms.business.basedata.category.dto.req.CategoryPageReq;
import com.example.wms.business.basedata.category.dto.req.CategorySaveReq;
import com.example.wms.business.basedata.category.entity.WmsCategory;
import com.example.wms.common.PageRsp;

import java.util.List;

public interface WmsCategoryService extends IService<WmsCategory> {

    PageRsp<WmsCategory> pageCategory(CategoryPageReq req);

    List<WmsCategory> categoryTree(WmsCategory category);

    List<WmsCategory> listAll();

    void saveCategory(CategorySaveReq req);

    void updateCategory(CategorySaveReq req);

    void deleteCategory(Long categoryId);

    void batchDeleteCategory(List<Long> categoryIds);
}
