package com.example.wms.business.basedata.supplier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.wms.business.basedata.supplier.dto.req.SupplierPageReq;
import com.example.wms.business.basedata.supplier.dto.req.SupplierSaveReq;
import com.example.wms.business.basedata.supplier.entity.WmsSupplier;
import com.example.wms.business.basedata.supplier.mapper.WmsSupplierMapper;
import com.example.wms.business.basedata.supplier.service.WmsSupplierService;
import com.example.wms.common.PageRsp;
import com.example.wms.common.exception.BizException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class WmsSupplierServiceImpl extends ServiceImpl<WmsSupplierMapper, WmsSupplier> implements WmsSupplierService {

    @Override
    public PageRsp<WmsSupplier> pageSupplier(SupplierPageReq req) {
        LambdaQueryWrapper<WmsSupplier> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(req.getSupplierName())) {
            wrapper.like(WmsSupplier::getSupplierName, req.getSupplierName());
        }
        if (StringUtils.hasText(req.getSupplierCode())) {
            wrapper.like(WmsSupplier::getSupplierCode, req.getSupplierCode());
        }
        if (StringUtils.hasText(req.getContact())) {
            wrapper.like(WmsSupplier::getContact, req.getContact());
        }
        if (StringUtils.hasText(req.getPhone())) {
            wrapper.like(WmsSupplier::getPhone, req.getPhone());
        }
        if (StringUtils.hasText(req.getStatus())) {
            wrapper.eq(WmsSupplier::getStatus, req.getStatus());
        }
        wrapper.orderByDesc(WmsSupplier::getCreateTime);
        Page<WmsSupplier> page = this.page(new Page<>(req.getPageNum(), req.getPageSize()), wrapper);
        return new PageRsp<>(page.getTotal(), page.getRecords(), req.getPageNum(), req.getPageSize());
    }

    @Override
    public List<WmsSupplier> listAll() {
        return this.list(new LambdaQueryWrapper<WmsSupplier>()
                .eq(WmsSupplier::getStatus, "0")
                .orderByDesc(WmsSupplier::getCreateTime));
    }

    @Override
    public void saveSupplier(SupplierSaveReq req) {
        long count = this.lambdaQuery()
                .eq(WmsSupplier::getSupplierCode, req.getSupplierCode())
                .count();
        if (count > 0) {
            throw new BizException("供应商编码已存在");
        }
        WmsSupplier supplier = new WmsSupplier();
        supplier.setSupplierCode(req.getSupplierCode());
        supplier.setSupplierName(req.getSupplierName());
        supplier.setShortName(req.getShortName());
        supplier.setContact(req.getContact());
        supplier.setPhone(req.getPhone());
        supplier.setTel(req.getTel());
        supplier.setEmail(req.getEmail());
        supplier.setFax(req.getFax());
        supplier.setQq(req.getQq());
        supplier.setWechat(req.getWechat());
        supplier.setAddress(req.getAddress());
        supplier.setTaxNo(req.getTaxNo());
        supplier.setBankName(req.getBankName());
        supplier.setBankAccount(req.getBankAccount());
        supplier.setTaxRate(req.getTaxRate());
        supplier.setPaymentTerms(req.getPaymentTerms());
        supplier.setCreditLevel(req.getCreditLevel());
        supplier.setStatus(req.getStatus() == null ? "0" : req.getStatus());
        this.save(supplier);
    }

    @Override
    public void updateSupplier(SupplierSaveReq req) {
        if (req.getSupplierId() == null) {
            throw new BizException("供应商ID不能为空");
        }
        WmsSupplier exist = this.getById(req.getSupplierId());
        if (exist == null) {
            throw new BizException("供应商不存在");
        }
        long count = this.lambdaQuery()
                .eq(WmsSupplier::getSupplierCode, req.getSupplierCode())
                .ne(WmsSupplier::getSupplierId, req.getSupplierId())
                .count();
        if (count > 0) {
            throw new BizException("供应商编码已存在");
        }
        exist.setSupplierCode(req.getSupplierCode());
        exist.setSupplierName(req.getSupplierName());
        exist.setShortName(req.getShortName());
        exist.setContact(req.getContact());
        exist.setPhone(req.getPhone());
        exist.setTel(req.getTel());
        exist.setEmail(req.getEmail());
        exist.setFax(req.getFax());
        exist.setQq(req.getQq());
        exist.setWechat(req.getWechat());
        exist.setAddress(req.getAddress());
        exist.setTaxNo(req.getTaxNo());
        exist.setBankName(req.getBankName());
        exist.setBankAccount(req.getBankAccount());
        exist.setTaxRate(req.getTaxRate());
        exist.setPaymentTerms(req.getPaymentTerms());
        exist.setCreditLevel(req.getCreditLevel());
        exist.setStatus(req.getStatus());
        this.updateById(exist);
    }

    @Override
    public void deleteSupplier(Long supplierId) {
        this.removeById(supplierId);
    }

    @Override
    public void batchDeleteSupplier(List<Long> supplierIds) {
        if (supplierIds == null || supplierIds.isEmpty()) {
            return;
        }
        this.removeByIds(supplierIds);
    }
}
