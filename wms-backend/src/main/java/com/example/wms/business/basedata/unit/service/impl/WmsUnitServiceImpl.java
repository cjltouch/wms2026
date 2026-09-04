package com.example.wms.business.basedata.unit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.wms.business.basedata.unit.dto.req.UnitPageReq;
import com.example.wms.business.basedata.unit.dto.req.UnitSaveReq;
import com.example.wms.business.basedata.unit.entity.WmsUnit;
import com.example.wms.business.basedata.unit.mapper.WmsUnitMapper;
import com.example.wms.business.basedata.unit.service.WmsUnitService;
import com.example.wms.common.PageRsp;
import com.example.wms.common.exception.BizException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class WmsUnitServiceImpl extends ServiceImpl<WmsUnitMapper, WmsUnit> implements WmsUnitService {

    @Override
    public PageRsp<WmsUnit> pageUnit(UnitPageReq req) {
        LambdaQueryWrapper<WmsUnit> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(req.getUnitName())) {
            wrapper.like(WmsUnit::getUnitName, req.getUnitName());
        }
        if (StringUtils.hasText(req.getUnitCode())) {
            wrapper.like(WmsUnit::getUnitCode, req.getUnitCode());
        }
        if (StringUtils.hasText(req.getStatus())) {
            wrapper.eq(WmsUnit::getStatus, req.getStatus());
        }
        wrapper.orderByAsc(WmsUnit::getUnitId);
        Page<WmsUnit> page = this.page(new Page<>(req.getPageNum(), req.getPageSize()), wrapper);
        return new PageRsp<>(page.getTotal(), page.getRecords(), req.getPageNum(), req.getPageSize());
    }

    @Override
    public List<WmsUnit> listAll() {
        return this.list(new LambdaQueryWrapper<WmsUnit>()
                .eq(WmsUnit::getStatus, "0")
                .orderByAsc(WmsUnit::getUnitId));
    }

    @Override
    public void saveUnit(UnitSaveReq req) {
        long count = this.lambdaQuery()
                .eq(WmsUnit::getUnitCode, req.getUnitCode())
                .count();
        if (count > 0) {
            throw new BizException("单位编码已存在");
        }
        WmsUnit unit = new WmsUnit();
        unit.setUnitName(req.getUnitName());
        unit.setUnitCode(req.getUnitCode());
        unit.setStatus(req.getStatus() == null ? "0" : req.getStatus());
        this.save(unit);
    }

    @Override
    public void updateUnit(UnitSaveReq req) {
        if (req.getUnitId() == null) {
            throw new BizException("单位ID不能为空");
        }
        WmsUnit exist = this.getById(req.getUnitId());
        if (exist == null) {
            throw new BizException("单位不存在");
        }
        long count = this.lambdaQuery()
                .eq(WmsUnit::getUnitCode, req.getUnitCode())
                .ne(WmsUnit::getUnitId, req.getUnitId())
                .count();
        if (count > 0) {
            throw new BizException("单位编码已存在");
        }
        exist.setUnitName(req.getUnitName());
        exist.setUnitCode(req.getUnitCode());
        exist.setStatus(req.getStatus());
        this.updateById(exist);
    }

    @Override
    public void deleteUnit(Long unitId) {
        this.removeById(unitId);
    }

    @Override
    public void batchDeleteUnit(List<Long> unitIds) {
        if (unitIds == null || unitIds.isEmpty()) {
            return;
        }
        this.removeByIds(unitIds);
    }
}
