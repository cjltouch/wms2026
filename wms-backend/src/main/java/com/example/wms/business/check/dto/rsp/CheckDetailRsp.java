package com.example.wms.business.check.dto.rsp;

import com.example.wms.business.check.entity.WmsCheckOrder;
import com.example.wms.business.check.entity.WmsCheckOrderItem;
import com.example.wms.business.check.entity.WmsCheckStatusLog;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class CheckDetailRsp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 盘点单主表信息 */
    private WmsCheckOrder order;

    /** 盘点单明细列表 */
    private List<WmsCheckOrderItem> items;

    /** 状态变更日志列表 */
    private List<WmsCheckStatusLog> statusLogs;
}
