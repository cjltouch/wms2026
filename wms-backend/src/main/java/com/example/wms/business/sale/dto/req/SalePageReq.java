package com.example.wms.business.sale.dto.req;

import com.example.wms.common.PageReq;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
public class SalePageReq extends PageReq {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 销售单号 */
    private String saleNo;

    /** 客户ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long customerId;

    /** 仓库ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long warehouseId;

    /** 单据状态 */
    private Integer status;
}
