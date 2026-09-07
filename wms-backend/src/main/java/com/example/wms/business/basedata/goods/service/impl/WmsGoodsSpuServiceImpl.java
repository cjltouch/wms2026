package com.example.wms.business.basedata.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.wms.business.basedata.brand.entity.WmsBrand;
import com.example.wms.business.basedata.brand.service.WmsBrandService;
import com.example.wms.business.basedata.category.entity.WmsCategory;
import com.example.wms.business.basedata.category.service.WmsCategoryService;
import com.example.wms.business.basedata.goods.dto.req.GoodsSpuPageReq;
import com.example.wms.business.basedata.goods.dto.req.SkuSaveReq;
import com.example.wms.business.basedata.goods.dto.req.SpuBatchChangeStatusReq;
import com.example.wms.business.basedata.goods.dto.req.SpuSaveReq;
import com.example.wms.business.basedata.goods.entity.WmsGoodsSku;
import com.example.wms.business.basedata.goods.mapper.WmsGoodsSkuMapper;
import com.example.wms.business.basedata.goods.entity.WmsGoodsSpu;
import com.example.wms.business.basedata.goods.mapper.WmsGoodsSpuMapper;
import com.example.wms.business.basedata.goods.service.WmsGoodsSkuService;
import com.example.wms.business.basedata.goods.service.WmsGoodsSpuService;
import com.example.wms.business.basedata.unit.entity.WmsUnit;
import com.example.wms.business.basedata.unit.service.WmsUnitService;
import com.example.wms.common.PageRsp;
import com.example.wms.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WmsGoodsSpuServiceImpl extends ServiceImpl<WmsGoodsSpuMapper, WmsGoodsSpu> implements WmsGoodsSpuService {

    private final WmsGoodsSkuService wmsGoodsSkuService;

    @org.springframework.beans.factory.annotation.Autowired
    private WmsGoodsSkuMapper wmsGoodsSkuMapper;
    private final WmsCategoryService wmsCategoryService;
    private final WmsBrandService wmsBrandService;
    private final WmsUnitService wmsUnitService;

    @Override
    public PageRsp<WmsGoodsSpu> pageSpu(GoodsSpuPageReq req) {
        LambdaQueryWrapper<WmsGoodsSpu> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(req.getKeyword())) {
            wrapper.and(w -> w.like(WmsGoodsSpu::getSpuName, req.getKeyword())
                    .or().like(WmsGoodsSpu::getSpuCode, req.getKeyword()));
        }
        if (StringUtils.hasText(req.getSpuName())) {
            wrapper.like(WmsGoodsSpu::getSpuName, req.getSpuName());
        }
        if (StringUtils.hasText(req.getSpuCode())) {
            wrapper.like(WmsGoodsSpu::getSpuCode, req.getSpuCode());
        }
        if (req.getCategoryId() != null) {
            wrapper.eq(WmsGoodsSpu::getCategoryId, req.getCategoryId());
        }
        if (req.getBrandId() != null) {
            wrapper.eq(WmsGoodsSpu::getBrandId, req.getBrandId());
        }
        if (StringUtils.hasText(req.getStatus())) {
            wrapper.eq(WmsGoodsSpu::getStatus, req.getStatus());
        }
        wrapper.orderByDesc(WmsGoodsSpu::getCreateTime);
        Page<WmsGoodsSpu> page = this.page(new Page<>(req.getPageNum(), req.getPageSize()), wrapper);

        List<WmsGoodsSpu> records = page.getRecords();
        enrichSpuExtraInfo(records);

        return new PageRsp<>(page.getTotal(), records, req.getPageNum(), req.getPageSize());
    }

    private void enrichSpuExtraInfo(List<WmsGoodsSpu> spus) {
        if (CollectionUtils.isEmpty(spus)) {
            return;
        }
        Set<Long> categoryIds = new HashSet<>();
        Set<Long> brandIds = new HashSet<>();
        Set<Long> unitIds = new HashSet<>();
        for (WmsGoodsSpu spu : spus) {
            if (spu.getCategoryId() != null) categoryIds.add(spu.getCategoryId());
            if (spu.getBrandId() != null) brandIds.add(spu.getBrandId());
            if (spu.getUnitId() != null) unitIds.add(spu.getUnitId());
        }
        if (!categoryIds.isEmpty()) {
            Map<Long, String> categoryMap = wmsCategoryService.listByIds(categoryIds)
                    .stream().collect(Collectors.toMap(WmsCategory::getCategoryId, WmsCategory::getCategoryName));
            spus.forEach(s -> s.setCategoryName(categoryMap.get(s.getCategoryId())));
        }
        if (!brandIds.isEmpty()) {
            Map<Long, String> brandMap = wmsBrandService.listByIds(brandIds)
                    .stream().collect(Collectors.toMap(WmsBrand::getBrandId, WmsBrand::getBrandName));
            spus.forEach(s -> s.setBrandName(brandMap.get(s.getBrandId())));
        }
        if (!unitIds.isEmpty()) {
            Map<Long, String> unitMap = wmsUnitService.listByIds(unitIds)
                    .stream().collect(Collectors.toMap(WmsUnit::getUnitId, WmsUnit::getUnitName));
            spus.forEach(s -> s.setUnitName(unitMap.get(s.getUnitId())));
        }
    }

    @Override
    public WmsGoodsSpu getSpuDetail(Long spuId) {
        WmsGoodsSpu spu = this.getById(spuId);
        if (spu == null) {
            return null;
        }
        List<WmsGoodsSku> skuList = wmsGoodsSkuService.lambdaQuery()
                .eq(WmsGoodsSku::getSpuId, spuId)
                .orderByAsc(WmsGoodsSku::getSkuCode)
                .list();
        spu.setSkuList(skuList);
        enrichSpuExtraInfo(List.of(spu));
        return spu;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveSpuWithSku(SpuSaveReq req) {
        if (req.getSpuId() == null) {
            return insertSpuWithSku(req);
        } else {
            return updateSpuWithSku(req);
        }
    }

    private Long insertSpuWithSku(SpuSaveReq req) {
        long count = this.lambdaQuery()
                .eq(WmsGoodsSpu::getSpuCode, req.getSpuCode())
                .count();
        if (count > 0) {
            throw new BizException("SPU编码已存在");
        }

        validateSkuCodes(req.getSkuList(), null);
        validateBarcodes(req.getSkuList(), null);

        WmsGoodsSpu spu = new WmsGoodsSpu();
        spu.setSpuCode(req.getSpuCode());
        spu.setSpuName(req.getSpuName());
        spu.setCategoryId(req.getCategoryId());
        spu.setBrandId(req.getBrandId());
        spu.setUnitId(req.getUnitId());
        spu.setOrigin(req.getOrigin());
        spu.setDescription(req.getDescription());
        spu.setPicUrls(req.getPicUrls());
        spu.setAbcLevel(req.getAbcLevel());
        spu.setStatus(req.getStatus() == null ? "0" : req.getStatus());
        this.save(spu);

        saveSkuList(spu.getSpuId(), spu.getSpuName(), req.getSkuList());
        return spu.getSpuId();
    }

    private Long updateSpuWithSku(SpuSaveReq req) {
        WmsGoodsSpu exist = this.getById(req.getSpuId());
        if (exist == null) {
            throw new BizException("SPU不存在");
        }
        long count = this.lambdaQuery()
                .eq(WmsGoodsSpu::getSpuCode, req.getSpuCode())
                .ne(WmsGoodsSpu::getSpuId, req.getSpuId())
                .count();
        if (count > 0) {
            throw new BizException("SPU编码已存在");
        }

        validateSkuCodes(req.getSkuList(), req.getSpuId());
        validateBarcodes(req.getSkuList(), req.getSpuId());

        exist.setSpuCode(req.getSpuCode());
        exist.setSpuName(req.getSpuName());
        exist.setCategoryId(req.getCategoryId());
        exist.setBrandId(req.getBrandId());
        exist.setUnitId(req.getUnitId());
        exist.setOrigin(req.getOrigin());
        exist.setDescription(req.getDescription());
        exist.setPicUrls(req.getPicUrls());
        exist.setAbcLevel(req.getAbcLevel());
        exist.setStatus(req.getStatus());
        this.updateById(exist);

        wmsGoodsSkuMapper.physicalDeleteBySpuId(req.getSpuId());


        saveSkuList(req.getSpuId(), req.getSpuName(), req.getSkuList());
        return req.getSpuId();
    }

    private void validateSkuCodes(List<SkuSaveReq> skuList, Long spuId) {
        if (CollectionUtils.isEmpty(skuList)) {
            return;
        }
        List<String> codes = skuList.stream()
                .map(SkuSaveReq::getSkuCode)
                .filter(StringUtils::hasText)
                .toList();
        if (codes.isEmpty()) {
            return;
        }
        Set<String> uniqueCodes = new HashSet<>(codes);
        if (uniqueCodes.size() != codes.size()) {
            throw new BizException("SKU编码重复");
        }
        LambdaQueryWrapper<WmsGoodsSku> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(WmsGoodsSku::getSkuCode, codes);
        if (spuId != null) {
            wrapper.ne(WmsGoodsSku::getSpuId, spuId);
        }
        long exist = wmsGoodsSkuService.count(wrapper);
        if (exist > 0) {
            throw new BizException("SKU编码已存在");
        }
    }

    private void validateBarcodes(List<SkuSaveReq> skuList, Long spuId) {
        if (CollectionUtils.isEmpty(skuList)) {
            return;
        }
        List<String> barcodes = skuList.stream()
                .map(SkuSaveReq::getBarcode)
                .filter(StringUtils::hasText)
                .toList();
        if (barcodes.isEmpty()) {
            return;
        }
        Set<String> uniqueBarcodes = new HashSet<>(barcodes);
        if (uniqueBarcodes.size() != barcodes.size()) {
            throw new BizException("条码重复");
        }
        LambdaQueryWrapper<WmsGoodsSku> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(WmsGoodsSku::getBarcode, barcodes);
        if (spuId != null) {
            wrapper.ne(WmsGoodsSku::getSpuId, spuId);
        }
        long exist = wmsGoodsSkuService.count(wrapper);
        if (exist > 0) {
            throw new BizException("条码已存在");
        }
    }

    private void saveSkuList(Long spuId, String spuName, List<SkuSaveReq> skuList) {
        if (CollectionUtils.isEmpty(skuList)) {
            return;
        }
        List<WmsGoodsSku> skus = new ArrayList<>();
        for (SkuSaveReq req : skuList) {
            WmsGoodsSku sku = new WmsGoodsSku();
            sku.setSpuId(spuId);
            sku.setSupplierId(req.getSupplierId());
            // 唯一索引列（sku_code/barcode）空字符串会判重，统一存NULL
            sku.setSkuCode(StringUtils.hasText(req.getSkuCode()) ? req.getSkuCode().trim() : null);
            sku.setInnerCode(StringUtils.hasText(req.getInnerCode()) ? req.getInnerCode().trim() : null);
            sku.setSkuName(spuName);
            sku.setBarcode(StringUtils.hasText(req.getBarcode()) ? req.getBarcode().trim() : null);
            sku.setSpecText(StringUtils.hasText(req.getSpecText()) ? req.getSpecText() : null);
            sku.setWeightG(req.getWeightG());
            sku.setVolumeMl(req.getVolumeMl());
            sku.setColor(StringUtils.hasText(req.getColor()) ? req.getColor() : null);
            sku.setBatchFlag(req.getBatchFlag() == null ? 0 : req.getBatchFlag());
            sku.setExpireFlag(req.getExpireFlag() == null ? 0 : req.getExpireFlag());
            sku.setSnFlag(req.getSnFlag() == null ? 0 : req.getSnFlag());
            sku.setShelfLifeDays(req.getShelfLifeDays());
            sku.setDefaultCost(req.getDefaultCost());
            sku.setDefaultSale(req.getDefaultSale());
            sku.setStatus(req.getStatus() == null ? "0" : req.getStatus());
            skus.add(sku);
        }
        wmsGoodsSkuService.saveBatch(skus);
    }

    @Override
    public void deleteSpu(Long spuId) {
        this.removeById(spuId);
    }

    @Override
    public void batchDeleteSpu(List<Long> spuIds) {
        if (CollectionUtils.isEmpty(spuIds)) {
            return;
        }
        this.removeByIds(spuIds);
    }

    @Override
    public void batchChangeStatus(SpuBatchChangeStatusReq req) {
        if (CollectionUtils.isEmpty(req.getIds())) {
            return;
        }
        if (!StringUtils.hasText(req.getStatus())) {
            throw new BizException("状态不能为空");
        }
        List<WmsGoodsSpu> spus = this.listByIds(req.getIds());
        for (WmsGoodsSpu spu : spus) {
            spu.setStatus(req.getStatus());
        }
        this.updateBatchById(spus);
    }

    @Override
    public List<WmsGoodsSku> searchSku(String keyword, Long warehouseId, Long supplierId) {
        LambdaQueryWrapper<WmsGoodsSku> skuWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            // 商品名存在 SPU 表，需先按 spuName 匹配出 spuIds 一并纳入条件
            List<Long> matchedSpuIds = this.list(new LambdaQueryWrapper<WmsGoodsSpu>()
                    .like(WmsGoodsSpu::getSpuName, keyword))
                    .stream().map(WmsGoodsSpu::getSpuId).toList();
            skuWrapper.and(w -> {
                w.like(WmsGoodsSku::getSkuCode, keyword)
                        .or().like(WmsGoodsSku::getBarcode, keyword)
                        .or().like(WmsGoodsSku::getSpecText, keyword);
                if (!matchedSpuIds.isEmpty()) {
                    w.or().in(WmsGoodsSku::getSpuId, matchedSpuIds);
                }
            });
        }
        // 采购按供应商选货：传了 supplierId 只返回该供应商的商品
        if (supplierId != null) {
            skuWrapper.eq(WmsGoodsSku::getSupplierId, supplierId);
        }
        skuWrapper.eq(WmsGoodsSku::getStatus, "0");
        skuWrapper.last("LIMIT 200");
        List<WmsGoodsSku> skus = wmsGoodsSkuService.list(skuWrapper);

        if (!skus.isEmpty()) {
            Set<Long> spuIds = skus.stream().map(WmsGoodsSku::getSpuId).collect(Collectors.toSet());
            Map<Long, WmsGoodsSpu> spuMap = this.listByIds(spuIds)
                    .stream().collect(Collectors.toMap(WmsGoodsSpu::getSpuId, s -> s));
            Set<Long> unitIds = spuMap.values().stream()
                    .map(WmsGoodsSpu::getUnitId)
                    .filter(id -> id != null)
                    .collect(Collectors.toSet());
            Map<Long, String> unitMap = unitIds.isEmpty() ? Map.of()
                    : wmsUnitService.listByIds(unitIds)
                            .stream().collect(Collectors.toMap(WmsUnit::getUnitId, WmsUnit::getUnitName));
            for (WmsGoodsSku sku : skus) {
                WmsGoodsSpu spu = spuMap.get(sku.getSpuId());
                if (spu != null) {
                    sku.setSkuName(spu.getSpuName());
                    sku.setUnitId(spu.getUnitId());
                    sku.setUnitName(unitMap.get(spu.getUnitId()));
                }
                sku.setAvailableQty(BigDecimal.ZERO);
            }
        }
        return skus;
    }
}
