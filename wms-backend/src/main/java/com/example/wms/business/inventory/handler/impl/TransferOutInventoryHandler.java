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
 * 调拨出库库存处理器
 * <p>
 * 调拨单确认出库时调用，从调出仓库扣减库存。
 * 调拨完整流程：确认出库(TransferOutHandler扣减) → 确认入库(TransferInHandler增加)。
 * 库存不足时抛出异常，事务回滚。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class TransferOutInventoryHandler implements InventoryChangeHandler {

    private final WmsInventoryMapper inventoryMapper;
    private final WmsInventoryLogMapper logMapper;

    @Override
    public ChangeType type() {
        return ChangeType.TRANSFER_OUT;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handle(Long billId, String billNo, LocalDateTime billTime, List<? extends ChangeItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (ChangeItem item : items) {
            Long locationId = item.getLocationId();
            // 批次空串/纯空格统一归一为 null，避免“无批次”在库里同时出现 '' 和 NULL 导致重复建库存行
            String batchNo = item.getBatchNo() == null || item.getBatchNo().trim().isEmpty()
                    ? null : item.getBatchNo().trim();
            // 先按 locationId/batchNo 精确匹配（null 值匹配 DB 的 NULL，而不是 0/空串）
            WmsInventory inv = inventoryMapper.selectOne(new LambdaQueryWrapper<WmsInventory>()
                    .eq(WmsInventory::getWarehouseId, item.getWarehouseId())
                    .eq(WmsInventory::getSkuId, item.getSkuId())
                    .eq(locationId != null, WmsInventory::getLocationId, locationId)
                    .isNull(locationId == null, WmsInventory::getLocationId)
                    .eq(batchNo != null && !batchNo.isEmpty(), WmsInventory::getBatchNo, batchNo)
                    .isNull(batchNo == null || batchNo.isEmpty(), WmsInventory::getBatchNo));
            // 调拨出库是扣减，找不到同批次就退化到 仓库+SKU 下可用数量最大的一条，只要总库存够就允许扣
            if (inv == null) {
                List<WmsInventory> cands = inventoryMapper.selectList(new LambdaQueryWrapper<WmsInventory>()
                        .eq(WmsInventory::getWarehouseId, item.getWarehouseId())
                        .eq(WmsInventory::getSkuId, item.getSkuId()));
                if (cands != null && !cands.isEmpty()) {
                    int needQty = item.getQty() != null ? item.getQty() : 0;
                    inv = cands.stream()
                            .filter(c -> c.getAvailableQty() != null && c.getAvailableQty() >= needQty)
                            .max((a, b) -> Integer.compare(a.getAvailableQty() == null ? 0 : a.getAvailableQty(),
                                    b.getAvailableQty() == null ? 0 : b.getAvailableQty()))
                            .orElse(cands.get(0));
                }
            }
            if (inv == null) {
                throw new BizException(ResultCode.STOCK_INSUFFICIENT);
            }

            int beforeQty = inv.getQuantity() != null ? inv.getQuantity() : 0;
            int qty = item.getQty() != null ? item.getQty() : 0;
            int afterQty = beforeQty - qty;
            if (afterQty < 0) {
                throw new BizException(ResultCode.STOCK_INSUFFICIENT);
            }

            BigDecimal costPrice = item.getCostPrice() != null ? item.getCostPrice() : BigDecimal.ZERO;
            BigDecimal subAmount = costPrice.multiply(BigDecimal.valueOf(qty));
            int oldVersion = inv.getVersion() != null ? inv.getVersion() : 0;
            int updated = inventoryMapper.subStock(inv.getInventoryId(), qty, subAmount, billTime, oldVersion);
            if (updated == 0) {
                throw new BizException(ResultCode.STOCK_INSUFFICIENT);
            }

            WmsInventoryLog log = new WmsInventoryLog();
            log.setLogId(SnowflakeId.getInstance().nextId());
            log.setBillId(billId);
            log.setBillNo(billNo);
            log.setBillTypeCode(type().getCode());
            log.setItemId(inv.getInventoryId());
            log.setWarehouseId(item.getWarehouseId());
            log.setSkuId(item.getSkuId());
            log.setLocationId(locationId);
            log.setBatchNo(batchNo);
            log.setDirection(-1);
            log.setQtyChange(qty);
            log.setUnitPrice(costPrice);
            log.setAmountChange(subAmount);
            log.setOperateBy(AuthContextHolder.getUserId());
            log.setOperateName(AuthContextHolder.getNickName());
            log.setOperateTime(billTime);
            log.setBeforeQty(beforeQty);
            log.setAfterQty(afterQty);
            log.setRemark(item.getRemark());
            log.setInnerCode(item.getInnerCode());
            logMapper.insert(log);
        }
    }
}
