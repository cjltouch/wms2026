package com.example.wms.business.basedata.location.service.impl;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.wms.business.basedata.location.dto.req.LocationBatchGenerateReq;
import com.example.wms.business.basedata.location.dto.req.LocationPageReq;
import com.example.wms.business.basedata.location.dto.req.LocationSaveReq;
import com.example.wms.business.basedata.location.entity.WmsLocation;
import com.example.wms.business.basedata.location.mapper.WmsLocationMapper;
import com.example.wms.business.basedata.location.service.WmsLocationService;
import com.example.wms.common.PageRsp;
import com.example.wms.common.exception.BizException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class WmsLocationServiceImpl extends ServiceImpl<WmsLocationMapper, WmsLocation> implements WmsLocationService {

    private static final Pattern TEMPLATE_RANGE_PATTERN = Pattern.compile("^(.*?)(\\d+)-(\\d+)-(\\d+)~(.*?)(\\d+)-(\\d+)-(\\d+)$");

    @Override
    public PageRsp<WmsLocation> pageLocation(LocationPageReq req) {
        LambdaQueryWrapper<WmsLocation> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(req.getLocationCode())) {
            wrapper.like(WmsLocation::getLocationCode, req.getLocationCode());
        }
        if (StringUtils.hasText(req.getLocationName())) {
            wrapper.like(WmsLocation::getLocationName, req.getLocationName());
        }
        if (req.getWarehouseId() != null) {
            wrapper.eq(WmsLocation::getWarehouseId, req.getWarehouseId());
        }
        if (req.getAreaId() != null) {
            wrapper.eq(WmsLocation::getAreaId, req.getAreaId());
        }
        if (StringUtils.hasText(req.getLocationType())) {
            wrapper.eq(WmsLocation::getLocationType, req.getLocationType());
        }
        if (StringUtils.hasText(req.getStatus())) {
            wrapper.eq(WmsLocation::getStatus, req.getStatus());
        }
        wrapper.orderByAsc(WmsLocation::getSort);
        Page<WmsLocation> page = this.page(new Page<>(req.getPageNum(), req.getPageSize()), wrapper);
        return new PageRsp<>(page.getTotal(), page.getRecords(), req.getPageNum(), req.getPageSize());
    }

    @Override
    public List<WmsLocation> listAll() {
        return this.list(new LambdaQueryWrapper<WmsLocation>()
                .eq(WmsLocation::getStatus, "0")
                .orderByAsc(WmsLocation::getSort));
    }

    @Override
    public List<WmsLocation> listByAreaId(Long areaId) {
        return this.list(new LambdaQueryWrapper<WmsLocation>()
                .eq(WmsLocation::getAreaId, areaId)
                .eq(WmsLocation::getStatus, "0")
                .orderByAsc(WmsLocation::getSort));
    }

    @Override
    public void saveLocation(LocationSaveReq req) {
        long count = this.lambdaQuery()
                .eq(WmsLocation::getLocationCode, req.getLocationCode())
                .count();
        if (count > 0) {
            throw new BizException("库位编码已存在");
        }
        WmsLocation location = buildLocationFromReq(req, null);
        this.save(location);
    }

    @Override
    public void updateLocation(LocationSaveReq req) {
        if (req.getLocationId() == null) {
            throw new BizException("库位ID不能为空");
        }
        WmsLocation exist = this.getById(req.getLocationId());
        if (exist == null) {
            throw new BizException("库位不存在");
        }
        long count = this.lambdaQuery()
                .eq(WmsLocation::getLocationCode, req.getLocationCode())
                .ne(WmsLocation::getLocationId, req.getLocationId())
                .count();
        if (count > 0) {
            throw new BizException("库位编码已存在");
        }
        WmsLocation location = buildLocationFromReq(req, exist);
        this.updateById(location);
    }

    private WmsLocation buildLocationFromReq(LocationSaveReq req, WmsLocation exist) {
        WmsLocation location = exist != null ? exist : new WmsLocation();
        location.setWarehouseId(req.getWarehouseId());
        location.setAreaId(req.getAreaId());
        location.setLocationCode(req.getLocationCode());
        location.setLocationName(req.getLocationName());
        location.setLocationType(req.getLocationType());
        location.setRowNo(req.getRowNo());
        location.setColumnNo(req.getColumnNo());
        location.setLevelNo(req.getLevelNo());
        location.setMaxQty(req.getMaxQty());
        location.setSort(req.getSort() == null ? 0 : req.getSort());
        location.setStatus(req.getStatus() == null ? "0" : req.getStatus());
        return location;
    }

    @Override
    public void deleteLocation(Long locationId) {
        this.removeById(locationId);
    }

    @Override
    public void batchDeleteLocation(List<Long> locationIds) {
        if (locationIds == null || locationIds.isEmpty()) {
            return;
        }
        this.removeByIds(locationIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchGenerate(LocationBatchGenerateReq req) {
        if (req.getWarehouseId() == null) {
            throw new BizException("仓库ID不能为空");
        }
        if (req.getAreaId() == null) {
            throw new BizException("库区ID不能为空");
        }
        if (!StringUtils.hasText(req.getLocationTemplate())) {
            throw new BizException("生成模板不能为空");
        }

        String template = req.getLocationTemplate().trim();
        Matcher matcher = TEMPLATE_RANGE_PATTERN.matcher(template);
        if (!matcher.matches()) {
            throw new BizException("模板格式错误，正确格式如：A01-01-01~A10-10-05");
        }

        String prefixStart = matcher.group(1);
        int rowStart = Integer.parseInt(matcher.group(2));
        int colStart = Integer.parseInt(matcher.group(3));
        int levelStart = Integer.parseInt(matcher.group(4));
        String prefixEnd = matcher.group(5);
        int rowEnd = Integer.parseInt(matcher.group(6));
        int colEnd = Integer.parseInt(matcher.group(7));
        int levelEnd = Integer.parseInt(matcher.group(8));

        if (rowEnd < rowStart || colEnd < colStart || levelEnd < levelStart) {
            throw new BizException("模板范围错误，结束值不能小于起始值");
        }
        if (!StrUtil.equals(prefixStart, prefixEnd)) {
            throw new BizException("模板前缀不一致");
        }

        String prefix = prefixStart;
        int rowWidth = matcher.group(2).length();
        int colWidth = matcher.group(3).length();
        int levelWidth = matcher.group(4).length();

        List<WmsLocation> locations = new ArrayList<>();
        int sortBase = 0;
        String status = req.getStatus() == null ? "0" : req.getStatus();

        for (int r = rowStart; r <= rowEnd; r++) {
            for (int c = colStart; c <= colEnd; c++) {
                for (int l = levelStart; l <= levelEnd; l++) {
                    String rowStr = String.format("%0" + rowWidth + "d", r);
                    String colStr = String.format("%0" + colWidth + "d", c);
                    String levelStr = String.format("%0" + levelWidth + "d", l);
                    String code = prefix + rowStr + "-" + colStr + "-" + levelStr;
                    WmsLocation loc = new WmsLocation();
                    loc.setWarehouseId(req.getWarehouseId());
                    loc.setAreaId(req.getAreaId());
                    loc.setLocationCode(code);
                    loc.setLocationName(code);
                    loc.setLocationType(req.getLocationType());
                    loc.setRowNo(r);
                    loc.setColumnNo(c);
                    loc.setLevelNo(l);
                    loc.setMaxQty(req.getMaxQty());
                    loc.setSort(sortBase++);
                    loc.setStatus(status);
                    locations.add(loc);
                }
            }
        }

        if (locations.isEmpty()) {
            return 0;
        }

        List<String> existCodes = this.lambdaQuery()
                .in(WmsLocation::getLocationCode, locations.stream().map(WmsLocation::getLocationCode).toList())
                .list()
                .stream()
                .map(WmsLocation::getLocationCode)
                .toList();

        if (!existCodes.isEmpty()) {
            throw new BizException("库位编码已存在：" + String.join(",", existCodes.subList(0, Math.min(5, existCodes.size()))));
        }

        this.saveBatch(locations);
        return locations.size();
    }
}
