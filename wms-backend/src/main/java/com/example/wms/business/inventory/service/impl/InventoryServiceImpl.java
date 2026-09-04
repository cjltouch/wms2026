package com.example.wms.business.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.wms.business.basedata.goods.entity.WmsGoodsSku;
import com.example.wms.business.basedata.goods.service.WmsGoodsSkuService;
import com.example.wms.business.basedata.warehouse.entity.WmsWarehouse;
import com.example.wms.business.basedata.warehouse.service.WmsWarehouseService;
import com.example.wms.business.inventory.dto.req.InventoryPageReq;
import com.example.wms.business.inventory.entity.WmsInventory;
import com.example.wms.business.inventory.mapper.WmsInventoryMapper;
import com.example.wms.business.inventory.service.InventoryService;
import com.example.wms.common.PageRsp;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InventoryServiceImpl extends ServiceImpl<WmsInventoryMapper, WmsInventory> implements InventoryService {

    @Autowired
    private WmsGoodsSkuService skuService;

    @Autowired
    private WmsWarehouseService warehouseService;

    @Autowired
    private com.example.wms.business.common.SkuMasterDataLoader skuMasterDataLoader;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public PageRsp<WmsInventory> pageInventory(InventoryPageReq req) {
        LambdaQueryWrapper<WmsInventory> wrapper = buildQueryWrapper(req);
        wrapper.orderByDesc(WmsInventory::getCreateTime);
        Page<WmsInventory> page = this.page(new Page<>(req.getPageNum(), req.getPageSize()), wrapper);
        List<WmsInventory> records = enrichNames(page.getRecords());
        return new PageRsp<>(page.getTotal(), records, req.getPageNum(), req.getPageSize());
    }

    @Override
    public void exportInventory(InventoryPageReq req, HttpServletResponse response) {
        LambdaQueryWrapper<WmsInventory> wrapper = buildQueryWrapper(req);
        wrapper.orderByDesc(WmsInventory::getCreateTime);
        List<WmsInventory> records = enrichNames(this.list(wrapper));
        java.time.format.DateTimeFormatter dateFmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<com.example.wms.business.inventory.dto.rsp.InventoryExportVo> voList = records.stream().map(inv -> {
            com.example.wms.business.inventory.dto.rsp.InventoryExportVo vo =
                    new com.example.wms.business.inventory.dto.rsp.InventoryExportVo();
            vo.setWarehouseName(inv.getWarehouseName());
            vo.setSupplierName(inv.getSupplierName());
            vo.setSkuCode(inv.getSkuCode());
            vo.setInnerCode(inv.getInnerCode());
            vo.setSkuName(inv.getSkuName());
            vo.setSpecText(inv.getSpecText());
            vo.setUnitName(inv.getUnitName());
            vo.setBatchNo(inv.getBatchNo());
            vo.setQuantity(inv.getQuantity());
            vo.setLockedQty(inv.getLockedQty());
            vo.setAvailableQty(inv.getAvailableQty());
            vo.setCostPrice(inv.getCostPrice());
            vo.setTotalAmount(inv.getTotalAmount());
            vo.setProduceDate(inv.getProduceDate() != null ? inv.getProduceDate().format(dateFmt) : "");
            vo.setExpireDate(inv.getExpireDate() != null ? inv.getExpireDate().format(dateFmt) : "");
            return vo;
        }).collect(Collectors.toList());
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = java.net.URLEncoder.encode(
                    "库存查询_" + LocalDate.now().format(dateFmt) + ".xlsx", java.nio.charset.StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName);
            com.alibaba.excel.EasyExcel.write(response.getOutputStream(),
                    com.example.wms.business.inventory.dto.rsp.InventoryExportVo.class)
                    .sheet("库存查询")
                    .doWrite(voList);
        } catch (java.io.IOException e) {
            throw new RuntimeException("导出库存Excel失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<WmsInventory> listBySkuId(Long skuId) {
        return this.lambdaQuery()
                .eq(WmsInventory::getSkuId, skuId)
                .list();
    }

    @Override
    public PageRsp<WmsInventory> pageByBatch(InventoryPageReq req) {
        LambdaQueryWrapper<WmsInventory> wrapper = buildQueryWrapper(req);
        wrapper.groupBy(WmsInventory::getWarehouseId, WmsInventory::getSkuId, WmsInventory::getBatchNo);
        wrapper.orderByDesc(WmsInventory::getCreateTime);
        Page<WmsInventory> page = this.page(new Page<>(req.getPageNum(), req.getPageSize()), wrapper);
        List<WmsInventory> records = enrichNames(page.getRecords());
        return new PageRsp<>(page.getTotal(), records, req.getPageNum(), req.getPageSize());
    }

    @Override
    public PageRsp<WmsInventory> warningExpire(InventoryPageReq req, Integer thresholdDays) {
        LambdaQueryWrapper<WmsInventory> wrapper = buildQueryWrapper(req);
        LocalDate thresholdDate = LocalDate.now().plusDays(thresholdDays != null ? thresholdDays : 30);
        wrapper.isNotNull(WmsInventory::getExpireDate)
                .le(WmsInventory::getExpireDate, thresholdDate)
                .gt(WmsInventory::getQuantity, 0);
        wrapper.orderByAsc(WmsInventory::getExpireDate);
        Page<WmsInventory> page = this.page(new Page<>(req.getPageNum(), req.getPageSize()), wrapper);
        List<WmsInventory> records = enrichNames(page.getRecords());
        return new PageRsp<>(page.getTotal(), records, req.getPageNum(), req.getPageSize());
    }

    @Override
    public PageRsp<WmsInventory> warningBelowMin(InventoryPageReq req) {
        LambdaQueryWrapper<WmsInventory> wrapper = buildQueryWrapper(req);
        wrapper.gt(WmsInventory::getQuantity, 0);
        wrapper.apply("available_qty < (SELECT min_stock FROM wms_goods_sku s WHERE s.sku_id = wms_inventory.sku_id)");
        Page<WmsInventory> page = this.page(new Page<>(req.getPageNum(), req.getPageSize()), wrapper);
        List<WmsInventory> records = enrichNames(page.getRecords());
        return new PageRsp<>(page.getTotal(), records, req.getPageNum(), req.getPageSize());
    }

    @Override
    public PageRsp<Map<String, Object>> turnoverAnalytics(InventoryPageReq req) {
        LambdaQueryWrapper<WmsInventory> wrapper = buildQueryWrapper(req);
        wrapper.gt(WmsInventory::getQuantity, 0);
        wrapper.orderByDesc(WmsInventory::getLastOutTime);
        Page<WmsInventory> page = this.page(new Page<>(req.getPageNum(), req.getPageSize()), wrapper);
        List<WmsInventory> enriched = enrichNames(page.getRecords());
        List<Map<String, Object>> result = enriched.stream().map(inv -> {
            Map<String, Object> map = new HashMap<>();
            map.put("inventoryId", inv.getInventoryId());
            map.put("warehouseId", inv.getWarehouseId());
            map.put("warehouseName", inv.getWarehouseName());
            map.put("skuId", inv.getSkuId());
            map.put("skuCode", inv.getSkuCode());
            map.put("skuName", inv.getSkuName());
            map.put("batchNo", inv.getBatchNo());
            map.put("quantity", inv.getQuantity());
            map.put("availableQty", inv.getAvailableQty());
            map.put("costPrice", inv.getCostPrice());
            map.put("totalAmount", inv.getTotalAmount());
            map.put("lastInTime", inv.getLastInTime());
            map.put("lastOutTime", inv.getLastOutTime());
            long days = inv.getLastOutTime() != null
                    ? java.time.Duration.between(inv.getLastOutTime(), LocalDateTime.now()).toDays()
                    : (inv.getLastInTime() != null
                    ? java.time.Duration.between(inv.getLastInTime(), LocalDateTime.now()).toDays()
                    : 0L);
            map.put("turnoverDays", days);
            return map;
        }).toList();
        return new PageRsp<>(page.getTotal(), result, req.getPageNum(), req.getPageSize());
    }

    /**
     * 根据前端 req 条件构建 QueryWrapper，支持 keyword（SKU编码/SKU名称模糊）
     * 和日期范围（startDate/endDate 过滤 lastInTime/lastOutTime/createTime）
     */
    private LambdaQueryWrapper<WmsInventory> buildQueryWrapper(InventoryPageReq req) {
        LambdaQueryWrapper<WmsInventory> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(req.getKeyword())) {
            // 先查匹配 keyword 的 skuId 列表，再按 skuId 过滤
            List<Long> skuIds = skuService.lambdaQuery()
                    .and(w -> w.like(WmsGoodsSku::getSkuCode, req.getKeyword())
                            .or().like(WmsGoodsSku::getSkuName, req.getKeyword()))
                    .list()
                    .stream().map(WmsGoodsSku::getSkuId)
                    .toList();
            if (skuIds.isEmpty()) {
                // 不匹配直接返回空结果
                wrapper.apply("1 = 0");
                return wrapper;
            }
            wrapper.in(WmsInventory::getSkuId, skuIds);
        }
        if (req.getWarehouseId() != null) {
            wrapper.eq(WmsInventory::getWarehouseId, req.getWarehouseId());
        }
        if (req.getSkuId() != null) {
            wrapper.eq(WmsInventory::getSkuId, req.getSkuId());
        }
        if (req.getLocationId() != null) {
            wrapper.eq(WmsInventory::getLocationId, req.getLocationId());
        }
        if (StringUtils.hasText(req.getBatchNo())) {
            wrapper.like(WmsInventory::getBatchNo, req.getBatchNo());
        }
        if (req.getAvailableOnly() != null && req.getAvailableOnly() == 1) {
            wrapper.gt(WmsInventory::getAvailableQty, 0);
        }
        if (StringUtils.hasText(req.getStartDate())) {
            try {
                LocalDateTime start = LocalDate.parse(req.getStartDate(), DATE_FMT).atStartOfDay();
                wrapper.and(w -> w.ge(WmsInventory::getLastInTime, start)
                        .or().ge(WmsInventory::getLastOutTime, start)
                        .or().ge(WmsInventory::getCreateTime, start));
            } catch (Exception ignored) { /* 日期格式错忽略 */ }
        }
        if (StringUtils.hasText(req.getEndDate())) {
            try {
                LocalDateTime end = LocalDate.parse(req.getEndDate(), DATE_FMT).atTime(LocalTime.MAX);
                wrapper.and(w -> w.le(WmsInventory::getLastInTime, end)
                        .or().le(WmsInventory::getLastOutTime, end)
                        .or().le(WmsInventory::getCreateTime, end));
            } catch (Exception ignored) { /* 日期格式错忽略 */ }
        }
        return wrapper;
    }

    /**
     * 批量为库存记录填充仓库名称/SKU信息（skuCode/skuName/specText/warehouseName），
     * 所有名称字段使用 @TableField(exist=false) 定义在 WmsInventory 实体中。
     */
    private List<WmsInventory> enrichNames(List<WmsInventory> records) {
        if (records == null || records.isEmpty()) return records;
        Set<Long> skuIds = records.stream().map(WmsInventory::getSkuId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> warehouseIds = records.stream().map(WmsInventory::getWarehouseId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, WmsGoodsSku> skuMap = skuIds.isEmpty()
                ? Collections.emptyMap()
                : skuService.listByIds(skuIds).stream().collect(Collectors.toMap(WmsGoodsSku::getSkuId, Function.identity(), (a, b) -> a));
        Map<Long, WmsWarehouse> whMap = warehouseIds.isEmpty()
                ? Collections.emptyMap()
                : warehouseService.listByIds(warehouseIds).stream().collect(Collectors.toMap(WmsWarehouse::getWarehouseId, Function.identity(), (a, b) -> a));
        Map<Long, com.example.wms.business.common.SkuMasterDataLoader.SkuMaster> masterMap =
                skuMasterDataLoader.load(skuIds);
        for (WmsInventory inv : records) {
            WmsGoodsSku sku = skuMap.get(inv.getSkuId());
            if (sku != null) {
                inv.setSkuCode(sku.getSkuCode());
                inv.setSkuName(sku.getSkuName());
                inv.setInnerCode(sku.getInnerCode() != null ? sku.getInnerCode().trim() : null);
                inv.setSpecText(sku.getSpecText() != null ? sku.getSpecText().trim() : null);
            }
            WmsWarehouse wh = whMap.get(inv.getWarehouseId());
            if (wh != null) {
                inv.setWarehouseName(wh.getWarehouseName());
            }
            com.example.wms.business.common.SkuMasterDataLoader.SkuMaster md = masterMap.get(inv.getSkuId());
            if (md != null) {
                inv.setSupplierName(md.supplierName);
                inv.setUnitName(md.unitName);
            }
        }
        return records;
    }
}
