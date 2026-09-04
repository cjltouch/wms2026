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

/**
 * 调拨入库库存处理器
 * <p>
 * 调拨单确认入库时调用，向调入仓库增加库存。
 * 如果调入仓库不存在该 SKU 的库存记录，则自动新建。
 * 调拨完整流程：确认出库(TransferOutHandler扣减) → 确认入库(TransferInHandler增加)。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class TransferInInventoryHandler implements InventoryChangeHandler {

    private final WmsInventoryMapper inventoryMapper;
    private final WmsInventoryLogMapper logMapper;

    @Override
    public ChangeType type() {
        return ChangeType.TRANSFER_IN;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handle(Long billId, String billNo, LocalDateTime billTime, List<? extends ChangeItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (ChangeItem item : items) {
            Long locationId = item.getLocationId();
            // 批次空串/纯空格统一归一为 null：查询用 IS NULL、新建存 NULL、流水记 NULL，
            // 避免“无批次”在库里同时出现 '' 和 NULL 两种值导致同仓库+SKU重复建库存行
            String batchNo = item.getBatchNo() == null || item.getBatchNo().trim().isEmpty()
                    ? null : item.getBatchNo().trim();
            // 调拨入库匹配：locationId/batchNo 为 null 时匹配 DB 的 NULL（不是 0/空串）
            WmsInventory inv = inventoryMapper.selectOne(new LambdaQueryWrapper<WmsInventory>()
                    .eq(WmsInventory::getWarehouseId, item.getWarehouseId())
                    .eq(WmsInventory::getSkuId, item.getSkuId())
                    .eq(locationId != null, WmsInventory::getLocationId, locationId)
                    .isNull(locationId == null, WmsInventory::getLocationId)
                    .eq(batchNo != null && !batchNo.isEmpty(), WmsInventory::getBatchNo, batchNo)
                    .isNull(batchNo == null || batchNo.isEmpty(), WmsInventory::getBatchNo));

            int beforeQty;
            int afterQty;
            Long inventoryId;
            int qty = item.getQty() != null ? item.getQty() : 0;
            BigDecimal price = item.getCostPrice() != null ? item.getCostPrice() : BigDecimal.ZERO;
            BigDecimal addAmount = price.multiply(BigDecimal.valueOf(qty));

            if (inv == null) {
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
                inv.setTotalAmount(addAmount);
                inv.setLastInTime(billTime);
                inventoryMapper.insert(inv);
                beforeQty = 0;
                afterQty = qty;
                inventoryId = inv.getInventoryId();
            } else {
                beforeQty = inv.getQuantity() != null ? inv.getQuantity() : 0;
                afterQty = beforeQty + qty;
                int oldVersion = inv.getVersion() != null ? inv.getVersion() : 0;
                int updated = inventoryMapper.addStock(inv.getInventoryId(), qty, addAmount, billTime, oldVersion);
                if (updated == 0) {
                    throw new BizException(ResultCode.OPTIMISTIC_LOCK_CONFLICT);
                }
                inventoryId = inv.getInventoryId();
            }

            if (afterQty < 0) {
                throw new BizException(ResultCode.STOCK_INSUFFICIENT);
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
            log.setDirection(1);
            log.setQtyChange(qty);
            log.setUnitPrice(price);
            log.setAmountChange(addAmount);
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
