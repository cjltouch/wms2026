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
    private final com.example.wms.business.basedata.supplier.service.WmsSupplierService wmsSupplierService;

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

    @Override
    public void exportSpu(GoodsSpuPageReq req, jakarta.servlet.http.HttpServletResponse response) {
        // 不分页，拉全量符合条件的 SPU
        LambdaQueryWrapper<WmsGoodsSpu> wrapper = new LambdaQueryWrapper<>();
        if (org.springframework.util.StringUtils.hasText(req.getKeyword())) {
            wrapper.and(w -> w.like(WmsGoodsSpu::getSpuName, req.getKeyword())
                    .or().like(WmsGoodsSpu::getSpuCode, req.getKeyword()));
        }
        if (org.springframework.util.StringUtils.hasText(req.getSpuName())) {
            wrapper.like(WmsGoodsSpu::getSpuName, req.getSpuName());
        }
        if (org.springframework.util.StringUtils.hasText(req.getSpuCode())) {
            wrapper.like(WmsGoodsSpu::getSpuCode, req.getSpuCode());
        }
        if (req.getCategoryId() != null) {
            // 导出分类：当前分类 + 后代（ancestors 包含当前 categoryId）
            wrapper.and(w -> w.eq(WmsGoodsSpu::getCategoryId, req.getCategoryId())
                    .or().apply("category_id IN (SELECT category_id FROM wms_category WHERE ancestors LIKE CONCAT('%', {0}, '%'))", req.getCategoryId()));
        }
        if (req.getBrandId() != null) {
            wrapper.eq(WmsGoodsSpu::getBrandId, req.getBrandId());
        }
        if (org.springframework.util.StringUtils.hasText(req.getStatus())) {
            wrapper.eq(WmsGoodsSpu::getStatus, req.getStatus());
        }
        wrapper.orderByDesc(WmsGoodsSpu::getCreateTime);
        List<WmsGoodsSpu> spus = this.list(wrapper);

        // 收集所有 categoryId / brandId / unitId，一次性查名称
        enrichSpuExtraInfo(spus);

        // 查所有 SKU，按 spuId 分组
        List<com.example.wms.business.basedata.goods.entity.WmsGoodsSku> allSkus = wmsGoodsSkuService.list(
                new LambdaQueryWrapper<com.example.wms.business.basedata.goods.entity.WmsGoodsSku>()
                        .in(com.example.wms.business.basedata.goods.entity.WmsGoodsSku::getSpuId,
                                spus.stream().map(WmsGoodsSpu::getSpuId).toList())
        );
        // 回填 SKU 的 unitName（从 SPU 带）
        java.util.Map<Long, String> spuUnitMap = spus.stream()
                .collect(Collectors.toMap(WmsGoodsSpu::getSpuId, s -> s.getUnitName() != null ? s.getUnitName() : "", (a, b) -> a));
        // 供应商名称
        Set<Long> supplierIds = allSkus.stream()
                .map(com.example.wms.business.basedata.goods.entity.WmsGoodsSku::getSupplierId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        java.util.Map<Long, String> supplierMap = new java.util.HashMap<>();
        if (!supplierIds.isEmpty()) {
            supplierMap = wmsSupplierService.listByIds(supplierIds).stream()
                    .collect(Collectors.toMap(
                            s -> s.getSupplierId(),
                            s -> s.getSupplierName() != null ? s.getSupplierName() : "",
                            (a, b) -> a));
        }

        // 查分类表，构造 id→完整名称路径 的 map
        List<WmsCategory> allCategories = wmsCategoryService.list();
        java.util.Map<Long, WmsCategory> catMap = allCategories.stream()
                .collect(Collectors.toMap(WmsCategory::getCategoryId, c -> c));
        java.util.Map<Long, String[]> pathMap = new java.util.HashMap<>();
        int maxDepth = 0;
        for (WmsCategory c : allCategories) {
            String[] path = buildCategoryPath(c, catMap);
            pathMap.put(c.getCategoryId(), path);
            if (path.length > maxDepth) maxDepth = path.length;
        }

        // 品牌名称 map（用 SPU 已回填的 brandName）
        java.util.Map<Long, WmsGoodsSpu> spuMap = spus.stream()
                .collect(Collectors.toMap(WmsGoodsSpu::getSpuId, s -> s, (a, b) -> a));

        // 展开到 SKU 维度
        List<com.example.wms.business.basedata.goods.dto.rsp.SpuExportVo> voList = new ArrayList<>();
        for (com.example.wms.business.basedata.goods.entity.WmsGoodsSku sku : allSkus) {
            WmsGoodsSpu spu = spuMap.get(sku.getSpuId());
            if (spu == null) continue;
            com.example.wms.business.basedata.goods.dto.rsp.SpuExportVo vo =
                    new com.example.wms.business.basedata.goods.dto.rsp.SpuExportVo();
            vo.setSpuCode(spu.getSpuCode());
            vo.setSpuName(spu.getSpuName());
            vo.setCategoryPath(pathMap.getOrDefault(spu.getCategoryId(), new String[maxDepth]));
            vo.setBrandName(spu.getBrandName());
            vo.setUnitName(spuUnitMap.getOrDefault(spu.getSpuId(), ""));
            vo.setOrigin(spu.getOrigin());
            vo.setAbcLevel(spu.getAbcLevel());
            vo.setStatusText("0".equals(sku.getStatus()) ? "启用" : "停用");
            vo.setCreateTime(sku.getCreateTime());

            // SKU 字段
            vo.setSkuCode(sku.getSkuCode());
            vo.setInnerCode(sku.getInnerCode());
            vo.setBarcode(sku.getBarcode());
            vo.setSpecText(sku.getSpecText());
            vo.setColor(sku.getColor());
            vo.setWeightG(sku.getWeightG());
            vo.setVolumeMl(sku.getVolumeMl());
            vo.setDefaultCost(sku.getDefaultCost());
            vo.setDefaultSale(sku.getDefaultSale());
            vo.setSupplierName(sku.getSupplierId() != null ? supplierMap.getOrDefault(sku.getSupplierId(), "") : "");
            vo.setSkuName(sku.getSkuName() != null ? sku.getSkuName() : spu.getSpuName());
            voList.add(vo);
        }

        // 如果 SKU 为空（纯 SPU 占位），也输出一行 SPU 信息
        if (allSkus.isEmpty()) {
            for (WmsGoodsSpu spu : spus) {
                com.example.wms.business.basedata.goods.dto.rsp.SpuExportVo vo =
                        new com.example.wms.business.basedata.goods.dto.rsp.SpuExportVo();
                vo.setSpuCode(spu.getSpuCode());
                vo.setSpuName(spu.getSpuName());
                vo.setCategoryPath(pathMap.getOrDefault(spu.getCategoryId(), new String[maxDepth]));
                vo.setBrandName(spu.getBrandName());
                vo.setUnitName(spu.getUnitName());
                vo.setOrigin(spu.getOrigin());
                vo.setAbcLevel(spu.getAbcLevel());
                vo.setStatusText("0".equals(spu.getStatus()) ? "启用" : "停用");
                vo.setCreateTime(spu.getCreateTime());
                voList.add(vo);
            }
        }

        // 动态表头
        List<List<String>> head = new ArrayList<>();
        head.add(java.util.Collections.singletonList("SPU编码"));
        head.add(java.util.Collections.singletonList("SPU名称"));
        head.add(java.util.Collections.singletonList("SKU编码"));
        head.add(java.util.Collections.singletonList("SKU名称"));
        head.add(java.util.Collections.singletonList("内部编码"));
        head.add(java.util.Collections.singletonList("条码"));
        for (int i = 0; i < Math.max(maxDepth, 1); i++) {
            head.add(java.util.Collections.singletonList(i == 0 ? "一级分类" : (i == 1 ? "二级分类" : (i == 2 ? "三级分类" : "分类第" + (i + 1) + "级"))));
        }
        head.add(java.util.Collections.singletonList("品牌"));
        head.add(java.util.Collections.singletonList("规格"));
        head.add(java.util.Collections.singletonList("颜色"));
        head.add(java.util.Collections.singletonList("单位"));
        head.add(java.util.Collections.singletonList("重量(g)"));
        head.add(java.util.Collections.singletonList("体积(ml)"));
        head.add(java.util.Collections.singletonList("默认成本"));
        head.add(java.util.Collections.singletonList("默认售价"));
        head.add(java.util.Collections.singletonList("供应商"));
        head.add(java.util.Collections.singletonList("产地"));
        head.add(java.util.Collections.singletonList("ABC分类"));
        head.add(java.util.Collections.singletonList("状态"));
        head.add(java.util.Collections.singletonList("创建时间"));

        // 动态列宽
        int categoryCols = Math.max(maxDepth, 1);
        List<List<Integer>> columnWidths = new ArrayList<>();
        columnWidths.add(java.util.Collections.singletonList(16)); // SPU编码
        columnWidths.add(java.util.Collections.singletonList(20)); // SPU名称
        columnWidths.add(java.util.Collections.singletonList(16)); // SKU编码
        columnWidths.add(java.util.Collections.singletonList(20)); // SKU名称
        columnWidths.add(java.util.Collections.singletonList(12)); // 内部编码
        columnWidths.add(java.util.Collections.singletonList(16)); // 条码
        for (int i = 0; i < categoryCols; i++) columnWidths.add(java.util.Collections.singletonList(14));
        columnWidths.add(java.util.Collections.singletonList(14)); // 品牌
        columnWidths.add(java.util.Collections.singletonList(16)); // 规格
        columnWidths.add(java.util.Collections.singletonList(10)); // 颜色
        columnWidths.add(java.util.Collections.singletonList(8));  // 单位
        columnWidths.add(java.util.Collections.singletonList(10)); // 重量
        columnWidths.add(java.util.Collections.singletonList(10)); // 体积
        columnWidths.add(java.util.Collections.singletonList(12)); // 默认成本
        columnWidths.add(java.util.Collections.singletonList(12)); // 默认售价
        columnWidths.add(java.util.Collections.singletonList(14)); // 供应商
        columnWidths.add(java.util.Collections.singletonList(12)); // 产地
        columnWidths.add(java.util.Collections.singletonList(10)); // ABC
        columnWidths.add(java.util.Collections.singletonList(8));  // 状态
        columnWidths.add(java.util.Collections.singletonList(18)); // 创建时间

        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = java.net.URLEncoder.encode(
                    "商品列表_" + java.time.LocalDate.now().toString() + ".xlsx",
                    java.nio.charset.StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName);

            List<List<Object>> dataRows = new ArrayList<>();
            for (com.example.wms.business.basedata.goods.dto.rsp.SpuExportVo v : voList) {
                List<Object> row = new ArrayList<>();
                row.add(v.getSpuCode());
                row.add(v.getSpuName());
                row.add(v.getSkuCode());
                row.add(v.getSkuName());
                row.add(v.getInnerCode());
                row.add(v.getBarcode());
                String[] path = v.getCategoryPath() != null ? v.getCategoryPath() : new String[categoryCols];
                for (int i = 0; i < categoryCols; i++) row.add(i < path.length ? path[i] : "");
                row.add(v.getBrandName());
                row.add(v.getSpecText());
                row.add(v.getColor());
                row.add(v.getUnitName());
                row.add(v.getWeightG());
                row.add(v.getVolumeMl());
                row.add(v.getDefaultCost());
                row.add(v.getDefaultSale());
                row.add(v.getSupplierName());
                row.add(v.getOrigin());
                row.add(v.getAbcLevel());
                row.add(v.getStatusText());
                row.add(v.getCreateTime());
                dataRows.add(row);
            }

            com.alibaba.excel.EasyExcel.write(response.getOutputStream())
                    .sheet("商品列表")
                    .head(head)
                    .registerWriteHandler(new CategoryWidthHandler(columnWidths))
                    .doWrite(dataRows);
        } catch (java.io.IOException e) {
            throw new RuntimeException("导出商品列表失败: " + e.getMessage(), e);
        }
    }

    /** 从 ancestors + 自己 categoryId 构造从根到叶子的分类名称路径 */
    private String[] buildCategoryPath(WmsCategory current, java.util.Map<Long, WmsCategory> catMap) {
        java.util.List<String> names = new ArrayList<>();
        String ancestors = current.getAncestors();
        if (ancestors != null && !ancestors.isBlank()) {
            for (String idStr : ancestors.split(",")) {
                if ("0".equals(idStr)) continue;
                try {
                    WmsCategory c = catMap.get(Long.parseLong(idStr.trim()));
                    if (c != null && c.getCategoryName() != null) names.add(c.getCategoryName());
                } catch (NumberFormatException ignored) {}
            }
        }
        if (current.getCategoryName() != null) names.add(current.getCategoryName());
        return names.toArray(new String[0]);
    }

    /** 按列号设置固定列宽的 EasyExcel 自定义处理器 */
    private static class CategoryWidthHandler implements com.alibaba.excel.write.handler.SheetWriteHandler {
        private final List<List<Integer>> columnWidths;
        CategoryWidthHandler(List<List<Integer>> columnWidths) { this.columnWidths = columnWidths; }
        @Override
        public void afterSheetCreate(com.alibaba.excel.write.handler.context.SheetWriteHandlerContext context) {
            if (context.getWriteSheetHolder() == null) return;
            org.apache.poi.ss.usermodel.Sheet sheet = context.getWriteSheetHolder().getSheet();
            int totalCols = columnWidths.size();
            for (int i = 0; i < totalCols; i++) {
                int width = 14;
                List<Integer> ws = columnWidths.get(i);
                if (ws != null && !ws.isEmpty()) width = ws.get(0);
                // EasyExcel 的宽度单位是 1/256 字符
                sheet.setColumnWidth(i, width * 256);
            }
        }
    }
}
