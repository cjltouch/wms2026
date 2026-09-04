package com.example.wms.business.stockout.dto.rsp;

import com.example.wms.business.stockout.entity.WmsStockOut;
import com.example.wms.business.stockout.entity.WmsStockOutItem;
import com.example.wms.business.stockout.entity.WmsStockOutStatusLog;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class StockOutDetailRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 出库单主表信息 */
    private WmsStockOut order;

    /** 出库单明细列表 */
    private List<WmsStockOutItem> items;

    /** 出库单状态变更日志列表 */
    private List<WmsStockOutStatusLog> statusLogs;
}
