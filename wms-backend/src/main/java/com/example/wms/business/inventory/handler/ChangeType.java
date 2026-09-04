package com.example.wms.business.inventory.handler;

/**
 * 库存变更类型枚举
 * <p>
 * 每种类型对应一个 InventoryChangeHandler 实现，由工厂统一分发。
 * 业务流程触发时机：
 * - STOCK_OUT_CONFIRM: 出库单审核通过时，扣减出库仓库库存
 * - TRANSFER_OUT: 调拨单确认出库时，扣减调出仓库库存
 * - TRANSFER_IN:  调拨单确认入库时，增加调入仓库库存
 * - LOSS:          报损单审核通过时，扣减报损仓库库存
 * - CHECK:         盘点单审核通过时，按盘点差异调整库存
 * - STOCK_IN:      入库单审核通过时，增加入库仓库库存
 * - PURCHASE_RETURN: 采购退货时，扣减库存
 */
public enum ChangeType {

    PURCHASE_RETURN(1, "采购退货"),
    STOCK_IN(2, "入库"),
    STOCK_OUT_LOCK(3, "出库锁定"),
    STOCK_OUT_CONFIRM(4, "出库确认"),
    TRANSFER_OUT(5, "调拨出库"),
    TRANSFER_IN(6, "调拨入库"),
    LOSS(7, "损耗"),
    CHECK(8, "盘点");

    private final Integer code;
    private final String desc;

    ChangeType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /** 根据 code 值匹配枚举实例，找不到返回 null */
    public static ChangeType ofCode(Integer code) {
        if (code == null) return null;
        for (ChangeType ct : values()) {
            if (ct.code.equals(code)) return ct;
        }
        return null;
    }
}
