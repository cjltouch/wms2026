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
 * 出库确认库存处理器
 * <p>
 * 出库单审核通过时调用，按 SKU + 仓库 + 批次扣减库存数量。
 * 库存不足或记录不存在时抛出业务异常，事务回滚。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class StockOutConfirmInventoryHandler implements InventoryChangeHandler {

    private final WmsInventoryMapper inventoryMapper;
    private final WmsInventoryLogMapper logMapper;

    @Override
    public ChangeType type() {
        return ChangeType.STOCK_OUT_CONFIRM;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handle(Long billId, String billNo, LocalDateTime billTime, List<? extends ChangeItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        LocalDateTime now = billTime != null ? billTime : LocalDateTime.now();
        for (ChangeItem item : items) {
            Integer qty = item.getQty() != null ? item.getQty() : 0;
            if (qty == null || qty <= 0) {
                continue;
            }
            Long locationId = item.getLocationId();
            // 批次空串/纯空格统一归一为 null，避免“无批次”在库里同时出现 '' 和 NULL 导致扣减定位失败
            String batchNo = item.getBatchNo() == null || item.getBatchNo().trim().isEmpty()
                    ? null : item.getBatchNo().trim();
            WmsInventory inv = inventoryMapper.selectOne(new LambdaQueryWrapper<WmsInventory>()
                    .eq(WmsInventory::getWarehouseId, item.getWarehouseId())
                    .eq(WmsInventory::getSkuId, item.getSkuId())
                    .eq(locationId != null, WmsInventory::getLocationId, locationId)
                    .isNull(locationId == null, WmsInventory::getLocationId)
                    .eq(batchNo != null, WmsInventory::getBatchNo, batchNo)
                    .isNull(batchNo == null, WmsInventory::getBatchNo));
            int beforeQty = inv != null && inv.getQuantity() != null ? inv.getQuantity() : 0;
            int afterQty = beforeQty - qty;

            int updated = inventoryMapper.deductStock(
                    item.getWarehouseId(),
                    item.getSkuId(),
                    locationId,
                    batchNo,
                    qty,
                    now);
            if (updated == 0) {
                throw new BizException("库存不足或库存记录不存在");
            }

            WmsInventoryLog log = new WmsInventoryLog();
            log.setLogId(SnowflakeId.getInstance().nextId());
            log.setBillId(billId);
            log.setBillNo(billNo);
            log.setBillTypeCode(type().getCode());
            log.setItemId(inv != null ? inv.getInventoryId() : null);
            log.setWarehouseId(item.getWarehouseId());
            log.setSkuId(item.getSkuId());
            log.setLocationId(locationId);
            log.setBatchNo(batchNo);
            log.setDirection(-1);
            log.setQtyChange(-qty);
            log.setUnitPrice(item.getCostPrice());
            log.setAmountChange((item.getCostPrice() != null ? item.getCostPrice() : BigDecimal.ZERO)
                    .multiply(BigDecimal.valueOf(qty)));
            log.setOperateBy(AuthContextHolder.getUserId());
            log.setOperateTime(now);
            log.setBeforeQty(beforeQty);
            log.setAfterQty(afterQty);
            log.setRemark(item.getRemark());
            log.setInnerCode(item.getInnerCode());
            logMapper.insert(log);
        }
    }
}
