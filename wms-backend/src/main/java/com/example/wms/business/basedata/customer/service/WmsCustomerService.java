package com.example.wms.business.basedata.customer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.wms.business.basedata.customer.dto.req.CustomerPageReq;
import com.example.wms.business.basedata.customer.dto.req.CustomerSaveReq;
import com.example.wms.business.basedata.customer.entity.WmsCustomer;
import com.example.wms.common.PageRsp;

import java.util.List;

public interface WmsCustomerService extends IService<WmsCustomer> {

    PageRsp<WmsCustomer> pageCustomer(CustomerPageReq req);

    List<WmsCustomer> listAll();

    void saveCustomer(CustomerSaveReq req);

    void updateCustomer(CustomerSaveReq req);

    void deleteCustomer(Long customerId);

    void batchDeleteCustomer(List<Long> customerIds);
}
