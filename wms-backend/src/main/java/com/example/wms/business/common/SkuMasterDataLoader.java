package com.example.wms.business.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.wms.business.basedata.goods.entity.WmsGoodsSku;
import com.example.wms.business.basedata.goods.entity.WmsGoodsSpu;
import com.example.wms.business.basedata.goods.mapper.WmsGoodsSkuMapper;
import com.example.wms.business.basedata.goods.mapper.WmsGoodsSpuMapper;
import com.example.wms.business.basedata.supplier.entity.WmsSupplier;
import com.example.wms.business.basedata.supplier.mapper.WmsSupplierMapper;
import com.example.wms.business.basedata.unit.entity.WmsUnit;
import com.example.wms.business.basedata.unit.mapper.WmsUnitMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 单据明细商品主数据加载器
 * 按 SKU 批量加载规格、商品名、计量单位、供应商名称，用于明细行落库字段缺失时的展示补齐
 */
@Component
@RequiredArgsConstructor
public class SkuMasterDataLoader {

    private final WmsGoodsSkuMapper skuMapper;
    private final WmsGoodsSpuMapper spuMapper;
    private final WmsUnitMapper unitMapper;
    private final WmsSupplierMapper supplierMapper;

    /**
     * 商品主数据视图
     */
    public static class SkuMaster {
        /** 规格描述 */
        public String specText;
        /** 商品名称 */
        public String skuName;
        /** 计量单位名称 */
        public String unitName;
        /** 供应商名称 */
        public String supplierName;
    }

    /**
     * 按 SKU ID 集合批量加载主数据
     *
     * @param skuIds SKU ID 集合
     * @return skuId → 主数据
     */
    public Map<Long, SkuMaster> load(Collection<Long> skuIds) {
        List<Long> distinct = skuIds == null ? Collections.emptyList()
                : skuIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (distinct.isEmpty()) {
            return new HashMap<>();
        }
        List<WmsGoodsSku> skus = skuMapper.selectList(
                new LambdaQueryWrapper<WmsGoodsSku>().in(WmsGoodsSku::getSkuId, distinct));
        if (CollectionUtils.isEmpty(skus)) {
            return new HashMap<>();
        }

        // SPU（取商品名、单位）
        List<Long> spuIds = skus.stream().map(WmsGoodsSku::getSpuId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, WmsGoodsSpu> spuMap = spuIds.isEmpty() ? new HashMap<>()
                : spuMapper.selectList(new LambdaQueryWrapper<WmsGoodsSpu>().in(WmsGoodsSpu::getSpuId, spuIds))
                .stream().collect(Collectors.toMap(WmsGoodsSpu::getSpuId, s -> s, (a, b) -> a));

        // 计量单位
        List<Long> unitIds = spuMap.values().stream().map(WmsGoodsSpu::getUnitId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> unitMap = unitIds.isEmpty() ? new HashMap<>()
                : unitMapper.selectList(new LambdaQueryWrapper<WmsUnit>().in(WmsUnit::getUnitId, unitIds))
                .stream().collect(Collectors.toMap(WmsUnit::getUnitId, WmsUnit::getUnitName, (a, b) -> a));

        // 供应商
        List<Long> supplierIds = skus.stream().map(WmsGoodsSku::getSupplierId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> supplierMap = supplierIds.isEmpty() ? new HashMap<>()
                : supplierMapper.selectList(new LambdaQueryWrapper<WmsSupplier>().in(WmsSupplier::getSupplierId, supplierIds))
                .stream().collect(Collectors.toMap(WmsSupplier::getSupplierId, WmsSupplier::getSupplierName, (a, b) -> a));

        Map<Long, SkuMaster> result = new HashMap<>();
        for (WmsGoodsSku sku : skus) {
            SkuMaster m = new SkuMaster();
            m.specText = sku.getSpecText() != null ? sku.getSpecText().trim() : null;
            if (!StringUtils.hasText(m.specText)) {
                m.specText = null;
            }
            m.skuName = StringUtils.hasText(sku.getSkuName()) ? sku.getSkuName().trim() : null;
            WmsGoodsSpu spu = sku.getSpuId() != null ? spuMap.get(sku.getSpuId()) : null;
            if (spu != null) {
                if (!StringUtils.hasText(m.skuName)) {
                    m.skuName = spu.getSpuName();
                }
                if (spu.getUnitId() != null) {
                    m.unitName = unitMap.get(spu.getUnitId());
                }
            }
            if (sku.getSupplierId() != null) {
                m.supplierName = supplierMap.get(sku.getSupplierId());
            }
            result.put(sku.getSkuId(), m);
        }
        return result;
    }
}
