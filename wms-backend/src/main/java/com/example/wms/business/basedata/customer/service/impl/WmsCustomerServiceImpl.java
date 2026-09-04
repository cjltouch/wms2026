package com.example.wms.business.basedata.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.wms.business.basedata.customer.dto.req.CustomerPageReq;
import com.example.wms.business.basedata.customer.dto.req.CustomerSaveReq;
import com.example.wms.business.basedata.customer.entity.WmsCustomer;
import com.example.wms.business.basedata.customer.mapper.WmsCustomerMapper;
import com.example.wms.business.basedata.customer.service.WmsCustomerService;
import com.example.wms.common.PageRsp;
import com.example.wms.common.exception.BizException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class WmsCustomerServiceImpl extends ServiceImpl<WmsCustomerMapper, WmsCustomer> implements WmsCustomerService {

    @Override
    public PageRsp<WmsCustomer> pageCustomer(CustomerPageReq req) {
        LambdaQueryWrapper<WmsCustomer> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(req.getCustomerName())) {
            wrapper.like(WmsCustomer::getCustomerName, req.getCustomerName());
        }
        if (StringUtils.hasText(req.getCustomerCode())) {
            wrapper.like(WmsCustomer::getCustomerCode, req.getCustomerCode());
        }
        if (StringUtils.hasText(req.getContact())) {
            wrapper.like(WmsCustomer::getContact, req.getContact());
        }
        if (StringUtils.hasText(req.getPhone())) {
            wrapper.like(WmsCustomer::getPhone, req.getPhone());
        }
        if (StringUtils.hasText(req.getStatus())) {
            wrapper.eq(WmsCustomer::getStatus, req.getStatus());
        }
        wrapper.orderByDesc(WmsCustomer::getCreateTime);
        Page<WmsCustomer> page = this.page(new Page<>(req.getPageNum(), req.getPageSize()), wrapper);
        return new PageRsp<>(page.getTotal(), page.getRecords(), req.getPageNum(), req.getPageSize());
    }

    @Override
    public List<WmsCustomer> listAll() {
        return this.list(new LambdaQueryWrapper<WmsCustomer>()
                .eq(WmsCustomer::getStatus, "0")
                .orderByDesc(WmsCustomer::getCreateTime));
    }

    @Override
    public void saveCustomer(CustomerSaveReq req) {
        long count = this.lambdaQuery()
                .eq(WmsCustomer::getCustomerCode, req.getCustomerCode())
                .count();
        if (count > 0) {
            throw new BizException("客户编码已存在");
        }
        WmsCustomer customer = new WmsCustomer();
        customer.setCustomerCode(req.getCustomerCode());
        customer.setCustomerName(req.getCustomerName());
        customer.setShortName(req.getShortName());
        customer.setContact(req.getContact());
        customer.setPhone(req.getPhone());
        customer.setTel(req.getTel());
        customer.setEmail(req.getEmail());
        customer.setAddress(req.getAddress());
        customer.setCreditLevel(req.getCreditLevel());
        customer.setPaymentDays(req.getPaymentDays());
        customer.setStatus(req.getStatus() == null ? "0" : req.getStatus());
        this.save(customer);
    }

    @Override
    public void updateCustomer(CustomerSaveReq req) {
        if (req.getCustomerId() == null) {
            throw new BizException("客户ID不能为空");
        }
        WmsCustomer exist = this.getById(req.getCustomerId());
        if (exist == null) {
            throw new BizException("客户不存在");
        }
        long count = this.lambdaQuery()
                .eq(WmsCustomer::getCustomerCode, req.getCustomerCode())
                .ne(WmsCustomer::getCustomerId, req.getCustomerId())
                .count();
        if (count > 0) {
            throw new BizException("客户编码已存在");
        }
        exist.setCustomerCode(req.getCustomerCode());
        exist.setCustomerName(req.getCustomerName());
        exist.setShortName(req.getShortName());
        exist.setContact(req.getContact());
        exist.setPhone(req.getPhone());
        exist.setTel(req.getTel());
        exist.setEmail(req.getEmail());
        exist.setAddress(req.getAddress());
        exist.setCreditLevel(req.getCreditLevel());
        exist.setPaymentDays(req.getPaymentDays());
        exist.setStatus(req.getStatus());
        this.updateById(exist);
    }

    @Override
    public void deleteCustomer(Long customerId) {
        this.removeById(customerId);
    }

    @Override
    public void batchDeleteCustomer(List<Long> customerIds) {
        if (customerIds == null || customerIds.isEmpty()) {
            return;
        }
        this.removeByIds(customerIds);
    }
}
