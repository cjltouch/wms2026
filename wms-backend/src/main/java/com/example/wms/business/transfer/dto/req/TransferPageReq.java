package com.example.wms.business.transfer.dto.req;

import com.example.wms.common.PageReq;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
public class TransferPageReq extends PageReq {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 调拨单号 */
    private String transferNo;

    /** 调出仓库ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long outWarehouseId;

    /** 调入仓库ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long inWarehouseId;

    /** 单据状态 */
    private Integer status;
}
