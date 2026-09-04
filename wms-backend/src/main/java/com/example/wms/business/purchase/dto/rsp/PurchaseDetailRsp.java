package com.example.wms.business.purchase.dto.rsp;

import com.example.wms.business.purchase.entity.WmsPurchaseOrder;
import com.example.wms.business.purchase.entity.WmsPurchaseOrderItem;
import com.example.wms.business.purchase.entity.WmsPurchaseStatusLog;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class PurchaseDetailRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 采购单主表信息 */
    private WmsPurchaseOrder order;

    /** 采购单明细列表 */
    private List<WmsPurchaseOrderItem> items;

    /** 状态变更日志列表 */
    private List<WmsPurchaseStatusLog> statusLogs;
}
