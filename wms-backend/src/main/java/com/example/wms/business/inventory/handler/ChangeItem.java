package com.example.wms.business.inventory.handler;

import java.math.BigDecimal;

public interface ChangeItem {

    Long getWarehouseId();

    Long getSkuId();

    Long getLocationId();

    String getBatchNo();

    Integer getQty();

    BigDecimal getCostPrice();

    String getRemark();

    String getInnerCode();
}
