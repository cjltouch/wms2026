package com.example.wms.business.purchase.dto.req;

import com.example.wms.common.PageReq;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
public class PurchasePageReq extends PageReq {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 采购单号 */
    private String purchaseNo;

    /** 供应商ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long supplierId;

    /** 仓库ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long warehouseId;

    /** 单据状态 */
    private Integer status;

    /** 采购员ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long purchaseBy;
}
