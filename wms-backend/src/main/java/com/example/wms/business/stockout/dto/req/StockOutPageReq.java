package com.example.wms.business.stockout.dto.req;

import com.example.wms.common.PageReq;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
public class StockOutPageReq extends PageReq {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 出库单号 */
    private String stockOutNo;

    /** 出库类型 */
    private Integer type;

    /** 来源单号 */
    private String sourceBillNo;

    /** 仓库ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long warehouseId;

    /** 客户ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long customerId;

    /** 单据状态 */
    private Integer status;

    /** 出库操作人ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long outBy;
}
