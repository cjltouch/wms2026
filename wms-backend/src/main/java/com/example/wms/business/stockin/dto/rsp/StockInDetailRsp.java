package com.example.wms.business.stockin.dto.rsp;

import com.example.wms.business.stockin.entity.WmsStockIn;
import com.example.wms.business.stockin.entity.WmsStockInItem;
import com.example.wms.business.stockin.entity.WmsStockInStatusLog;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class StockInDetailRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 入库单主表信息 */
    private WmsStockIn order;

    /** 入库单明细列表 */
    private List<WmsStockInItem> items;

    /** 状态变更日志列表 */
    private List<WmsStockInStatusLog> statusLogs;
}
