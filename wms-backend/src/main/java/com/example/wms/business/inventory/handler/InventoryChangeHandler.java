package com.example.wms.business.inventory.handler;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存变更处理器接口
 * <p>
 * 所有库存增减操作（出库扣减、入库增加、调拨双向、报损扣减等）
 * 统一通过此接口处理，由 InventoryChangeHandlerFactory 按 ChangeType 分发。
 */
public interface InventoryChangeHandler {

    /** 返回此处理器对应的库存变更类型 */
    ChangeType type();

    /**
     * 执行库存变更
     * @param billId  单据ID（出库单/调拨单/报损单等）
     * @param billNo  单据编号
     * @param billTime 单据业务时间
     * @param items   变更明细列表（含 SKU、数量、仓库、批次等）
     */
    void handle(Long billId, String billNo, LocalDateTime billTime, List<? extends ChangeItem> items);
}
