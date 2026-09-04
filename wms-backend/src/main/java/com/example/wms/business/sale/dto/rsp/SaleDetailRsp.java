package com.example.wms.business.sale.dto.rsp;

import com.example.wms.business.sale.entity.WmsSaleOrder;
import com.example.wms.business.sale.entity.WmsSaleOrderItem;
import com.example.wms.business.sale.entity.WmsSaleStatusLog;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class SaleDetailRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 销售单主表信息 */
    private WmsSaleOrder order;

    /** 销售单明细列表 */
    private List<WmsSaleOrderItem> items;

    /** 状态变更日志列表 */
    private List<WmsSaleStatusLog> statusLogs;
}
