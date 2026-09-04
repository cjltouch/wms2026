package com.example.wms.business.basedata.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.wms.business.basedata.warehouse.dto.req.WarehousePageReq;
import com.example.wms.business.basedata.warehouse.dto.req.WarehouseSaveReq;
import com.example.wms.business.basedata.warehouse.entity.WmsWarehouse;
import com.example.wms.business.basedata.warehouse.mapper.WmsWarehouseMapper;
import com.example.wms.business.basedata.warehouse.service.WmsWarehouseService;
import com.example.wms.common.PageRsp;
import com.example.wms.common.exception.BizException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class WmsWarehouseServiceImpl extends ServiceImpl<WmsWarehouseMapper, WmsWarehouse> implements WmsWarehouseService {

    @Override
    public PageRsp<WmsWarehouse> pageWarehouse(WarehousePageReq req) {
        LambdaQueryWrapper<WmsWarehouse> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(req.getWarehouseName())) {
            wrapper.like(WmsWarehouse::getWarehouseName, req.getWarehouseName());
        }
        if (StringUtils.hasText(req.getWarehouseCode())) {
            wrapper.like(WmsWarehouse::getWarehouseCode, req.getWarehouseCode());
        }
        if (req.getWarehouseType() != null) {
            wrapper.eq(WmsWarehouse::getWarehouseType, req.getWarehouseType());
        }
        if (StringUtils.hasText(req.getStatus())) {
            wrapper.eq(WmsWarehouse::getStatus, req.getStatus());
        }
        wrapper.orderByAsc(WmsWarehouse::getSort);
        Page<WmsWarehouse> page = this.page(new Page<>(req.getPageNum(), req.getPageSize()), wrapper);
        return new PageRsp<>(page.getTotal(), page.getRecords(), req.getPageNum(), req.getPageSize());
    }

    @Override
    public List<WmsWarehouse> listAll() {
        return this.list(new LambdaQueryWrapper<WmsWarehouse>()
                .eq(WmsWarehouse::getStatus, "0")
                .orderByAsc(WmsWarehouse::getSort));
    }

    @Override
    public void saveWarehouse(WarehouseSaveReq req) {
        long count = this.lambdaQuery()
                .eq(WmsWarehouse::getWarehouseCode, req.getWarehouseCode())
                .count();
        if (count > 0) {
            throw new BizException("仓库编码已存在");
        }
        WmsWarehouse warehouse = new WmsWarehouse();
        warehouse.setWarehouseCode(req.getWarehouseCode());
        warehouse.setWarehouseName(req.getWarehouseName());
        warehouse.setWarehouseType(req.getWarehouseType());
        warehouse.setProvince(req.getProvince());
        warehouse.setCity(req.getCity());
        warehouse.setDistrict(req.getDistrict());
        warehouse.setDetailAddress(req.getDetailAddress());
        warehouse.setLongitude(req.getLongitude());
        warehouse.setLatitude(req.getLatitude());
        warehouse.setManager(req.getManager());
        warehouse.setPhone(req.getPhone());
        warehouse.setArea(req.getArea());
        warehouse.setSort(req.getSort() == null ? 0 : req.getSort());
        warehouse.setStatus(req.getStatus() == null ? "0" : req.getStatus());
        this.save(warehouse);
    }

    @Override
    public void updateWarehouse(WarehouseSaveReq req) {
        if (req.getWarehouseId() == null) {
            throw new BizException("仓库ID不能为空");
        }
        WmsWarehouse exist = this.getById(req.getWarehouseId());
        if (exist == null) {
            throw new BizException("仓库不存在");
        }
        long count = this.lambdaQuery()
                .eq(WmsWarehouse::getWarehouseCode, req.getWarehouseCode())
                .ne(WmsWarehouse::getWarehouseId, req.getWarehouseId())
                .count();
        if (count > 0) {
            throw new BizException("仓库编码已存在");
        }
        exist.setWarehouseCode(req.getWarehouseCode());
        exist.setWarehouseName(req.getWarehouseName());
        exist.setWarehouseType(req.getWarehouseType());
        exist.setProvince(req.getProvince());
        exist.setCity(req.getCity());
        exist.setDistrict(req.getDistrict());
        exist.setDetailAddress(req.getDetailAddress());
        exist.setLongitude(req.getLongitude());
        exist.setLatitude(req.getLatitude());
        exist.setManager(req.getManager());
        exist.setPhone(req.getPhone());
        exist.setArea(req.getArea());
        exist.setSort(req.getSort() == null ? 0 : req.getSort());
        exist.setStatus(req.getStatus());
        this.updateById(exist);
    }

    @Override
    public void deleteWarehouse(Long warehouseId) {
        this.removeById(warehouseId);
    }

    @Override
    public void batchDeleteWarehouse(List<Long> warehouseIds) {
        if (warehouseIds == null || warehouseIds.isEmpty()) {
            return;
        }
        this.removeByIds(warehouseIds);
    }
}
