package com.example.wms.business.transfer.dto.rsp;

import com.example.wms.business.transfer.entity.WmsTransferOrder;
import com.example.wms.business.transfer.entity.WmsTransferOrderItem;
import com.example.wms.business.transfer.entity.WmsTransferStatusLog;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class TransferDetailRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 调拨单主表信息 */
    private WmsTransferOrder order;

    /** 调拨单明细列表 */
    private List<WmsTransferOrderItem> items;

    /** 调拨单状态变更日志列表 */
    private List<WmsTransferStatusLog> statusLogs;
}
