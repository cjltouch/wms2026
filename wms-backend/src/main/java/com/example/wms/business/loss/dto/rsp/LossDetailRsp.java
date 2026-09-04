package com.example.wms.business.loss.dto.rsp;

import com.example.wms.business.loss.entity.WmsLossOrder;
import com.example.wms.business.loss.entity.WmsLossOrderItem;
import com.example.wms.business.loss.entity.WmsLossStatusLog;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class LossDetailRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 报损单主表信息 */
    private WmsLossOrder order;

    /** 报损单明细列表 */
    private List<WmsLossOrderItem> items;

    /** 状态变更日志列表 */
    private List<WmsLossStatusLog> statusLogs;
}
