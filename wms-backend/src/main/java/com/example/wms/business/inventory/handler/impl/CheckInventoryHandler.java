package com.example.wms.business.inventory.handler.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.wms.auth.AuthContextHolder;
import com.example.wms.business.inventory.entity.WmsInventory;
import com.example.wms.business.inventory.entity.WmsInventoryLog;
import com.example.wms.business.inventory.handler.ChangeItem;
import com.example.wms.business.inventory.handler.ChangeType;
import com.example.wms.business.inventory.handler.InventoryChangeHandler;
import com.example.wms.business.inventory.mapper.WmsInventoryLogMapper;
import com.example.wms.business.inventory.mapper.WmsInventoryMapper;
import com.example.wms.common.ResultCode;
import com.example.wms.common.exception.BizException;
import com.example.wms.common.utils.SnowflakeId;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class CheckInventoryHandler implements InventoryChangeHandler {

    private final WmsInventoryMapper inventoryMapper;
    private final WmsInventoryLogMapper logMapper;

    @Override
    public ChangeType type() {
        return ChangeType.CHECK;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handle(Long billId, String billNo, LocalDateTime billTime, List<? extends ChangeItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (ChangeItem item : items) {
            Integer qty = item.getQty() != null ? item.getQty() : 0;
            if (qty == 0) {
                continue;
            }
            Long locationId = item.getLocationId();
            // 批次空串/纯空格统一归一为 null，避免“无批次”在库里同时出现 '' 和 NULL 导致重复建库存行
            String batchNo = item.getBatchNo() == null || item.getBatchNo().trim().isEmpty()
                    ? null : item.getBatchNo().trim();
            // 先做精确匹配：locationId/batchNo 为 null 时匹配 DB 的 NULL（而不是 0/空串）
            WmsInventory inv = inventoryMapper.selectOne(new LambdaQueryWrapper<WmsInventory>()
                    .eq(WmsInventory::getWarehouseId, item.getWarehouseId())
                    .eq(WmsInventory::getSkuId, item.getSkuId())
                    .eq(locationId != null, WmsInventory::getLocationId, locationId)
                    .isNull(locationId == null, WmsInventory::getLocationId)
                    .eq(batchNo != null && !batchNo.isEmpty(), WmsInventory::getBatchNo, batchNo)
                    .isNull(batchNo == null || batchNo.isEmpty(), WmsInventory::getBatchNo));
            // 如果精确匹配不到（扣减场景常见原因：前端没传 location/batch，但库存里有默认值），
            // 就退化到 仓库+SKU 下取 availableQty 最大的一条，保证总库存够就允许扣减
            if (inv == null && qty < 0) {
                List<WmsInventory> cands = inventoryMapper.selectList(new LambdaQueryWrapper<WmsInventory>()
                        .eq(WmsInventory::getWarehouseId, item.getWarehouseId())
                        .eq(WmsInventory::getSkuId, item.getSkuId()));
                if (cands != null && !cands.isEmpty()) {
                    inv = cands.stream()
                            .filter(c -> c.getAvailableQty() != null && c.getAvailableQty() + qty >= 0)
                            .max((a, b) -> Integer.compare(a.getAvailableQty() == null ? 0 : a.getAvailableQty(),
                                    b.getAvailableQty() == null ? 0 : b.getAvailableQty()))
                            .orElse(cands.get(0));
                }
            }

            BigDecimal price = item.getCostPrice() != null ? item.getCostPrice() : BigDecimal.ZERO;
            BigDecimal amountChange = price.multiply(BigDecimal.valueOf(qty));
            int beforeQty;
            int afterQty;
            Long inventoryId;

            if (inv == null) {
                if (qty < 0) {
                    throw new BizException(ResultCode.STOCK_INSUFFICIENT);
                }
                inv = new WmsInventory();
                inv.setInventoryId(SnowflakeId.getInstance().nextId());
                inv.setWarehouseId(item.getWarehouseId());
                inv.setSkuId(item.getSkuId());
                inv.setLocationId(locationId);
                inv.setBatchNo(batchNo);
                inv.setQuantity(qty);
                inv.setLockedQty(0);
                inv.setAvailableQty(qty);
                inv.setCostPrice(price);
                inv.setTotalAmount(amountChange);
                inv.setLastInTime(billTime);
                inventoryMapper.insert(inv);
                beforeQty = 0;
                afterQty = qty;
                inventoryId = inv.getInventoryId();
            } else {
                beforeQty = inv.getQuantity() != null ? inv.getQuantity() : 0;
                int oldVersion = inv.getVersion() != null ? inv.getVersion() : 0;
                int updated;
                if (qty > 0) {
                    updated = inventoryMapper.addStock(inv.getInventoryId(), qty, amountChange, billTime, oldVersion);
                } else {
                    updated = inventoryMapper.adjustStock(inv.getInventoryId(), qty, amountChange, billTime, oldVersion);
                }
                if (updated == 0) {
                    WmsInventory latest = inventoryMapper.selectById(inv.getInventoryId());
                    int latestAvailable = latest != null && latest.getAvailableQty() != null ? latest.getAvailableQty() : 0;
                    if (latestAvailable + qty < 0) {
                        throw new BizException(ResultCode.STOCK_INSUFFICIENT);
                    }
                    throw new BizException(ResultCode.OPTIMISTIC_LOCK_CONFLICT);
                }
                afterQty = beforeQty + qty;
                inventoryId = inv.getInventoryId();
            }

            WmsInventoryLog log = new WmsInventoryLog();
            log.setLogId(SnowflakeId.getInstance().nextId());
            log.setBillId(billId);
            log.setBillNo(billNo);
            log.setBillTypeCode(type().getCode());
            log.setItemId(inventoryId);
            log.setWarehouseId(item.getWarehouseId());
            log.setSkuId(item.getSkuId());
            log.setLocationId(locationId);
            log.setBatchNo(batchNo);
            log.setDirection(qty > 0 ? 1 : -1);
            log.setQtyChange(qty);
            log.setUnitPrice(price);
            log.setAmountChange(amountChange);
            log.setOperateBy(AuthContextHolder.getUserId());
            log.setOperateTime(billTime);
            log.setBeforeQty(beforeQty);
            log.setAfterQty(afterQty);
            log.setRemark(item.getRemark());
            log.setInnerCode(item.getInnerCode());
            logMapper.insert(log);
        }
    }
}
