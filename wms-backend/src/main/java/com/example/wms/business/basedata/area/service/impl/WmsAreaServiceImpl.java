package com.example.wms.business.basedata.area.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.wms.business.basedata.area.dto.req.AreaPageReq;
import com.example.wms.business.basedata.area.dto.req.AreaSaveReq;
import com.example.wms.business.basedata.area.entity.WmsArea;
import com.example.wms.business.basedata.area.mapper.WmsAreaMapper;
import com.example.wms.business.basedata.area.service.WmsAreaService;
import com.example.wms.common.PageRsp;
import com.example.wms.common.exception.BizException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class WmsAreaServiceImpl extends ServiceImpl<WmsAreaMapper, WmsArea> implements WmsAreaService {

    @Override
    public PageRsp<WmsArea> pageArea(AreaPageReq req) {
        LambdaQueryWrapper<WmsArea> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(req.getAreaName())) {
            wrapper.like(WmsArea::getAreaName, req.getAreaName());
        }
        if (StringUtils.hasText(req.getAreaCode())) {
            wrapper.like(WmsArea::getAreaCode, req.getAreaCode());
        }
        if (req.getWarehouseId() != null) {
            wrapper.eq(WmsArea::getWarehouseId, req.getWarehouseId());
        }
        if (req.getAreaType() != null) {
            wrapper.eq(WmsArea::getAreaType, req.getAreaType());
        }
        if (StringUtils.hasText(req.getStatus())) {
            wrapper.eq(WmsArea::getStatus, req.getStatus());
        }
        wrapper.orderByAsc(WmsArea::getSort);
        Page<WmsArea> page = this.page(new Page<>(req.getPageNum(), req.getPageSize()), wrapper);
        return new PageRsp<>(page.getTotal(), page.getRecords(), req.getPageNum(), req.getPageSize());
    }

    @Override
    public List<WmsArea> listAll() {
        return this.list(new LambdaQueryWrapper<WmsArea>()
                .eq(WmsArea::getStatus, "0")
                .orderByAsc(WmsArea::getSort));
    }

    @Override
    public List<WmsArea> listByWarehouseId(Long warehouseId) {
        return this.list(new LambdaQueryWrapper<WmsArea>()
                .eq(WmsArea::getWarehouseId, warehouseId)
                .eq(WmsArea::getStatus, "0")
                .orderByAsc(WmsArea::getSort));
    }

    @Override
    public void saveArea(AreaSaveReq req) {
        if (req.getWarehouseId() == null) {
            throw new BizException("仓库ID不能为空");
        }
        long count = this.lambdaQuery()
                .eq(WmsArea::getWarehouseId, req.getWarehouseId())
                .eq(WmsArea::getAreaCode, req.getAreaCode())
                .count();
        if (count > 0) {
            throw new BizException("该仓库下库区编码已存在");
        }
        WmsArea area = new WmsArea();
        area.setWarehouseId(req.getWarehouseId());
        area.setAreaCode(req.getAreaCode());
        area.setAreaName(req.getAreaName());
        area.setAreaType(req.getAreaType());
        area.setManager(req.getManager());
        area.setPhone(req.getPhone());
        area.setSort(req.getSort() == null ? 0 : req.getSort());
        area.setStatus(req.getStatus() == null ? "0" : req.getStatus());
        this.save(area);
    }

    @Override
    public void updateArea(AreaSaveReq req) {
        if (req.getAreaId() == null) {
            throw new BizException("库区ID不能为空");
        }
        WmsArea exist = this.getById(req.getAreaId());
        if (exist == null) {
            throw new BizException("库区不存在");
        }
        long count = this.lambdaQuery()
                .eq(WmsArea::getWarehouseId, req.getWarehouseId())
                .eq(WmsArea::getAreaCode, req.getAreaCode())
                .ne(WmsArea::getAreaId, req.getAreaId())
                .count();
        if (count > 0) {
            throw new BizException("该仓库下库区编码已存在");
        }
        exist.setWarehouseId(req.getWarehouseId());
        exist.setAreaCode(req.getAreaCode());
        exist.setAreaName(req.getAreaName());
        exist.setAreaType(req.getAreaType());
        exist.setManager(req.getManager());
        exist.setPhone(req.getPhone());
        exist.setSort(req.getSort() == null ? 0 : req.getSort());
        exist.setStatus(req.getStatus());
        this.updateById(exist);
    }

    @Override
    public void deleteArea(Long areaId) {
        this.removeById(areaId);
    }

    @Override
    public void batchDeleteArea(List<Long> areaIds) {
        if (areaIds == null || areaIds.isEmpty()) {
            return;
        }
        this.removeByIds(areaIds);
    }
}
