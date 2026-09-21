package com.example.wms.business.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.wms.business.basedata.goods.entity.WmsGoodsSku;
import com.example.wms.business.basedata.goods.service.WmsGoodsSkuService;
import com.example.wms.business.basedata.warehouse.entity.WmsWarehouse;
import com.example.wms.business.basedata.warehouse.service.WmsWarehouseService;
import com.example.wms.business.inventory.dto.req.InventoryLogPageReq;
import com.example.wms.business.inventory.entity.WmsInventoryLog;
import com.example.wms.business.inventory.mapper.WmsInventoryLogMapper;
import com.example.wms.business.inventory.service.InventoryLogService;
import com.example.wms.common.PageRsp;
import com.example.wms.system.entity.SysUser;
import com.example.wms.system.service.SysUserService;
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
public class InventoryLogServiceImpl extends ServiceImpl<WmsInventoryLogMapper, WmsInventoryLog> implements InventoryLogService {

    @Autowired
    private WmsGoodsSkuService skuService;

    @Autowired
    private WmsWarehouseService warehouseService;

    @Autowired
    private SysUserService sysUserService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * DB bill_type (varchar) -> 前端中文展示，保持与前端 billTypeOptions 的 value 字符串一致
     */
    private static final Map<String, String> BILL_TYPE_NAME;
    static {
        Map<String, String> m = new HashMap<>();
        m.put("STOCK_IN", "入库单");
        m.put("STOCK_OUT", "出库单");
        m.put("STOCK_OUT_LOCK", "出库锁定");
        m.put("PURCHASE_RETURN", "采购退货");
        m.put("TRANSFER_OUT", "调拨出库");
        m.put("TRANSFER_IN", "调拨入库");
        m.put("LOSS", "报损单");
        m.put("CHECK", "盘点单");
        m.put("PURCHASE", "采购单");
        m.put("SALE", "销售单");
        m.put("TRANSFER", "调拨单");
        BILL_TYPE_NAME = Collections.unmodifiableMap(m);
    }

    @Override
    public PageRsp<WmsInventoryLog> pageLog(InventoryLogPageReq req) {
        LambdaQueryWrapper<WmsInventoryLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(req.getBillNo())) {
            wrapper.like(WmsInventoryLog::getBillNo, req.getBillNo());
        }
        // billType 前端可能传两种：
        //  - 字符串（直接等于 DB bill_type）：STOCK_IN / STOCK_OUT 等
        //  - 数字（兼容老前端，用 ChangeType.code 对应枚举映射）
        Object billTypeRaw = req.getBillType();
        if (billTypeRaw != null) {
            String s = String.valueOf(billTypeRaw);
            if (StringUtils.hasText(s) && !"0".equals(s)) {
                if (s.matches("\\d+")) {
                    // 数字→通过 ChangeType 或 我们的映射表反向查找字符串值
                    // 方案：直接尝试用字符串本身匹配，若不匹配再按常见数字→字符串 fallback
                    switch (s) {
                        case "1" -> wrapper.eq(WmsInventoryLog::getBillType, "PURCHASE_RETURN");
                        case "2" -> wrapper.eq(WmsInventoryLog::getBillType, "STOCK_IN");
                        case "3" -> wrapper.eq(WmsInventoryLog::getBillType, "STOCK_OUT_LOCK");
                        case "4" -> wrapper.eq(WmsInventoryLog::getBillType, "STOCK_OUT");
                        case "5" -> wrapper.eq(WmsInventoryLog::getBillType, "TRANSFER_OUT");
                        case "6" -> wrapper.eq(WmsInventoryLog::getBillType, "TRANSFER_IN");
                        case "7" -> wrapper.eq(WmsInventoryLog::getBillType, "LOSS");
                        case "8" -> wrapper.eq(WmsInventoryLog::getBillType, "CHECK");
                        default -> wrapper.eq(WmsInventoryLog::getBillType, s);
                    }
                } else {
                    wrapper.eq(WmsInventoryLog::getBillType, s);
                }
            }
        }
        // direction（变动方向）：前端 1=入 或 -1=出，结合 changeQty 的正负判断
        if (req.getDirection() != null) {
            if (req.getDirection() > 0) {
                wrapper.gt(WmsInventoryLog::getChangeQty, 0);
            } else if (req.getDirection() < 0) {
                wrapper.lt(WmsInventoryLog::getChangeQty, 0);
            }
        }
        if (req.getWarehouseId() != null) {
            wrapper.eq(WmsInventoryLog::getWarehouseId, req.getWarehouseId());
        }
        if (StringUtils.hasText(req.getSkuCode())) {
            List<Long> skuIds = skuService.lambdaQuery()
                    .like(WmsGoodsSku::getSkuCode, req.getSkuCode())
                    .or()
                    .like(WmsGoodsSku::getSkuName, req.getSkuCode())
                    .list().stream().map(WmsGoodsSku::getSkuId).toList();
            if (skuIds.isEmpty()) {
                wrapper.apply("1 = 0");
            } else {
                wrapper.in(WmsInventoryLog::getSkuId, skuIds);
            }
        }
        if (req.getSkuId() != null) {
            wrapper.eq(WmsInventoryLog::getSkuId, req.getSkuId());
        }
        if (StringUtils.hasText(req.getBatchNo())) {
            wrapper.like(WmsInventoryLog::getBatchNo, req.getBatchNo());
        }
        // 时间范围：优先用 PageReq 的 dateRangeStart/dateRangeEnd（LocalDateTime），兼容 startDate/endDate（String）
        LocalDateTime startTime = req.getDateRangeStart();
        LocalDateTime endTime = req.getDateRangeEnd();
        if (startTime == null && StringUtils.hasText(req.getStartDate())) {
            try {
                startTime = LocalDate.parse(req.getStartDate(), DATE_FMT).atStartOfDay();
            } catch (Exception ignored) { /* ignore */ }
        }
        if (endTime == null && StringUtils.hasText(req.getEndDate())) {
            try {
                endTime = LocalDate.parse(req.getEndDate(), DATE_FMT).atTime(LocalTime.MAX);
            } catch (Exception ignored) { /* ignore */ }
        }
        if (startTime != null) {
            wrapper.ge(WmsInventoryLog::getOperateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(WmsInventoryLog::getOperateTime, endTime);
        }
        wrapper.orderByDesc(WmsInventoryLog::getOperateTime);
        Page<WmsInventoryLog> page = this.page(new Page<>(req.getPageNum(), req.getPageSize()), wrapper);
        List<WmsInventoryLog> records = enrichAndAdapt(page.getRecords());
        return new PageRsp<>(page.getTotal(), records, req.getPageNum(), req.getPageSize());
    }

    /**
     * 1) 名称填充（仓库/SKU）
     * 2) 向前端兼容字段赋值：direction / qtyChange / unitPrice / amountChange / itemId / billTypeName / directionName
     */
    private List<WmsInventoryLog> enrichAndAdapt(List<WmsInventoryLog> records) {
        if (records == null || records.isEmpty()) return records;
        Set<Long> skuIds = records.stream().map(WmsInventoryLog::getSkuId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> whIds = records.stream().map(WmsInventoryLog::getWarehouseId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, WmsGoodsSku> skuMap = skuIds.isEmpty()
                ? Collections.emptyMap()
                : skuService.listByIds(skuIds).stream().collect(Collectors.toMap(WmsGoodsSku::getSkuId, Function.identity(), (a, b) -> a));
        Map<Long, WmsWarehouse> whMap = whIds.isEmpty()
                ? Collections.emptyMap()
                : warehouseService.listByIds(whIds).stream().collect(Collectors.toMap(WmsWarehouse::getWarehouseId, Function.identity(), (a, b) -> a));
        Set<Long> userIds = records.stream().map(WmsInventoryLog::getOperateBy).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, SysUser> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : sysUserService.listByIds(userIds).stream().collect(Collectors.toMap(SysUser::getUserId, Function.identity(), (a, b) -> a));
        for (WmsInventoryLog log : records) {
            WmsGoodsSku sku = skuMap.get(log.getSkuId());
            if (sku != null) {
                log.setSkuCode(sku.getSkuCode());
                log.setSkuName(sku.getSkuName());
                log.setInnerCode(sku.getInnerCode());
            }
            WmsWarehouse wh = whMap.get(log.getWarehouseId());
            if (wh != null) {
                log.setWarehouseName(wh.getWarehouseName());
            }
            // 操作人姓名：DB 已落库则直接用，否则按 operateBy JOIN sys_user 取 真实姓名(real_name) 回退填充
            if (log.getOperateName() == null || log.getOperateName().isEmpty()) {
                SysUser user = userMap.get(log.getOperateBy());
                if (user != null) {
                    if (user.getRealName() != null && !user.getRealName().isEmpty()) {
                        log.setOperateName(user.getRealName());
                    } else if (user.getNickName() != null && !user.getNickName().isEmpty()) {
                        log.setOperateName(user.getNickName());
                    } else {
                        log.setOperateName(user.getUserName());
                    }
                }
            }
            // 兼容前端字段
            log.setItemId(log.getBillItemId());
            log.setUnitPrice(log.getCostPrice());
            log.setAmountChange(log.getChangeAmount());
            int qty = log.getChangeQty() == null ? 0 : log.getChangeQty();
            if (qty > 0) {
                log.setDirection(1);
                log.setDirectionName("入库");
                log.setQtyChange(qty);
            } else if (qty < 0) {
                log.setDirection(-1);
                log.setDirectionName("出库");
                log.setQtyChange(-qty);
            } else {
                log.setDirection(0);
                log.setDirectionName("调整");
                log.setQtyChange(0);
            }
            log.setBillTypeName(BILL_TYPE_NAME.getOrDefault(log.getBillType(), (log.getBillType() == null ? "-" : log.getBillType())));
        }
        return records;
    }
}
