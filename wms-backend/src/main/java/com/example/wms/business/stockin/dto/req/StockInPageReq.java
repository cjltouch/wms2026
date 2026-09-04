package com.example.wms.business.stockin.dto.req;

import com.example.wms.common.PageReq;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
public class StockInPageReq extends PageReq {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 入库单号 */
    private String stockInNo;

    /** 入库类型 */
    private Integer type;

    /** 来源单号 */
    private String sourceBillNo;

    /** 仓库ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long warehouseId;

    /** 供应商ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long supplierId;

    /** 单据状态 */
    private Integer status;

    /** 入库人ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long inBy;
}
